package com.fmsh.einkesl.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;

import com.bumptech.glide.Glide;
import com.fmsh.base.ui.BaseActivity;
import com.fmsh.base.utils.UIUtils;
import com.fmsh.einkesl.R;
import com.fmsh.einkesl.tools.crop.view.CropImageView;
import com.fmsh.einkesl.tools.image.BmpUtils;

import java.io.File;
import java.io.FileOutputStream;

import butterknife.BindView;

/**
 * @author wuyajiang
 * @date 2021/4/22
 */
public class CropImageActivity extends BaseActivity {
    @BindView(R.id.crop_image)
    CropImageView cropImage;
    private String mBmpPath;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_cropimage;
    }

    @Override
    protected void initView() {
        setTitle(UIUtils.getString(mContext, R.string.string_res_38));
        setBackImage();
        mBmpPath = getIntent().getStringExtra("bmpPath");
        Glide.with(this).load(mBmpPath).into(cropImage);
        mTopBar.addRightImageButton(R.mipmap.save,0x11).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap croppedImage = cropImage.getCroppedImage();
                BmpUtils.saveBmp(croppedImage);
                Bundle bundle = new Bundle();
                bundle.putString("CropImageActivity",BmpUtils.getImagePath());
                startActivity(bundle,RefreshScreenActivity.class);
                finish();

            }
        });
    }



    @Override
    protected void initData() {

    }

}
