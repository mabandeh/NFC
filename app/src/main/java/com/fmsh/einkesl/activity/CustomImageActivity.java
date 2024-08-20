package com.fmsh.einkesl.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.fmsh.base.ui.BaseActivity;
import com.fmsh.base.utils.CustomDialog;
import com.fmsh.base.utils.UIUtils;
import com.fmsh.einkesl.App;
import com.fmsh.einkesl.R;
import com.fmsh.einkesl.tools.custom.MyDrawView;
import com.fmsh.einkesl.tools.image.BmpUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;

import butterknife.BindView;

/**
 * @author wuyajiang
 * @date 2021/4/23
 */
public class CustomImageActivity extends BaseActivity {
    @BindView(R.id.topbar)
    QMUITopBarLayout topbar;
    @BindView(R.id.drawView)
    MyDrawView drawView;
    @BindView(R.id.floatAB)
    FloatingActionButton floatAB;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_custom_image;
    }

    @Override
    protected void initView() {
        setTitle(UIUtils.getString(mContext, R.string.string_res_39));
        setBackImage();
        drawView.m_SaveBmpwidth = App.getDeviceInfo().getWidth();
        drawView.m_Saveheight = App.getDeviceInfo().getHeight();
        mTopBar.addRightImageButton(R.mipmap.save, 0x11).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BmpUtils.saveBmp(drawView.mSaveBimtp);
                Bundle bundle = new Bundle();
                bundle.putString("CustomImageActivity",BmpUtils.getImagePath());
                startActivity(bundle, RefreshScreenActivity.class);
                finish();

            }
        });
    }

    @Override
    protected void initData() {
        floatAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });
    }
    String[] strWidth = new String[]{"5","10","15","20","25","30","35"};
    int color = Color.BLACK;
    int width = 5;
    private void showDialog() {
        CustomDialog customDialog = new CustomDialog(mContext);
        View view = UIUtils.loadView(mContext, R.layout.dialog_setting, null);
        RadioButton rbBlack = view.findViewById(R.id.rb_black);
        RadioButton rbRed = view.findViewById(R.id.rb_red);
        RadioGroup rg = view.findViewById(R.id.rg);
        Button btn_cancel = view.findViewById(R.id.btn_cancel);
        Button btn_confirm = view.findViewById(R.id.btn_confirm);

        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.rb_black:
                        color = Color.BLACK;
                        break;
                    case R.id.rb_red:
                        color = Color.RED;
                        break;
                    default:
                        break;
                }
            }
        });
        if(App.getDeviceInfo().getColorCount() == 2){
            rbRed.setVisibility(View.INVISIBLE);
        }
        Spinner spinner = view.findViewById(R.id.spinnerBrushWidth);
        //第二个参数表示spinner没有展开前的UI类型
        ArrayAdapter<String> dd = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,strWidth);
        //之前已经通过Spinner spin = (Spinner) findViewById(R.id.spinner);来获取spin对象
        spinner.setAdapter(dd);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                width =  Integer.parseInt(strWidth[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        customDialog.setLayout(view);
        QMUIDialog qmuiDialog = customDialog.create(R.style.DialogTheme2);
        qmuiDialog.show();
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qmuiDialog.dismiss();
            }
        });
        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawView.fontcolor =color;
                drawView.m_StrokeWidth =width;
                qmuiDialog.dismiss();
            }
        });
    }
}
