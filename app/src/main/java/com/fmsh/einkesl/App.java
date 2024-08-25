package com.fmsh.einkesl;

import android.app.Application;
import android.os.Handler;

import com.fmsh.base.utils.Density;
import com.fmsh.einkesl.bean.DeviceInfo;

/**
 * @author wuyajiang
 * @date 2021/4/14
 */
public class App extends Application {
    private static DeviceInfo deviceInfo;
    private static Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        Density.setDensity(this,360);
    }

    public static void setDeviceInfo(DeviceInfo deviceInfo) {
        App.deviceInfo = deviceInfo;
    }

    public static DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public static void setHandler(Handler mHandler) {
        App.mHandler = mHandler;
    }

    public static Handler getHandler() {
        return mHandler;
    }
}
