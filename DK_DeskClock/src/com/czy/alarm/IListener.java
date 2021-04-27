/**
 * bv name zhangjiachu Alarm数据同步：aidl,夸进程通讯工具类
 */

/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.czy.alarm;
//parcelable AlarmInfoTemp;
// Declare any non-default types here with import statements

public interface IListener extends android.os.IInterface
{
  /** Default implementation for IListener. */
  public static class Default implements com.czy.alarm.IListener
  {
    @Override public void sendProccessResult(java.lang.String mAlarmInfoTemp) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.czy.alarm.IListener
  {
    private static final java.lang.String DESCRIPTOR = "com.czy.alarm.IListener";
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.czy.alarm.IListener interface,
     * generating a proxy if needed.
     */
    public static com.czy.alarm.IListener asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.czy.alarm.IListener))) {
        return ((com.czy.alarm.IListener)iin);
      }
      return new com.czy.alarm.IListener.Stub.Proxy(obj);
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
        case TRANSACTION_sendProccessResult:
        {
          data.enforceInterface(descriptor);
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.sendProccessResult(_arg0);
          reply.writeNoException();
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements com.czy.alarm.IListener
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
      @Override public void sendProccessResult(java.lang.String mAlarmInfoTemp) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(mAlarmInfoTemp);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendProccessResult, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().sendProccessResult(mAlarmInfoTemp);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      public static com.czy.alarm.IListener sDefaultImpl;
    }
    static final int TRANSACTION_sendProccessResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    public static boolean setDefaultImpl(com.czy.alarm.IListener impl) {
      if (Stub.Proxy.sDefaultImpl == null && impl != null) {
        Stub.Proxy.sDefaultImpl = impl;
        return true;
      }
      return false;
    }
    public static com.czy.alarm.IListener getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  public void sendProccessResult(java.lang.String mAlarmInfoTemp) throws android.os.RemoteException;
}
