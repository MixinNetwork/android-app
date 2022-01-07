/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package androidx.room;
/**
 * RPC Callbacks for {@link IMultiInstanceInvalidationService}.
 *
 * @hide
 */
public interface IMultiInstanceInvalidationCallback extends android.os.IInterface
{
  /** Default implementation for IMultiInstanceInvalidationCallback. */
  public static class Default implements androidx.room.IMultiInstanceInvalidationCallback
  {
    /**
         * Called when invalidation is detected in another instance of the same database.
         *
         * @param tables List of invalidated table names
         */
    @Override public void onInvalidation(java.lang.String[] tables) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements androidx.room.IMultiInstanceInvalidationCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an androidx.room.IMultiInstanceInvalidationCallback interface,
     * generating a proxy if needed.
     */
    public static androidx.room.IMultiInstanceInvalidationCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof androidx.room.IMultiInstanceInvalidationCallback))) {
        return ((androidx.room.IMultiInstanceInvalidationCallback)iin);
      }
      return new androidx.room.IMultiInstanceInvalidationCallback.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_onInvalidation:
        {
          data.enforceInterface(descriptor);
          java.lang.String[] _arg0;
          _arg0 = data.createStringArray();
          this.onInvalidation(_arg0);
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements androidx.room.IMultiInstanceInvalidationCallback
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      /**
           * Called when invalidation is detected in another instance of the same database.
           *
           * @param tables List of invalidated table names
           */
      @Override public void onInvalidation(java.lang.String[] tables) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStringArray(tables);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onInvalidation, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            if (getDefaultImpl() != null) {
              getDefaultImpl().onInvalidation(tables);
              return;
            }
          }
        }
        finally {
          _data.recycle();
        }
      }
      public static androidx.room.IMultiInstanceInvalidationCallback sDefaultImpl;
    }
    static final int TRANSACTION_onInvalidation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    public static boolean setDefaultImpl(androidx.room.IMultiInstanceInvalidationCallback impl) {
      // Only one user of this interface can use this function
      // at a time. This is a heuristic to detect if two different
      // users in the same process use this function.
      if (Stub.Proxy.sDefaultImpl != null) {
        throw new IllegalStateException("setDefaultImpl() called twice");
      }
      if (impl != null) {
        Stub.Proxy.sDefaultImpl = impl;
        return true;
      }
      return false;
    }
    public static androidx.room.IMultiInstanceInvalidationCallback getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  public static final java.lang.String DESCRIPTOR = "androidx.room.IMultiInstanceInvalidationCallback";
  /**
       * Called when invalidation is detected in another instance of the same database.
       *
       * @param tables List of invalidated table names
       */
  public void onInvalidation(java.lang.String[] tables) throws android.os.RemoteException;
}
