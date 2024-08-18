package com.fmsh.einkesl.tools;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;


import com.fmsh.base.utils.Constant;
import com.fmsh.base.utils.UIUtils;

import java.util.List;

/**
 * @author wuyajiang
 * @date 2020/3/19
 */
public class MyThread extends Thread {
    private static MyThread myThread = new MyThread();
    private Handler mHandler;
    private MyThread(){}
    public static MyThread getInstance(){
        return myThread;
    }


    private MyHandler mMyHandler;

    public MyHandler getMyHandler() {
        return mMyHandler;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }



    @Override
    public void run() {
        super.run();
        Looper.prepare();
        mMyHandler = new MyHandler();
        Looper.loop();
    }

    public class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    IsoDepUtils.startIsoDep((Bundle) msg.obj);
                    break;
                default:
                    break;
            }

            }


    }

}
