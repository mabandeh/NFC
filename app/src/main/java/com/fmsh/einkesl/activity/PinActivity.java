package com.fmsh.einkesl.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.fmsh.base.ui.BaseNFCActivity;
import com.fmsh.base.utils.BroadcastManager;
import com.fmsh.base.utils.HintDialog;
import com.fmsh.base.utils.NfcConstant;
import com.fmsh.base.utils.UIUtils;
import com.fmsh.einkesl.App;
import com.fmsh.einkesl.R;
import com.fmsh.einkesl.tools.MyThread;
import com.fmsh.einkesl.utils.Constants;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * @author wuyajiang
 * @date 2021/10/13
 */
public class PinActivity extends BaseNFCActivity {

    @BindView(R.id.topbar)
    QMUITopBarLayout topbar;
    @BindView(R.id.et_old_pin)
    EditText etOldPin;
    @BindView(R.id.et_pin)
    EditText etPin;
    @BindView(R.id.et_confirm_pin)
    EditText etConfirmPin;
    @BindView(R.id.btn_update)
    Button btnUpdate;
    private String mOldPin;
    private String mPin;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_pin;
    }

    @Override
    protected void initView() {
        setTitle(UIUtils.getString(R.string.string_res_63));
        setBackImage();

    }

    @Override
    protected void initData() {
        BroadcastManager.getInstance(mContext).addAction("pin", new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mQmuiTipDialog.dismiss();
                String result = intent.getStringExtra("String");
                if("9000".equals(result)){
                    HintDialog.successDialog(mContext,UIUtils.getString(R.string.string_res_64));
                }else {
                    HintDialog.faileDialog(mContext,UIUtils.getString(R.string.string_res_5)+"("+result+")");
                }

            }
        });

    }


    private boolean checkPinCode() {
        mOldPin = etOldPin.getText().toString().trim();
        mPin = etPin.getText().toString().trim();
        String confirmPin = etConfirmPin.getText().toString().trim();
        if(mOldPin.isEmpty()){
            HintDialog.messageDialog(UIUtils.getString(R.string.pin_4));
            return false;
        }
        if(mOldPin.length()%2 != 0 || !mOldPin.matches(Constants.HEX_REGULAR)){
            HintDialog.faileDialog(mContext,UIUtils.getString(R.string.string_res_65));
            return false;
        }
        if(mPin.isEmpty()){
            HintDialog.messageDialog(UIUtils.getString(R.string.pin_5));
            return false;
        }
        if(mPin.length()%2 != 0 || !mPin.matches(Constants.HEX_REGULAR)){
            HintDialog.faileDialog(mContext,UIUtils.getString(R.string.string_res_65));
            return false;
        }
        if(confirmPin.isEmpty()){
            HintDialog.messageDialog(UIUtils.getString(R.string.pin_6));
            return false;
        }
        if(!mPin.equals(confirmPin)){
            HintDialog.faileDialog(mContext,UIUtils.getString(R.string.pin_7));
            return false;
        }
        return true;


    }


    @OnClick(R.id.btn_update)
    public void onClick() {
        if(checkPinCode()){
            showNfcDialog();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (nfcDialogIsShowing()) {
            dismissNfcDialog();
            mQmuiTipDialog.show();
            Bundle bundle = new Bundle();
            bundle.putParcelable(NfcConstant.KEY_TAG, mTag);
            bundle.putInt("position", 3);
            bundle.putString("oldPin", mOldPin);
            bundle.putString("pin", mPin);
            UIUtils.sendMessage(bundle, 0, MyThread.getInstance().getMyHandler());

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BroadcastManager.getInstance(mContext).destroy("pin");
    }
}
