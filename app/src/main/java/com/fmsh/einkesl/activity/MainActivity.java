package com.fmsh.einkesl.activity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fmsh.base.adapter.BaseRecyclerAdapter;
import com.fmsh.base.adapter.GridDividerItemDecoration;
import com.fmsh.base.ui.BaseNFCActivity;
import com.fmsh.base.utils.BroadcastManager;
import com.fmsh.base.utils.FMUtil;
import com.fmsh.base.utils.HintDialog;
import com.fmsh.base.utils.LogUtil;
import com.fmsh.base.utils.NfcConstant;
import com.fmsh.base.utils.SpUtils;
import com.fmsh.base.utils.UIUtils;
import com.fmsh.einkesl.App;
import com.fmsh.einkesl.R;
import com.fmsh.einkesl.adapter.MainAdapter;
import com.fmsh.einkesl.bean.DeviceInfo;
import com.fmsh.einkesl.tools.MyThread;
import com.fmsh.einkesl.tools.image.BmpUtils;
import com.fmsh.einkesl.tools.image.ImageUtils;
import com.fmsh.einkesl.utils.IUtils;
import com.google.gson.Gson;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.entity.LocalMedia;
import com.qmuiteam.qmui.widget.QMUILoadingView;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.yalantis.ucrop.UCrop;

import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.List;


public class MainActivity extends BaseNFCActivity {

    public ReceiveHandler receiveHandler;
    QMUITopBarLayout topbar;
    RecyclerView recyclerView;
    private QMUIGroupListView mGroupListView;
    private MainAdapter mMainAdapter;

    private QMUICommonListItemView mItemView;
    private QMUICommonListItemView mItemView1;
    private QMUICommonListItemView mItemView2;
    private QMUICommonListItemView mItemView3;
    private QMUICommonListItemView mItemView4;
    private QMUICommonListItemView mItemView5;
    private DeviceInfo mDeviceInfo;
    private QMUICommonListItemView mItemView6;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
         topbar = findViewById(R.id.topbar);
        recyclerView = findViewById(R.id.recyclerView);
        receiveHandler = new ReceiveHandler(this);
        setTitle(UIUtils.getString(mContext, R.string.app_name) + "  " + getCurrentVersionName());
        if (!MyThread.getInstance().isAlive()) {
            MyThread.getInstance().start();
        }
        mGroupListView = new QMUIGroupListView(mContext);
        mMainAdapter = new MainAdapter(mContext);
        mMainAdapter.setHeaderView(mGroupListView);
        int spanCount = 3;
        recyclerView.setLayoutManager(new GridLayoutManager(mContext, spanCount));
        recyclerView.addItemDecoration(new GridDividerItemDecoration(mContext, spanCount, 0));
        recyclerView.setAdapter(mMainAdapter);

        mItemView = createItem("UID");
        mItemView1 = createItem(UIUtils.getString(mContext, R.string.string_rp_id));
        mItemView2 = createItem(UIUtils.getString(mContext, R.string.text_support_color));
        mItemView4 = createItem(UIUtils.getString(mContext, R.string.text_manufacturer));
        mItemView5 = createItem(UIUtils.getString(mContext, R.string.text_color_type));
        mItemView6 = createItem(UIUtils.getString(R.string.pin_update));
        mItemView6.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);
        mItemView6.setVisibility(View.GONE);
        QMUIGroupListView.newSection(mContext)
                .setTitle(UIUtils.getString(mContext, R.string.text_device_info))
                .addItemView(mItemView, null)
                .addItemView(mItemView1, null)
                .addItemView(mItemView2, null)
                .addItemView(mItemView4, null)
                .addItemView(mItemView5, null)
                .addItemView(mItemView6, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(null,PinActivity.class);
                    }
                })
                .addTo(this.mGroupListView);

        mMainAdapter.setOnItemClickListener(new BaseRecyclerAdapter.OnItemClickListener() {
            @Override
            public void itemClickListener(int position) {
                if (position != 0 && mDeviceInfo == null) {
                    HintDialog.messageDialog(UIUtils.getString(mContext, R.string.text_bind_device));
                    return;
                }
                switch (position) {
                    case 0:
                        showNfcDialog();
                        break;
                    case 1:
                        IUtils.selectPicture(MainActivity.this);
                        break;
                    case 4:
                        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                        } else {
                            IUtils.openCamera(MainActivity.this);
                        }
                        break;
                    case 2:
                        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                        } else {

                            startActivity(null, TextGenerateBmpActivity.class);
                        }
                        break;
                    case 3:
                        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 102);
                        } else {
                            startActivity(null, CustomImageActivity.class);
                        }
                        break;
                    case 5:
                        startActivity(null, CommandActivity.class);
                        break;
                    default:
                        break;
                }

            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case 100:
                    IUtils.openCamera(MainActivity.this);
                    break;
                case 101:
                    startActivity(null, TextGenerateBmpActivity.class);
                    break;
                case 102:
                    startActivity(null, CustomImageActivity.class);
                    break;
                default:
                    break;

            }

        }
    }

    @Override
    protected void initData() {
        String info = SpUtils.getStringValue(mContext, "info", "");
        if (!info.isEmpty()) {
            Gson gson = new Gson();
            DeviceInfo deviceInfo = gson.fromJson(info, DeviceInfo.class);
            mDeviceInfo = deviceInfo;
            App.setDeviceInfo(mDeviceInfo);
            loadView();
        }


    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (nfcDialogIsShowing()) {
            dismissNfcDialog();
            Bundle bundle = new Bundle();
            bundle.putParcelable(NfcConstant.KEY_TAG, mTag);
            bundle.putInt("position", 0);
            App.setHandler(receiveHandler);
            UIUtils.sendMessage(bundle, 0, MyThread.getInstance().getMyHandler());
            mItemView.setDetailText(FMUtil.byteToHex(mTag.getId()));
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PictureConfig.CHOOSE_REQUEST:
                    ImageUtils.loadImage(data, mDeviceInfo, MainActivity.this);
                    break;
                case PictureConfig.REQUEST_CAMERA:
                    // 结果回调
                    List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
                    break;
                case UCrop.REQUEST_CROP:
                    if (resultCode == RESULT_OK) {
                        Uri resultUri = UCrop.getOutput(data);
                        try {
                            Bundle bundle = new Bundle();
                            String path = resultUri.getPath();
                            LogUtil.d(path);
                            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(resultUri));
                            Intent intent = new Intent(MainActivity.this, RefreshScreenActivity.class);
                            bundle.putString("CropImageActivity", path);
                            intent.putExtras(bundle);
                            startActivity(intent);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                default:
                    break;
            }
        }

    }


    private QMUICommonListItemView createItem(String text) {
        QMUICommonListItemView itemView = mGroupListView.createItemView(text);
        QMUILoadingView qmuiLoadingView = new QMUILoadingView(mContext);
        itemView.addAccessoryCustomView(qmuiLoadingView);
        return itemView;
    }

    private void loadDeviceInfo(String info) {
        if (info == null) {
            IUtils.showDialogErrorInfo(mContext);
            return;
        }
        mDeviceInfo = IUtils.loadDeviceInfo(mContext, info);

        Gson gson = new Gson();
        String json = gson.toJson(mDeviceInfo);
        SpUtils.putStringValue(mContext, "info", json);
        App.setDeviceInfo(mDeviceInfo);
        loadView();
    }

    private void loadView() {
        mItemView.setDetailText(mDeviceInfo.getUID());
        mItemView1.setDetailText(mDeviceInfo.getScreen() + mDeviceInfo.getScanType());
        if (IUtils.isCN(mContext)) {
            mItemView2.setDetailText(mDeviceInfo.getColor());
        } else {
            mItemView2.setDetailText(mDeviceInfo.getEN_Color());
        }
        mItemView4.setDetailText(mDeviceInfo.getManufacturer());
        mItemView5.setDetailText(mDeviceInfo.getColorType());
        if(mDeviceInfo.getCosVersion() != 2){
            if(mDeviceInfo.getPin() ){
                mItemView6.setVisibility(View.VISIBLE);
            }else {
                mItemView6.setVisibility(View.GONE);
            }
        }else if (mDeviceInfo.getCosVersion() == 2){
            mItemView6.setVisibility(View.GONE);
        }

    }


    public static class ReceiveHandler extends Handler {
        WeakReference<MainActivity> reference;

        public ReceiveHandler(MainActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity activity = (MainActivity) reference.get();
            if (null != activity) {
                activity.dismissNfcDialog();
                switch (msg.what) {
                    case 0:
                        String deviceInfo = (String) msg.obj;
                        LogUtil.d(deviceInfo);
                        activity.loadDeviceInfo(deviceInfo);
                        break;
                    default:
                        HintDialog.faileDialog(activity.mContext, UIUtils.getString(activity.mContext, R.string.text_error));
                        break;
                }
            }
        }

    }
}