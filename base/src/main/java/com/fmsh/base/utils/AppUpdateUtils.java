package com.fmsh.base.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.View;
import android.widget.ProgressBar;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.fmsh.base.R;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

import java.io.File;

/**
 * @author wuyajiang
 * @date 2021/4/6
 */
public class AppUpdateUtils {


    public static void newVersionDialog(Context context,UpdateApkListener updateApkListener){
        QMUIDialog.MessageDialogBuilder builder = new QMUIDialog.MessageDialogBuilder(context);
        builder.setTitle(UIUtils.getString(context, R.string.text_explain));
        builder.setMessage(UIUtils.getString(context,R.string.text_app_update_message));

        builder.addAction(UIUtils.getString(context, R.string.text_app_no_update), new QMUIDialogAction.ActionListener() {
            @Override
            public void onClick(QMUIDialog dialog, int index) {
                dialog.dismiss();

            }
        });
        builder.addAction(UIUtils.getString(context, R.string.text_app_update), new QMUIDialogAction.ActionListener() {
            @Override
            public void onClick(QMUIDialog dialog, int index) {
                dialog.dismiss();
                if(updateApkListener != null){
                    updateApkListener.update();
                }
//                downApkDialog(context);
            }
        });
        QMUIDialog qmuiDialog = builder.create(R.style.DialogTheme2);
        qmuiDialog.show();
    }


    public static void installApk(Context context,File file){
        //调用系统安装程序
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        Uri uri= FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".FileProvider",file);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        ActivityUtils.instance.getCurrentActivity().startActivity(intent );
    }


    public void setUpdateApkListener(UpdateApkListener updateApkListener) {
        mUpdateApkListener = updateApkListener;
    }

    private UpdateApkListener mUpdateApkListener;
    public interface UpdateApkListener{
        void update();
    }
}
