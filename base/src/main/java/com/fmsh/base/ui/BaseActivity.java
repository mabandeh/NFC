package com.fmsh.base.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fmsh.base.R;
import com.fmsh.base.utils.ActivityUtils;
import com.fmsh.base.utils.HintDialog;
import com.fmsh.base.utils.UIUtils;
import com.qmuiteam.qmui.util.QMUIStatusBarHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;



/**
 * @author wuyajiang
 * @date 2020/3/10
 */
public abstract class BaseActivity extends AppCompatActivity {
    public Context mContext;
    public QMUITopBarLayout mTopBar;
    public QMUITipDialog mQmuiTipDialog;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        QMUIStatusBarHelper.translucent(this, UIUtils.getColor(this,R.color.colorPrimary));
        mTopBar = findViewById(R.id.topbar);
        ActivityUtils.instance.addActivity(this);
        this.mContext = this;
        mQmuiTipDialog = HintDialog.loadingDialog(this);
        initView();
        initData();
    }

    protected abstract int getLayoutId();

    protected abstract void initView();

    protected abstract void initData();

    @Override
    protected void onDestroy() {
        ActivityUtils.instance.removeActivity(this);
        super.onDestroy();
    }

    /**
     *设置标题
     * @param title
     */
    public void setTitle(String title){
        if(mTopBar != null){
            mTopBar.setTitle(title);
        }
    }

    /**
     * 设置标题的位置
     * @param Gravity
     */
    public void setTitleGravity(int Gravity ){
        if(mTopBar != null){
            mTopBar.setTitleGravity(Gravity);
        }
    }

    /**
     * 设置是否显示返回按钮
     */
    public void setBackImage(){
        if(mTopBar != null){
            mTopBar.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    public int getCurrentVersionCode(){
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public String getCurrentVersionName(){
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return "1.0";
    }




    public void startActivity(Bundle bundle,Class<?> cla){
        Intent intent = new Intent(this, cla);
        if(bundle != null){
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }
    @Override
    protected void onPause() {
        super.onPause();

    }


}
