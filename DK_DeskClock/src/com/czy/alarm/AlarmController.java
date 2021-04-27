/**
 * bv name zhangjiachu Alarm数据同步：aidl,夸进程通讯工具类
 */
/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.czy.alarm;
//parcelable AlarmInfoTemp;
// Declare any non-default types here with import statements

public interface AlarmController extends android.os.IInterface
{
  /** Default implementation for AlarmController. */
  public static class Default implements com.czy.alarm.AlarmController
  {
    //client send msg to server
    /**
         * Demonstrates some basic types that you can use as parameters
         * and return values in AIDL.
         */
    @Override public void sendAlarmInfoTempInOut(java.lang.String alarmInfoTemp) throws android.os.RemoteException
    {
    }
    //void sendProcessResult(int _id, int flag);
    //void getAlarmTemp();

    @Override public void setListener(com.czy.alarm.IListener lst) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.czy.alarm.AlarmController
  {
    private static final java.lang.String DESCRIPTOR = "com.czy.alarm.AlarmController";
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.czy.alarm.AlarmController interface,
     * generating a proxy if needed.
     */
    public static com.czy.alarm.AlarmController asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.czy.alarm.AlarmController))) {
        return ((com.czy.alarm.AlarmController)iin);
      }
      return new com.czy.alarm.AlarmController.Stub.Proxy(obj);
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
        case TRANSACTION_sendAlarmInfoTempInOut:
        {
          data.enforceInterface(descriptor);
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.sendAlarmInfoTempInOut(_arg0);
          reply.writeNoException();
          return true;
        }
        case TRANSACTION_setListener:
        {
          data.enforceInterface(descriptor);
          com.czy.alarm.IListener _arg0;
          _arg0 = com.czy.alarm.IListener.Stub.asInterface(data.readStrongBinder());
          this.setListener(_arg0);
          reply.writeNoException();
          return true;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
    }
    private static class Proxy implements com.czy.alarm.AlarmController
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
      //client send msg to server
      /**
           * Demonstrates some basic types that you can use as parameters
           * and return values in AIDL.
           */
      @Override public void sendAlarmInfoTempInOut(java.lang.String alarmInfoTemp) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(alarmInfoTemp);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendAlarmInfoTempInOut, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().sendAlarmInfoTempInOut(alarmInfoTemp);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      //void sendProcessResult(int _id, int flag);
      //void getAlarmTemp();

      @Override public void setListener(com.czy.alarm.IListener lst) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongBinder((((lst!=null))?(lst.asBinder()):(null)));
          boolean _status = mRemote.transact(Stub.TRANSACTION_setListener, _data, _reply, 0);
          if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().setListener(lst);
            return;
          }
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      public static com.czy.alarm.AlarmController sDefaultImpl;
    }
    static final int TRANSACTION_sendAlarmInfoTempInOut = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_setListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    public static boolean setDefaultImpl(com.czy.alarm.AlarmController impl) {
      if (Stub.Proxy.sDefaultImpl == null && impl != null) {
        Stub.Proxy.sDefaultImpl = impl;
        return true;
      }
      return false;
    }
    public static com.czy.alarm.AlarmController getDefaultImpl() {
      return Stub.Proxy.sDefaultImpl;
    }
  }
  //client send msg to server
  /**
       * Demonstrates some basic types that you can use as parameters
       * and return values in AIDL.
       */
  public void sendAlarmInfoTempInOut(java.lang.String alarmInfoTemp) throws android.os.RemoteException;
  //void sendProcessResult(int _id, int flag);
  //void getAlarmTemp();

  public void setListener(com.czy.alarm.IListener lst) throws android.os.RemoteException;
}
