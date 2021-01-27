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
import androidx.sqlite.db.SupportSQLiteQuery;
import timber.log.Timber;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class MixinLimitOffsetDataSource<T> extends PositionalDataSource<T> {
    private final RoomSQLiteQuery mSourceQuery;
    private final RoomSQLiteQuery mCountQuery;
    private final String mLimitOffsetQuery;
    private final RoomDatabase mDb;
    @SuppressWarnings("FieldCanBeLocal")
    private final InvalidationTracker.Observer mObserver;
    private final boolean mInTransaction;

    protected MixinLimitOffsetDataSource(RoomDatabase db, SupportSQLiteQuery query,
                                         RoomSQLiteQuery countQuery,
                                         boolean inTransaction, String... tables) {
        this(db, RoomSQLiteQuery.copyFrom(query), RoomSQLiteQuery.copyFrom(countQuery), inTransaction, tables);
    }

    protected MixinLimitOffsetDataSource(RoomDatabase db, RoomSQLiteQuery query,
                                         RoomSQLiteQuery countQuery,
                                         boolean inTransaction, String... tables) {
        mDb = db;
        mSourceQuery = query;
        mInTransaction = inTransaction;
        mCountQuery = countQuery;
        mLimitOffsetQuery = "SELECT * FROM ( " + mSourceQuery.getSql() + " ) LIMIT ? OFFSET ?";
        mObserver = new InvalidationTracker.Observer(tables) {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                invalidate();
            }
        };
        db.getInvalidationTracker().addWeakObserver(mObserver);
    }

    /**
     * Count number of rows query can return
     */
    @SuppressWarnings("WeakerAccess")
    public int countItems() {
        Cursor cursor = mDb.query(mCountQuery);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            cursor.close();
            mCountQuery.release();
        }
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
        long start = System.currentTimeMillis();
        int totalCount = countItems();
        if (totalCount == 0) {
            callback.onResult(Collections.emptyList(), 0, 0);
            return;
        }

        // bound the size requested, based on known count
        final int firstLoadPosition = computeInitialLoadPosition(params, totalCount);
        final int firstLoadSize = computeInitialLoadSize(params, firstLoadPosition, totalCount);

        List<T> list = loadRange(firstLoadPosition, firstLoadSize);
        Timber.d("@@@ loadInitial cost: " + (System.currentTimeMillis() - start));
        if (list != null && list.size() == firstLoadSize) {
            try {
                callback.onResult(list, firstLoadPosition, totalCount);
            } catch (IllegalArgumentException e) {
                // workaround with paging incorrect tiling
                Timber.w(e);
            }
        } else {
            // null list, or size doesn't match request - DB modified between count and load
            invalidate();
        }
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params,
                          @NonNull LoadRangeCallback<T> callback) {
        List<T> list = loadRange(params.startPosition, params.loadSize);
        if (list != null) {
            callback.onResult(list);
        } else {
            invalidate();
        }
    }

    /**
     * Return the rows from startPos to startPos + loadCount
     */
    @Nullable
    public List<T> loadRange(int startPosition, int loadCount) {
        final RoomSQLiteQuery sqLiteQuery = RoomSQLiteQuery.acquire(mLimitOffsetQuery,
                mSourceQuery.getArgCount() + 2);
        sqLiteQuery.copyArgumentsFrom(mSourceQuery);
        sqLiteQuery.bindLong(sqLiteQuery.getArgCount() - 1, loadCount);
        sqLiteQuery.bindLong(sqLiteQuery.getArgCount(), startPosition);
        if (mInTransaction) {
            mDb.beginTransaction();
            Cursor cursor = null;
            try {
                cursor = mDb.query(sqLiteQuery);
                List<T> rows = convertRows(cursor);
                mDb.setTransactionSuccessful();
                return rows;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                mDb.endTransaction();
                sqLiteQuery.release();
            }
        } else {
            long start = System.currentTimeMillis();
            Cursor cursor = mDb.query(sqLiteQuery);
            //noinspection TryFinallyCanBeTryWithResources
            try {
                List<T> list = convertRows(cursor);
                Timber.d("@@@ loadRange cost: " + (System.currentTimeMillis() - start) + ", startPosition: " + startPosition + ", loadCount: " + loadCount);
                return list;
            } finally {
                cursor.close();
                sqLiteQuery.release();
            }
        }
    }
}