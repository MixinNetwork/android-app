package one.mixin.android.ui.search;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.paging.PositionalDataSource;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import one.mixin.android.util.CrashExceptionReportKt;
import timber.log.Timber;

@SuppressLint("RestrictedApi")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class CancellationLimitOffsetDataSource<T> extends PositionalDataSource<T> {
    private final RoomSQLiteQuery mSourceQuery;
    private final RoomSQLiteQuery mCountQuery;
    private final String mLimitOffsetQuery;
    private final RoomDatabase mDb;
    @SuppressWarnings("FieldCanBeLocal")
    private final InvalidationTracker.Observer mObserver;
    private final boolean mInTransaction;
    private final CancellationSignal mCancellationSignal;

    protected CancellationLimitOffsetDataSource(RoomDatabase db, RoomSQLiteQuery query,
                                         RoomSQLiteQuery countQuery,
                                         CancellationSignal cancellationSignal,
                                         boolean inTransaction, String... tables) {
        mDb = db;
        mSourceQuery = query;
        mInTransaction = inTransaction;
        mCountQuery = countQuery;
        mCancellationSignal = cancellationSignal;
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
        try (Cursor cursor = mDb.query(mCountQuery, mCancellationSignal)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } catch (OperationCanceledException e) {
            return 0;
        }  finally {
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
        if (list != null) {
            try {
                callback.onResult(list, firstLoadPosition, totalCount);
            } catch (IllegalArgumentException e) {
                // workaround with paging initial load size NOT to be a multiple of page size
                Timber.w(e);
                try {
                    callback.onResult(list, firstLoadPosition, firstLoadPosition + list.size());
                } catch (IllegalArgumentException iae) {
                    // workaround with paging incorrect tiling
                    String message = "CancellationLimitOffsetDataSource "
                            + "firstLoadPosition: " + firstLoadPosition
                            + ", list size: " + list.size()
                            + ", count: " + totalCount;
                    CrashExceptionReportKt.reportException(message, iae);
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
                cursor = mDb.query(sqLiteQuery, mCancellationSignal);
                List<T> rows = convertRows(cursor);
                mDb.setTransactionSuccessful();
                return rows;
            } catch (OperationCanceledException e) {
                return new ArrayList<T>();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                mDb.endTransaction();
                sqLiteQuery.release();
            }
        } else {
            Cursor cursor = mDb.query(sqLiteQuery, mCancellationSignal);
            //noinspection TryFinallyCanBeTryWithResources
            try {
                return convertRows(cursor);
            } catch (OperationCanceledException e) {
                return new ArrayList<T>();
            }  finally {
                cursor.close();
                sqLiteQuery.release();
            }
        }
    }
}