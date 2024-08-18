package com.fmsh.base.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fmsh.base.R;
import com.fmsh.base.utils.UIUtils;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogBuilder;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogView;


/**
 * @author wuyajiang
 * @date 2020/3/10
 */
public abstract class BaseFragment extends Fragment {


    public Context mContext;
    public BaseNFCActivity mActivity;
    public QMUIDialog mQmuiDialog;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
        if(getActivity() instanceof BaseNFCActivity){
            this.mActivity = (BaseNFCActivity) getActivity();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(),container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        initData();

    }

    protected abstract int getLayoutId();

    protected abstract void initView(View view);

    protected abstract void initData();

    public void startActivity(Bundle bundle,Class<?> cla){
        Intent intent = new Intent(getActivity(), cla);
        if(bundle != null){
            intent.putExtras(bundle);
        }
        startActivity(intent);
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


}
