package com.fmsh.einkesl.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.fmsh.base.ui.BaseNFCActivity;
import com.fmsh.base.utils.BroadcastManager;
import com.fmsh.base.utils.Constant;
import com.fmsh.base.utils.HintDialog;
import com.fmsh.base.utils.NfcConstant;
import com.fmsh.base.utils.UIUtils;
import com.fmsh.einkesl.App;
import com.fmsh.einkesl.R;
import com.fmsh.einkesl.tools.MyThread;

import java.lang.ref.WeakReference;


/**
 * @author wuyajiang
 * @date 2021/5/31
 */
public class CommandActivity extends BaseNFCActivity {
    EditText etApdu;
    Button btnConfirm;
    TextView tvContent;
    private ReceiveHandler mReceiveHandler;
    private String mApdu;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_command;
    }

    @Override
    protected void initView() {
         etApdu = findViewById(R.id.et_apdu);
         btnConfirm= findViewById (R.id.btn_confirm);
         btnConfirm.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 onClick2();
             }
         });
         tvContent= findViewById (R.id.tvContent);
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
                    App.setHandler(mReceiveHandler);
                    UIUtils.sendMessage(bundle, 0, MyThread.getInstance().getMyHandler());
                }
            }
        });
    }

    @Override
    protected void initData() {

    }

    public void onClick2() {
        mApdu = etApdu.getText().toString();
        if (mApdu.isEmpty()) {
            HintDialog.messageDialog(UIUtils.getString(R.string.empty_data));
            return;
        }
        if (mApdu.length() % 2 != 0 || !mApdu.matches(Constant.HEX_REGULAR)) {
            HintDialog.messageDialog(UIUtils.getString(R.string.empty_incorrect));
            return;
        }
        showNfcDialog();
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

                        break;
                    default:
                        HintDialog.faileDialog(activity.mContext, UIUtils.getString(activity.mContext, R.string.text_error));
                        break;
                }
            }
        }

    }
}
