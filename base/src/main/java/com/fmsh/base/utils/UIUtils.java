package com.fmsh.base.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author wuyajiang
 * @date 2020/3/10
 */
public class UIUtils {



    /**
     * 获取资源文件
     *
     * @return
     */
    public static Resources getResources(Context context) {
        return context.getResources();
    }

    /**
     * 获取资源文件字符串
     * @param id
     * @return
     */
    public static String getString(Context context,int id) {
        return getResources(context).getString(id);
    }
    public static String getString(int id) {
        return getResources(ActivityUtils.instance.getCurrentActivity()).getString(id);
    }
    public static int getColor(Context context,int id) {
        return getResources(context).getColor(id);
    }
    public static int getColor(int id) {
        return getResources(ActivityUtils.instance.getCurrentActivity()).getColor(id);
    }

    public static Drawable getDrawable(Context context,int id) {
        return getResources(context).getDrawable(id);
    }
    public static Drawable getDrawable(int id) {
        return getResources(ActivityUtils.instance.getCurrentActivity()).getDrawable(id);
    }

    public static String[] getStringArray(Context context,int id) {
        return getResources(context).getStringArray(id);
    }public static String[] getStringArray(int id) {
        return getResources(ActivityUtils.instance.getCurrentActivity()).getStringArray(id);
    }
    public static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }

    /**
     * 将px转换成与之对应的dp
     *
     * @param px
     * @return
     */
    public static int getPx2Dp(Context context,int px) {
        float scale = getResources(context).getDisplayMetrics().density;
        return (int) (px / scale + 0.5f);
    }
    public static int getPx2Dp(int px) {
        float scale = getResources(ActivityUtils.instance.getCurrentActivity()).getDisplayMetrics().density;
        return (int) (px / scale + 0.5f);
    }

    /**
     * 将dp转换成与之对应的px
     *
     * @param dp
     * @return
     */
    public static int getDp2Px(Context context,int dp) {
        float density = getResources(context).getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
    public static int getDp2Px(int dp) {
        float density = getResources(ActivityUtils.instance.getCurrentActivity()).getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * 将px转换成sp
     *
     * @param px
     * @return
     */
    public static int getPx2Sp(Context context,int px) {
        float scaledDensity = getResources(context).getDisplayMetrics().scaledDensity;
        return (int) (px / scaledDensity + 0.5f);
    }
    public static int getPx2Sp(int px) {
        float scaledDensity = getResources(ActivityUtils.instance.getCurrentActivity()).getDisplayMetrics().scaledDensity;
        return (int) (px / scaledDensity + 0.5f);
    }
    /**
     * 将sp转换成px
     *
     * @param dp
     * @return
     */
    public static int getSp2Px(Context context,int dp) {
        float scaledDensity = getResources(context).getDisplayMetrics().scaledDensity;
        return (int) (dp * scaledDensity + 0.5f);
    }
    public static int getSp2Px(int dp) {
        float scaledDensity = getResources(ActivityUtils.instance.getCurrentActivity()).getDisplayMetrics().scaledDensity;
        return (int) (dp * scaledDensity + 0.5f);
    }
    /**
     * 获取dimens下的尺寸大小
     *
     * @param id
     * @return
     */
    public static int getResDimens(Context context,int id) {
        return getResources(context).getDimensionPixelSize(id);
    }


    /**
     * 加载布局生成View
     *
     * @param context
     * @param LayoutId
     * @param viewGroup
     * @return
     */
    public static View loadView(Context context, int LayoutId, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(LayoutId, viewGroup, false);
    }


    /**
     * handler消息发送
     *
     * @param object
     * @param what
     * @param handler
     */
    public static void sendMessage(Object object, int what, Handler handler) {
        if(handler != null){
            Message message = new Message();
            message.obj = object;
            message.what = what;
            handler.sendMessage(message);
        }

    }





}
