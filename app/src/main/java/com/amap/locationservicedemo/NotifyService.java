package com.amap.locationservicedemo;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

/**
 * 利用双service进行notification绑定，进而将Service的OOM_ADJ提高到1
 * 同时利用LocationHelperService充当守护进程，在NotiService被关闭后，重启他。（如果LocationHelperService被停止，NotiService不负责唤醒)
 */


public class NotifyService extends Service {

    /**
     * i
     * startForeground的 noti_id
     */
    private static final int NOTI_ID = 123321;
    public Binder mBinder;
    private Utils.CloseServiceReceiver mCloseReceiver;
    private ServiceConnection connection;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mCloseReceiver = new Utils.CloseServiceReceiver(this);
        registerReceiver(mCloseReceiver, Utils.getCloseServiceFilter());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mCloseReceiver != null) {
            unregisterReceiver(mCloseReceiver);
            mCloseReceiver = null;
        }

        super.onDestroy();
    }

    /**
     * 触发利用notification增加进程优先级
     */
    protected void applyNotiKeepMech() {
        startForeground(NOTI_ID, Utils.buildNotification(getBaseContext()));
        startBindHelperService();
    }

    public void unApplyNotiKeepMech() {
        stopForeground(true);
    }

    private void startBindHelperService() {

        final String mHelperServiceName = "com.amap.locationservicedemo.LocationHelperService";
        connection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                //doing nothing
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ILocationHelperServiceAIDL l = ILocationHelperServiceAIDL.Stub.asInterface(service);
                try {
                    l.onFinishBind(NOTI_ID);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };
        Intent intent = new Intent();
        intent.setAction(mHelperServiceName);
        bindService(Utils.getExplicitIntent(getApplicationContext(), intent), connection, Service.BIND_AUTO_CREATE);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = new LocationServiceBinder();
        }
        return mBinder;
    }

    public class LocationServiceBinder extends ILocationServiceAIDL.Stub {
        public void onFinishBind() {
        }
    }

}
