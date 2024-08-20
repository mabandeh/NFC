package com.fmsh.einkesl.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.fmsh.base.ui.BaseNFCActivity;
import com.fmsh.base.utils.BroadcastManager;
import com.fmsh.base.utils.Constant;
import com.fmsh.base.utils.HintDialog;
import com.fmsh.base.utils.NfcConstant;
import com.fmsh.base.utils.UIUtils;
import com.fmsh.einkesl.App;
import com.fmsh.einkesl.R;
import com.fmsh.einkesl.tools.MyThread;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * @author wuyajiang
 * @date 2021/5/31
 */
public class CommandActivity extends BaseNFCActivity {
    @BindView(R.id.et_apdu)
    EditText etApdu;
    @BindView(R.id.btn_confirm)
    Button btnConfirm;
    @BindView(R.id.tvContent)
    TextView tvContent;
    @BindView(R.id.topbar)
    QMUITopBarLayout topbar;
    @BindView(R.id.btn_confirm_edep)
    Button btnConfirmEdep;
    private ReceiveHandler mReceiveHandler;
    private String mApdu;
    static private String mResps;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_command;
    }

    @Override
    protected void initView() {
        btnConfirmEdep.setText(btnConfirmEdep.getText().toString()+"(EDEP)");
        mReceiveHandler = new ReceiveHandler(this);
        setTitle(UIUtils.getString(R.string.text_customer));
        setBackImage();
        BroadcastManager.getInstance(mContext).addAction(NfcConstant.KEY_TAG, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (nfcDialogIsShowing()) {
                    dismissNfcDialog();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(NfcConstant.KEY_TAG, mTag);
                    bundle.putInt("position", 2);
                    bundle.putString("apdu", mApdu);
                    bundle.putBoolean("isPin",isEDEP);
                    bundle.putString("pin","1122334455");
                    App.setHandler(mReceiveHandler);
                    UIUtils.sendMessage(bundle, 0, MyThread.getInstance().getMyHandler());
                }
            }
        });
    }

    @Override
    protected void initData() {

    }

    @OnClick({R.id.btn_confirm})
    public void onClick() {

    }

    @OnClick({R.id.btn_confirm, R.id.btn_confirm_edep})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_confirm:
                isEDEP = false;
                break;
            case R.id.btn_confirm_edep:
                isEDEP = true;
                break;
        }
        sendApdu();
    }

    private boolean isEDEP = false;
    private void sendApdu(){

        mApdu = etApdu.getText().toString();
        String[] apdu = mApdu.split(",");
        if (mApdu.isEmpty()) {
            HintDialog.messageDialog(UIUtils.getString(R.string.empty_data));
            return;
        }
        for(int i = 0;i< apdu.length;i++)
        {
            if (apdu[i].length() % 2 != 0 || !apdu[i].matches(Constant.HEX_REGULAR)) {
                HintDialog.messageDialog("APDU" + i + ": " +UIUtils.getString(R.string.empty_incorrect));
                return;
            }
        }

        showNfcDialog();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tvContent.setMovementMethod(new ScrollingMovementMethod());
    }

    public static class ReceiveHandler extends Handler {
        WeakReference<CommandActivity> reference;

        public ReceiveHandler(CommandActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            CommandActivity activity = (CommandActivity) reference.get();
            if (null != activity) {
                activity.dismissNfcDialog();
                switch (msg.what) {
                    case 0:
                        String result = (String) msg.obj;
                        activity.tvContent.setText(result);
                    case 1:
                        mResps += (String) msg.obj;
                        activity.tvContent.setText(mResps);
                        break;
                    case 2:
                        mResps = "";
                        activity.tvContent.setText(mResps);
                        break;
                    default:
                        HintDialog.faileDialog(activity.mContext, UIUtils.getString(activity.mContext, R.string.text_error));
                        break;
                }
            }
        }

    }
}
