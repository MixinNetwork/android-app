/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package androidx.room;
/**
 * RPC Service that controls interaction about multi-instance invalidation.
 *
 * @hide
 */
public interface IMultiInstanceInvalidationService extends android.os.IInterface
{
  /** Default implementation for IMultiInstanceInvalidationService. */
  public static class Default implements androidx.room.IMultiInstanceInvalidationService
  {
    /**
         * Registers a new {@link IMultiInstanceInvalidationCallback} as a client of this service.
         *
         * @param callback The RPC callback.
         * @param name The name of the database file as it is passed to {@link RoomDatabase.Builder}.
         * @return A new client ID. The client needs to hold on to this ID and pass it to the service
         *         for subsequent calls.
         */
    @Override public int registerCallback(androidx.room.IMultiInstanceInvalidationCallback callback, java.lang.String name) throws android.os.RemoteException
    {
      return 0;
    }
    /**
         * Unregisters the specified {@link IMultiInstanceInvalidationCallback} from this service.
         * <p>
         * Clients might die without explicitly calling this method. In that case, the service should
         * handle the clean up.
         *
         * @param callback The RPC callback.
         * @param clientId The client ID returned from {@link #registerCallback}.
         */
    @Override public void unregisterCallback(androidx.room.IMultiInstanceInvalidationCallback callback, int clientId) throws android.os.RemoteException
    {
    }
    /**
         * Broadcasts invalidation of database tables to other clients registered to this service.
         * <p>
         * The broadcast is delivered to {@link IMultiInstanceInvalidationCallback#onInvalidation} of
         * the registered clients. The client calling this method will not receive its own broadcast.
         * Clients that are associated with a different database file will not be notified.
         *
         * @param clientId The client ID returned from {@link #registerCallback}.
         * @param tables The names of invalidated tables.
         */
    @Override public void broadcastInvalidation(int clientId, java.lang.String[] tables) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements androidx.room.IMultiInstanceInvalidationService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an androidx.room.IMultiInstanceInvalidationService interface,
     * generating a proxy if needed.
     */
    public static androidx.room.IMultiInstanceInvalidationService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof androidx.room.IMultiInstanceInvalidationService))) {
        return ((androidx.room.IMultiInstanceInvalidationService)iin);
      }
      return new androidx.room.IMultiInstanceInvalidationService.Stub.Proxy(obj);
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
        case TRANSACTION_registerCallback:
        {
          data.enforceInterface(descriptor);
          androidx.room.IMultiInstanceInvalidationCallback _arg0;
          _arg0 = androidx.room.IMultiInstanceInvalidationCallback.Stub.asInterface(data.readStrongBinder());
          java.lang.String _arg1;
          _arg1 = data.readString();
          int _result = this.registerCallback(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          return true;
        }
        case TRANSACTION_unregisterCallback:
        {
          data.enforceInterface(descriptor);
          androidx.room.IMultiInstanceInvalidationCallback _arg0;
          _arg0 = androidx.room.IMultiInstanceInvalidationCallback.Stub.asInterface(data.readStrongBinder());
          int _arg1;
          _arg1 = data.readInt();
          this.unregisterCallback(_arg0, _arg1);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_broadcastInvalidation:
        {
          data.enforceInterface(descriptor);
          int _arg0;
          _arg0 = data.readInt();
          java.lang.String[] _arg1;
          _arg1 = data.createStringArray();
          this.broadcastInvalidation(_arg0, _arg1);
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements androidx.room.IMultiInstanceInvalidationService
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
           * Registers a new {@link IMultiInstanceInvalidationCallback} as a client of this service.
           *
           * @param callback The RPC callback.
           * @param name The name of the database file as it is passed to {@link RoomDatabase.Builder}.
           * @return A new client ID. The client needs to hold on to this ID and pass it to the service
           *         for subsequent calls.
           */
      @Override public int registerCallback(androidx.room.IMultiInstanceInvalidationCallback callback, java.lang.String name) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
          _data.writeString(name);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerCallback, _data, _reply, 0);
          if (!_status) {
            if (getDefaultImpl() != null) {
              return getDefaultImpl().registerCallback(callback, name);
            }
          }
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /**
           * Unregisters the specified {@link IMultiInstanceInvalidationCallback} from this service.
           * <p>
           * Clients might die without explicitly calling this method. In that case, the service should
           * handle the clean up.
           *
           * @param callback The RPC callback.
           * @param clientId The client ID returned from {@link #registerCallback}.
           */
      @Override public void unregisterCallback(androidx.room.IMultiInstanceInvalidationCallback callback, int clientId) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
          _data.writeInt(clientId);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterCallback, _data, _reply, 0);
          if (!_status) {
            if (getDefaultImpl() != null) {
              getDefaultImpl().unregisterCallback(callback, clientId);
              return;
            }
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
           * Broadcasts invalidation of database tables to other clients registered to this service.
           * <p>
           * The broadcast is delivered to {@link IMultiInstanceInvalidationCallback#onInvalidation} of
           * the registered clients. The client calling this method will not receive its own broadcast.
           * Clients that are associated with a different database file will not be notified.
           *
           * @param clientId The client ID returned from {@link #registerCallback}.
           * @param tables The names of invalidated tables.
           */
      @Override public void broadcastInvalidation(int clientId, java.lang.String[] tables) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(clientId);
          _data.writeStringArray(tables);
          boolean _status = mRemote.transact(Stub.TRANSACTION_broadcastInvalidation, _data, null, android.os.IBinder.FLAG_ONEWAY);
          if (!_status) {
            if (getDefaultImpl() != null) {
              getDefaultImpl().broadcastInvalidation(clientId, tables);
              return;
            }
          }
        }
        finally {
          _data.recycle();
        }
      }
      public static androidx.room.IMultiInstanceInvalidationService sDefaultImpl;
    }
    static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_unregisterCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_broadcastInvalidation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    public static boolean setDefaultImpl(androidx.room.IMultiInstanceInvalidationService impl) {
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
    public static androidx.room.IMultiInstanceInvalidationService getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  public static final java.lang.String DESCRIPTOR = "androidx.room.IMultiInstanceInvalidationService";
  /**
       * Registers a new {@link IMultiInstanceInvalidationCallback} as a client of this service.
       *
       * @param callback The RPC callback.
       * @param name The name of the database file as it is passed to {@link RoomDatabase.Builder}.
       * @return A new client ID. The client needs to hold on to this ID and pass it to the service
       *         for subsequent calls.
       */
  public int registerCallback(androidx.room.IMultiInstanceInvalidationCallback callback, java.lang.String name) throws android.os.RemoteException;
  /**
       * Unregisters the specified {@link IMultiInstanceInvalidationCallback} from this service.
       * <p>
       * Clients might die without explicitly calling this method. In that case, the service should
       * handle the clean up.
       *
       * @param callback The RPC callback.
       * @param clientId The client ID returned from {@link #registerCallback}.
       */
  public void unregisterCallback(androidx.room.IMultiInstanceInvalidationCallback callback, int clientId) throws android.os.RemoteException;
  /**
       * Broadcasts invalidation of database tables to other clients registered to this service.
       * <p>
       * The broadcast is delivered to {@link IMultiInstanceInvalidationCallback#onInvalidation} of
       * the registered clients. The client calling this method will not receive its own broadcast.
       * Clients that are associated with a different database file will not be notified.
       *
       * @param clientId The client ID returned from {@link #registerCallback}.
       * @param tables The names of invalidated tables.
       */
  public void broadcastInvalidation(int clientId, java.lang.String[] tables) throws android.os.RemoteException;
}
