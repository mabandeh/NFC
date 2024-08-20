package com.fmsh.base.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;


import com.fmsh.base.R;
import com.qmuiteam.qmui.QMUIConfig;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.util.QMUIStatusBarHelper;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;


/**
 * @author wuyajiang
 * @date 2019/9/12
 */
public class HintDialog {
    /**
     * 显示加载框
     *
     */
    public static QMUITipDialog loadingDialog(Context context) {
        QMUITipDialog tipDialog = new QMUITipDialog.Builder(context)
                .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                .setTipWord(context.getString(R.string.text_loading))
                .create();
        return  tipDialog;
    }

    /**
     * 加载框
     * @param context
     * @param msg
     * @return
     */
    public static QMUITipDialog loadingDialog(Context context, String msg){
        QMUITipDialog tipDialog = new QMUITipDialog.Builder(context)
                .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                .setTipWord(msg)
                .create();
        return  tipDialog;
    }


    /**
     * 消息提示
     * @param msg
     */
    public static void messageDialog(String msg) {
        QMUITipDialog tipDialog = new QMUITipDialog.Builder(ActivityUtils.instance.getCurrentActivity())
                .setIconType(QMUITipDialog.Builder.ICON_TYPE_INFO)
                .setTipWord(msg)
                .create();
        tipDialog.show();
        dialogDismiss(tipDialog);
    }


    /**
     * 消息提示
     * @param msg 提示内容
     * @param gravity 显示位置
     * @param offset 偏移值dp
     */
    public static void messageDialog(String msg,int gravity,int offset) {
        QMUITipDialog tipDialog = new QMUITipDialog.Builder(ActivityUtils.instance.getCurrentActivity())
                .setIconType(QMUITipDialog.Builder.ICON_TYPE_INFO)
                .setTipWord(msg)
                .create();
        Window window = tipDialog.getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.y = QMUIDisplayHelper.dp2px(ActivityUtils.instance.getCurrentActivity(),offset);
        window.setAttributes(attributes);
        window.setGravity(gravity);
        tipDialog.show();
        dialogDismiss(tipDialog);
    }

    /**
     * 失败消息提示
     * @param msg
     */
    public static  void faileDialog(Context context,String msg){
        QMUITipDialog tipDialog = new QMUITipDialog.Builder(context)
                .setIconType(QMUITipDialog.Builder.ICON_TYPE_FAIL)
                .setTipWord(msg)
                .create();
        tipDialog.show();
        dialogDismiss(tipDialog);
    }

    /**
     * 成功消息提示
     * @param msg
     */
    public static  void  successDialog(String msg){
        QMUITipDialog tipDialog = new QMUITipDialog.Builder(ActivityUtils.instance.getCurrentActivity())
                .setIconType(QMUITipDialog.Builder.ICON_TYPE_SUCCESS)
                .setTipWord(msg)
                .create();
        tipDialog.show();
        dialogDismiss(tipDialog);
    }
    /**
     * 成功消息提示
     * @param msg
     */
    public static  void  successDialog(Context context,String msg){
        QMUITipDialog tipDialog = new QMUITipDialog.Builder(context)
                .setIconType(QMUITipDialog.Builder.ICON_TYPE_SUCCESS)
                .setTipWord(msg)
                .create();
        tipDialog.show();
        dialogDismiss(tipDialog);
    }

    /**
     * 关闭dialog
     * @param qmuiTipDialog
     */
    private static void dialogDismiss(final QMUITipDialog qmuiTipDialog) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (qmuiTipDialog != null) {

                    qmuiTipDialog.dismiss();
                }
            }
        }, 2000);
    }

//    public static void NFCHintDialog(){
//        new QMUIDialog.CustomDialogBuilder(ActivityUtils.instance.getCurrentActivity())
//                .setLayout(R.layout.dialog_nfc_hint).addAction(UIUtils.getString(R.string.text_cancel), new QMUIDialogAction.ActionListener() {
//            @Override
//            public void onClick(QMUIDialog dialog, int index) {
//                dialog.dismiss();
//            }
//        }).create(R.style.DialogTheme2).show();
//
//    }

}
