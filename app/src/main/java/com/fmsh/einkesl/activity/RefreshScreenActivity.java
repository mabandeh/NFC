package com.fmsh.einkesl.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fmsh.base.adapter.BaseRecyclerAdapter;
import com.fmsh.base.adapter.GridDividerItemDecoration;
import com.fmsh.base.ui.BaseNFCActivity;
import com.fmsh.base.utils.BroadcastManager;
import com.fmsh.base.utils.HintDialog;
import com.fmsh.base.utils.LogUtil;
import com.fmsh.base.utils.NfcConstant;
import com.fmsh.base.utils.UIUtils;
import com.fmsh.einkesl.App;
import com.fmsh.einkesl.R;
import com.fmsh.einkesl.adapter.RefreshScreenAdapter;
import com.fmsh.einkesl.bean.DeviceInfo;
import com.fmsh.einkesl.tools.MyThread;
import com.fmsh.einkesl.tools.image.BMPConverterUtil;
import com.fmsh.einkesl.tools.image.BmpUtils;
import com.fmsh.einkesl.tools.image.ImageScalingUtil;
import com.fmsh.einkesl.tools.image.ImageUtils;
import com.fmsh.einkesl.tools.image.PaperPicture;
import com.fmsh.einkesl.utils.Constants;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.lang.ref.WeakReference;

import butterknife.BindView;

/**
 * @author wuyajiang
 * @date 2021/4/22
 */
public class RefreshScreenActivity extends BaseNFCActivity {
    @BindView(R.id.topbar)
    QMUITopBarLayout topbar;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.image)
    ImageView image;
    private ImageView mImageView;
    private ReceiveHandler mReceiveHandler;
    private String mBmpFilePath;
    private Button mBtnConfirm;
    private SeekBar seekBar;
    private QMUITipDialog mQmuiTipDialog;
    private EditText mEtPin;
    private String mPin;
    private boolean isLvl = true;

    private Bitmap  shakeImage; // 抖动图片
    private Bitmap  colorLevelImage; // 色阶图片


    @Override
    protected int getLayoutId() {
        return R.layout.activity_refresh_screen;
    }

    @Override
    protected void initView() {
        setTitle(UIUtils.getString(mContext, R.string.text_import_preview));
        setBackImage();
        mReceiveHandler = new ReceiveHandler(this);
        mQmuiTipDialog = HintDialog.loadingDialog(mContext, UIUtils.getString(R.string.string_text_write_bmp_11));

        RefreshScreenAdapter refreshScreenAdapter = new RefreshScreenAdapter(mContext);
        View view = UIUtils.loadView(mContext, R.layout.item_header, null);
        mImageView = view.findViewById(R.id.imageView);
        mBtnConfirm = view.findViewById(R.id.btn_confirm);
        RadioButton rb1 = view.findViewById(R.id.rb1);
        RadioButton rb2 = view.findViewById(R.id.rb2);
        RadioGroup rg = view.findViewById(R.id.rg);
        CheckBox   cb1= view.findViewById(R.id.cb1);
        CheckBox   cb2= view.findViewById(R.id.cb2);
        CheckBox   cb5= view.findViewById(R.id.cb5);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (rb1.isChecked()) {
                    isLvl = true;
                    if(colorLevelImage == null){
                        loadImage();
                    }
                    App.getDeviceInfo().setBitmap(colorLevelImage);
                    mImageView.setImageBitmap(colorLevelImage);
                } else {
                    isLvl = false;
                    if(shakeImage == null){
                        loadImage();
                    }
                    App.getDeviceInfo().setBitmap(shakeImage);
                    mImageView.setImageBitmap(shakeImage);
                }


            }
        });
        cb1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            //水平翻转
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Bitmap bm = (isLvl == true)?colorLevelImage:shakeImage;
                if(null != bm) {
                    transformImage(bm, true, false, (char)0);
                    if(isLvl == true)
                    {
                        App.getDeviceInfo().setBitmap(colorLevelImage);
                        mImageView.setImageBitmap(colorLevelImage);
                    }
                    else {
                        App.getDeviceInfo().setBitmap(shakeImage);
                        mImageView.setImageBitmap(shakeImage);
                    }
                }
            }
        });
        cb2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            //垂直翻转
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Bitmap bm = (isLvl == true)?colorLevelImage:shakeImage;
                if(null != bm) {
                    transformImage(bm, false, true, (char)0);
                    if(isLvl == true)
                    {
                        App.getDeviceInfo().setBitmap(colorLevelImage);
                        mImageView.setImageBitmap(colorLevelImage);
                    }
                    else {
                        App.getDeviceInfo().setBitmap(shakeImage);
                        mImageView.setImageBitmap(shakeImage);
                    }
                }
            }
        });
        cb5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            //旋转180度
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Bitmap bm = (isLvl == true)?colorLevelImage:shakeImage;
                if(null != bm) {
                    transformImage(bm, false, false, (char)1);
                    if(isLvl == true)
                    {
                        App.getDeviceInfo().setBitmap(colorLevelImage);
                        mImageView.setImageBitmap(colorLevelImage);
                    }
                    else {
                        App.getDeviceInfo().setBitmap(shakeImage);
                        mImageView.setImageBitmap(shakeImage);
                    }
                }
            }
        });
        seekBar = view.findViewById(R.id.seekBar);
        mEtPin = view.findViewById(R.id.et_pin);
        if(App.getDeviceInfo().getCosVersion() != 2){
            if (App.getDeviceInfo().getPin()) {
                mEtPin.setVisibility(View.VISIBLE);
            } else {
                mEtPin.setVisibility(View.GONE);

            }
        }else {
            mEtPin.setVisibility(View.GONE);
            mEtPin.setText(App.getDeviceInfo().getPinCode());
        }

        seekBar.setVisibility(View.GONE);
        //        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        //        mImageView.setLayoutParams(layoutParams);
        //        mImageView.setAdjustViewBounds(true);
        mBtnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (App.getDeviceInfo().getPin()) {
                    mPin = mEtPin.getText().toString().trim();
                    if (mPin.isEmpty()) {
                        HintDialog.messageDialog(UIUtils.getString(R.string.pin_8));
                        return;
                    }
                    if (mPin.length() % 2 != 0 || !mPin.matches(Constants.HEX_REGULAR)) {
                        HintDialog.faileDialog(mContext, UIUtils.getString(R.string.string_res_65));
                        return;
                    }
                }
                showNfcDialog();
            }
        });
        BitmapFactory.Options options = new BitmapFactory.Options();

        //默认值为false，如果设置成true，那么在解码的时候就不会返回bitmap，即bitmap = null。
        options.inJustDecodeBounds = false;
        //可以复用之前用过的bitmap
        options.inBitmap = null;
        //是该bitmap缓存是否可变，如果设置为true，将可被inBitmap复用
        options.inMutable = true;
        seekBar.setVisibility(View.GONE);
        seekBar.setMax(App.getDeviceInfo().getWidth());
        seekBar.setProgress(App.getDeviceInfo().getWidth() / 8);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBmpFilePath = BmpUtils.binarization(progress);
                Bitmap bitmap = BitmapFactory.decodeFile(mBmpFilePath, options);
                mImageView.setImageBitmap(bitmap);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        refreshScreenAdapter.setHeaderView(view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(mContext, 3);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.addItemDecoration(new GridDividerItemDecoration(mContext, 3, 0));
        recyclerView.setAdapter(refreshScreenAdapter);
        refreshScreenAdapter.setOnItemClickListener(new BaseRecyclerAdapter.OnItemClickListener() {
            @Override
            public void itemClickListener(int position) {
                switch (position) {
                    case 0:
                        ImageUtils.antiColor(mImageView);
                        mBmpFilePath = BmpUtils.getImagePath("/fmtemp1.bmp");
                        break;
                    case 1:
                        ImageUtils.reversal(mImageView);
                        mBmpFilePath = BmpUtils.getImagePath("/fmtemp2.bmp");
                        break;
                    case 2:
                        ImageUtils.horizontalMirror(mImageView);
                        mBmpFilePath = BmpUtils.getImagePath("/fmtemp3.bmp");
                        break;
                    case 3:
                        ImageUtils.verticallyMirror(mImageView);
                        mBmpFilePath = BmpUtils.getImagePath("/fmtemp4.bmp");
                        break;
                    case 4:
                        ImageUtils.grayscale(mImageView);
                        mBmpFilePath = BmpUtils.getImagePath("/fmtemp5.bmp");
                        break;
                    case 5:
                        ImageUtils.binarization(mImageView);
                        mBmpFilePath = BmpUtils.getImagePath("/fmtemp6.bmp");
                        break;
                    case 6:
                        ImageUtils.blackAndWhite(mImageView);
                        mBmpFilePath = BmpUtils.getImagePath("/fmtemp7.bmp");
                        break;
                    case 7:
                        Bitmap bitmap = ((BitmapDrawable) ((ImageView) image).getDrawable()).getBitmap();
                        mImageView.setImageBitmap(bitmap);
                        mBmpFilePath = BmpUtils.getImagePath("/fmtemp.bmp");
                        break;
                    case 8:
                        showNfcDialog();

                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    protected void initData() {
        loadImage();
        App.getDeviceInfo().setBitmap(colorLevelImage);
        isLvl= true;
        loadImage();
        mImageView.setImageBitmap(colorLevelImage);
        BroadcastManager.getInstance(mContext).addAction(NfcConstant.KEY_TAG, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (nfcDialogIsShowing()) {
                    dismissNfcDialog();
                    mQmuiTipDialog.show();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(NfcConstant.KEY_TAG, mTag);
                    bundle.putInt("position", 1);
                    bundle.putString("path", mBmpFilePath);
                    bundle.putString("pin", mPin);
                    bundle.putBoolean("isLvl", isLvl);
                    App.setHandler(mReceiveHandler);
                    UIUtils.sendMessage(bundle, 0, MyThread.getInstance().getMyHandler());

                }
            }
        });

    }



    private void loadImage() {
        BitmapFactory.Options options = new BitmapFactory.Options();

        //默认值为false，如果设置成true，那么在解码的时候就不会返回bitmap，即bitmap = null。
        options.inJustDecodeBounds = false;
        //可以复用之前用过的bitmap
        options.inBitmap = null;
        //是该bitmap缓存是否可变，如果设置为true，将可被inBitmap复用
        options.inMutable = true;
        //符合标签配置数据的图片
        mBmpFilePath = getIntent().getStringExtra("configBmpPath");
        if (mBmpFilePath == null) {
            // 裁剪完之后的图片
            mBmpFilePath = getIntent().getStringExtra("CropImageActivity");
            LogUtil.d("22222");
            if (mBmpFilePath != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(mBmpFilePath, options);
                bitmap = BmpUtils.zoomBitmapEx(bitmap, App.getDeviceInfo().getWidth(), App.getDeviceInfo().getHeight());
                //                 bitmap = ImageScalingUtil.compressBitmapFromPath(mBmpFilePath, App.getDeviceInfo().getWidth(),App.getDeviceInfo().getHeight());
                String ColorDesc =App.getDeviceInfo().getColorDesc();
                if(null == ColorDesc)
                {
                    if (App.getDeviceInfo().getColorCount() == 2) {
                        byte[] bytes = BMPConverterUtil.floydSteinberg(bitmap, App.getDeviceInfo().getDeviceType(), isLvl);
                        BmpUtils.saveBmp(bitmap, BmpUtils.getImagePath("bw.bmp"), bytes);
                        BmpUtils.Convert24bmpToBlackWithebmp(BmpUtils.getImagePath("bw.bmp"), BmpUtils.getImagePath(Constants.IMAGE_BLACK_WHITE_NAME), true);
                        mBmpFilePath = BmpUtils.getImagePath(Constants.IMAGE_BLACK_WHITE_NAME);
                    } else {
                        byte[] bytes = BMPConverterUtil.floydSteinberg(bitmap, App.getDeviceInfo().getDeviceType(), isLvl);
                        mBmpFilePath = BmpUtils.getImagePath("floydSteinberg.bmp");
                        BmpUtils.saveBmp(bitmap, mBmpFilePath, bytes);
                    }
                }
                else {
                    if ((14 == App.getDeviceInfo().getColorDesc().length()) && ("4_color Screen".contentEquals(App.getDeviceInfo().getColorDesc()))) {//四色图处理
                        byte[] bytes = BMPConverterUtil.floydSteinberg(bitmap, App.getDeviceInfo().getDeviceType(), isLvl);
                        mBmpFilePath = BmpUtils.getImagePath("floydSteinberg.bmp");
                        BmpUtils.saveBmp(bitmap, mBmpFilePath, bytes);
                    }
                }

            } else {
                mBmpFilePath = BmpUtils.getImagePath();
                Bitmap bitmap = BitmapFactory.decodeFile(mBmpFilePath, options);
                bitmap = BmpUtils.zoomBitmapEx(bitmap, App.getDeviceInfo().getWidth(), App.getDeviceInfo().getHeight());
                if (App.getDeviceInfo().getColorCount() == 2) {
                    byte[] bytes = BMPConverterUtil.floydSteinberg(bitmap, App.getDeviceInfo().getDeviceType(), isLvl);
                    BmpUtils.saveBmp(bitmap, mBmpFilePath, bytes);
                    BmpUtils.Convert24bmpToBlackWithebmp(mBmpFilePath, BmpUtils.getImagePath(Constants.IMAGE_BLACK_WHITE_NAME), true);
                    mBmpFilePath = BmpUtils.getImagePath(Constants.IMAGE_BLACK_WHITE_NAME);
                } else {

                    byte[] bytes = BMPConverterUtil.floydSteinberg(bitmap, App.getDeviceInfo().getDeviceType(), isLvl);
                    mBmpFilePath = BmpUtils.getImagePath("binarization.bmp");
                    BmpUtils.saveBmp(bitmap, mBmpFilePath, bytes);

                }
            }
            if(isLvl){
                colorLevelImage = BitmapFactory.decodeFile(mBmpFilePath, options);
            }else {
                shakeImage = BitmapFactory.decodeFile(mBmpFilePath, options);

            }
        } else {
            LogUtil.d("1111");
            Bitmap bitmap = BitmapFactory.decodeFile(mBmpFilePath, options);
            mBmpFilePath = BmpUtils.getImagePath("tp.bmp");
            byte[] bytes;
            String ColorDesc = App.getDeviceInfo().getColorDesc();
            if(null == ColorDesc)
            {
                if(App.getDeviceInfo().getColorCount() == 2){
                    bytes= BMPConverterUtil.floydSteinberg(bitmap, App.getDeviceInfo().getDeviceType(), isLvl);
                    BmpUtils.saveBmp(bitmap, mBmpFilePath, bytes);
                    BmpUtils.Convert24bmpToBlackWithebmp(mBmpFilePath, BmpUtils.getImagePath(Constants.IMAGE_BLACK_WHITE_NAME), true);
                    mBmpFilePath = BmpUtils.getImagePath(Constants.IMAGE_BLACK_WHITE_NAME);
                }else {
                    bytes = BMPConverterUtil.floydSteinberg(bitmap, App.getDeviceInfo().getDeviceType(), isLvl);
                    BmpUtils.saveBmp(bitmap, mBmpFilePath, bytes);
                }
            }
            else if((14 == App.getDeviceInfo().getColorDesc().length())&&("4_color Screen".contentEquals(App.getDeviceInfo().getColorDesc())))
            {//四色图处理
                bytes = BMPConverterUtil.floydSteinberg(bitmap, App.getDeviceInfo().getDeviceType(), isLvl);

                BmpUtils.saveBmp(bitmap, mBmpFilePath, bytes);
            }
            if(isLvl){
                colorLevelImage = BitmapFactory.decodeFile(mBmpFilePath, options);
            }else {
                shakeImage = BitmapFactory.decodeFile(mBmpFilePath, options);

            }

            image.setVisibility(View.GONE);
        }
    }
    private void transformImage(Bitmap bm, boolean Hconvert, boolean Vconvert, char Rotate) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        //默认值为false，如果设置成true，那么在解码的时候就不会返回bitmap，即bitmap = null。
        options.inJustDecodeBounds = false;
        //可以复用之前用过的bitmap
        options.inBitmap = null;
        //是该bitmap缓存是否可变，如果设置为true，将可被inBitmap复用
        options.inMutable = true;
        //bitmap = BmpUtils.zoomBitmapEx(bitmap, App.getDeviceInfo().getWidth(), App.getDeviceInfo().getHeight());
        //                 bitmap = ImageScalingUtil.compressBitmapFromPath(mBmpFilePath, App.getDeviceInfo().getWidth(),App.getDeviceInfo().getHeight());
        String ColorDesc =App.getDeviceInfo().getColorDesc();
        if(null == ColorDesc)
        {
                    if (App.getDeviceInfo().getColorCount() == 2) {
                        byte[] bytes = BMPConverterUtil.floydSteinbergTransformer(bm, App.getDeviceInfo().getDeviceType(), isLvl, Hconvert, Vconvert, Rotate);
                        BmpUtils.saveBmp(bm,  BmpUtils.getImagePath("bw.bmp"), bytes);
                        BmpUtils.Convert24bmpToBlackWithebmp(BmpUtils.getImagePath("bw.bmp"), BmpUtils.getImagePath(Constants.IMAGE_BLACK_WHITE_NAME), true);
                        mBmpFilePath = BmpUtils.getImagePath(Constants.IMAGE_BLACK_WHITE_NAME);

                    } else {
                        byte[] bytes = BMPConverterUtil.floydSteinbergTransformer(bm, App.getDeviceInfo().getDeviceType(), isLvl, Hconvert, Vconvert, Rotate);
                        mBmpFilePath = BmpUtils.getImagePath("floydSteinberg.bmp");
                        BmpUtils.saveBmp(bm, mBmpFilePath, bytes);
                    }
        }
        else {
            if ((14 == App.getDeviceInfo().getColorDesc().length()) && ("4_color Screen".contentEquals(App.getDeviceInfo().getColorDesc()))) {//四色图处理
                byte[] bytes = BMPConverterUtil.floydSteinbergTransformer(bm, App.getDeviceInfo().getDeviceType(), isLvl, Hconvert, Vconvert, Rotate);
                mBmpFilePath = BmpUtils.getImagePath("floydSteinberg.bmp");
                BmpUtils.saveBmp(bm, mBmpFilePath, bytes);
            }
        }
        if(isLvl){
            colorLevelImage = BitmapFactory.decodeFile(mBmpFilePath, options);
        }else {
            shakeImage = BitmapFactory.decodeFile(mBmpFilePath, options);

        }
    }
    private void deleteTempImage() {
        try {
            File file = new File(mBmpFilePath);
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deleteTempImage();
    }

    public static class ReceiveHandler extends Handler {
        WeakReference<RefreshScreenActivity> reference;

        public ReceiveHandler(RefreshScreenActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            RefreshScreenActivity activity = (RefreshScreenActivity) reference.get();
            if (null != activity) {
                activity.dismissNfcDialog();
                activity.mQmuiTipDialog.dismiss();
                switch (msg.what) {
                    case 0:
                        HintDialog.successDialog(activity.mContext, (String) msg.obj);
                        break;
                    case 1:
                        break;
                    default:
                        HintDialog.faileDialog(activity.mContext, (String) msg.obj);
                        break;
                }
            }
        }

    }
}
