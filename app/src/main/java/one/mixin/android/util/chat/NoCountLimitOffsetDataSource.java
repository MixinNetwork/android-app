package one.mixin.android.util.chat;

import android.annotation.SuppressLint;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.paging.PositionalDataSource;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;

import java.util.List;
import java.util.Set;

import timber.log.Timber;

@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class NoCountLimitOffsetDataSource<T> extends PositionalDataSource<T> {
    private final RoomSQLiteQuery mSourceQuery;
    private final String mLimitOffsetQuery;
    private final int mTotalCount;
    private final RoomDatabase mDb;
    @SuppressWarnings("FieldCanBeLocal")
    private final InvalidationTracker.Observer mObserver;

    protected NoCountLimitOffsetDataSource(RoomDatabase db, RoomSQLiteQuery query, int count, String... tables) {
        mDb = db;
        mTotalCount = count;
        mSourceQuery = query;
        mLimitOffsetQuery = mSourceQuery.getSql() + " LIMIT ? OFFSET ?";
        mObserver = new InvalidationTracker.Observer(tables) {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                invalidate();
            }
        };
        db.getInvalidationTracker().addWeakObserver(mObserver);
    }

    @Override
    public boolean isInvalid() {
        mDb.getInvalidationTracker().refreshVersionsSync();
        return super.isInvalid();
    }

    @SuppressWarnings("WeakerAccess")
    protected abstract List<T> convertRows(Cursor cursor);

    @Override
    public void loadInitial(@NonNull LoadInitialParams params,
                            @NonNull LoadInitialCallback<T> callback) {
        final int firstLoadPosition = computeInitialLoadPosition(params, mTotalCount);
        final int firstLoadSize = computeInitialLoadSize(params, firstLoadPosition, mTotalCount);
        List<T> initialList = loadRange(firstLoadPosition, firstLoadSize);
        if (initialList != null) {
            try {
                callback.onResult(initialList, firstLoadPosition, mTotalCount);
            } catch (IllegalArgumentException e) {
                // workaround with paging initial load size NOT to be a multiple of page size
                Timber.w(e);
                try {
                    callback.onResult(initialList, firstLoadPosition, firstLoadPosition + initialList.size());
                } catch (IllegalArgumentException iae) {
                    // workaround with paging incorrect tiling
                    Timber.w(iae);
                }
            }
        } else {
            // null list, or size doesn't match request - DB modified between count and load
            invalidate();
        }
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params,
                          @NonNull LoadRangeCallback<T> callback) {
        callback.onResult(loadRange(params.startPosition, params.loadSize));
    }

    @Nullable
    public List<T> loadRange(int startPosition, int loadCount) {
        final RoomSQLiteQuery sqLiteQuery = RoomSQLiteQuery.acquire(mLimitOffsetQuery,
                mSourceQuery.getArgCount() + 2);
        sqLiteQuery.copyArgumentsFrom(mSourceQuery);
        sqLiteQuery.bindLong(sqLiteQuery.getArgCount() - 1, loadCount);
        sqLiteQuery.bindLong(sqLiteQuery.getArgCount(), startPosition);
        Cursor cursor = mDb.query(sqLiteQuery);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            return convertRows(cursor);
        } finally {
            cursor.close();
            sqLiteQuery.release();
        }
    }
}
