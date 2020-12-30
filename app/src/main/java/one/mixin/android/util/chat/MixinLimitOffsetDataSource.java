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
        mLimitOffsetQuery = mSourceQuery.getSql() + "LIMIT ? OFFSET ?";
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
        int totalCount = countItems();
        if (totalCount == 0) {
            callback.onResult(Collections.emptyList(), 0, 0);
            return;
        }

        // bound the size requested, based on known count
        final int firstLoadPosition = computeInitialLoadPosition(params, totalCount);
        final int firstLoadSize = computeInitialLoadSize(params, firstLoadPosition, totalCount);

        List<T> list = loadRange(firstLoadPosition, firstLoadSize);
        if (list != null && list.size() == firstLoadSize) {
            callback.onResult(list, firstLoadPosition, totalCount);
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
}
