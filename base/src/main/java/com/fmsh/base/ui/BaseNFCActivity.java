package com.fmsh.base.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.system.Os;

import androidx.annotation.Nullable;

import com.fmsh.base.R;
import com.fmsh.base.utils.BroadcastManager;
import com.fmsh.base.utils.LogUtil;
import com.fmsh.base.utils.NfcConstant;
import com.fmsh.base.utils.UIUtils;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

/**
 * @author wuyajiang
 * @date 2020/3/12
 */
public abstract class BaseNFCActivity extends BaseActivity {

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    public Tag mTag;
    private QMUIDialog mQmuiDialog;


    @Override
    protected void onStart() {
        super.onStart();
        if (!hasNfc(this)){
            startAppSettings();
        }
//        startReaderMode();
        if (mNfcAdapter != null) {
            int flag = 0;
//            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
//                flag = PendingIntent.FLAG_IMMUTABLE;
//            }
            mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), flag);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mTag= intent.getExtras().getParcelable(NfcAdapter.EXTRA_TAG);
        Bundle bundle = new Bundle();
        bundle.putParcelable(NfcConstant.KEY_TAG, mTag);
        BroadcastManager.getInstance(mContext).sendBroadcast(NfcConstant.KEY_TAG, bundle);
    }

    public void startReaderMode() {
//        Bundle bundle = new Bundle();
//        bundle.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 0);
//        mNfcAdapter.enableReaderMode(this, new NfcAdapter.ReaderCallback() {
//            @Override
//            public void onTagDiscovered(Tag tag) {
//                String[] techList = tag.getTechList();
//                for (int i = 0; i < techList.length; i++) {
//                    LogUtil.d(techList[i]);
//
//                }
//                mTag = tag;
//                Bundle bundle = new Bundle();
//                bundle.putParcelable(NfcConstant.KEY_TAG, mTag);
//                BroadcastManager.getInstance(mContext).sendBroadcast(NfcConstant.KEY_TAG, bundle);
//            }
//
//        }, NfcAdapter.FLAG_READER_NFC_A, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //设置优先与所有NFC的处理
        if (mNfcAdapter != null) {
            /**
             * 设置前台识别过滤 为null都识别
             */
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
//            mNfcAdapter.disableReaderMode(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BroadcastManager.getInstance(mContext).destroy(NfcConstant.KEY_TAG);
    }



    public void showNfcDialog() {
        if (mQmuiDialog == null) {
            mQmuiDialog = new QMUIDialog.CustomDialogBuilder(mContext)
                    .setLayout(R.layout.dialog_nfc_hint).addAction(UIUtils.getString(mContext,R.string.text_cancel), new QMUIDialogAction.ActionListener() {
                        @Override
                        public void onClick(QMUIDialog dialog, int index) {
                            dialog.dismiss();
                        }
                    }).create(R.style.DialogTheme2);
            mQmuiDialog.setCanceledOnTouchOutside(false);
            mQmuiDialog.show();
        } else {
            mQmuiDialog.show();
        }
    }
    public void dismissNfcDialog(){
        if(mQmuiDialog != null){
            mQmuiDialog.dismiss();
        }
    }

    public boolean nfcDialogIsShowing(){
        if(mQmuiDialog != null){
            return mQmuiDialog.isShowing();
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.d(requestCode+ "---"+resultCode);
        if(requestCode == 88){
            if(!hasNfc(this)){
                showDialog();
            }
        }
    }

    public  boolean hasNfc(Activity context){

        NfcManager manager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
        mNfcAdapter = manager.getDefaultAdapter();
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
            // adapter存在，能启用
            return true;
        }
        return false;

    }
    /**
     * 启动应用的设置
     */
    private void startAppSettings() {
        try {
            Intent intent = new Intent(
                    Settings.ACTION_NFC_SETTINGS);
            startActivityForResult(intent, 88);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    private void showDialog(){
        QMUIDialog qmuiDialog = new QMUIDialog.MessageDialogBuilder(mContext)
                .setTitle(UIUtils.getString(R.string.tips))
                .setMessage(UIUtils.getString(R.string.open_nfc))
                .addAction(UIUtils.getString(R.string.text_cancel), new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        dialog.dismiss();
                        finish();
                    }
                }).addAction(UIUtils.getString(R.string.setting), new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        dialog.dismiss();
                        startAppSettings();

                    }
                }).create();
        qmuiDialog.show();
    }


}
