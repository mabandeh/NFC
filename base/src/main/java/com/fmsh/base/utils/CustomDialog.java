package com.fmsh.base.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.fmsh.base.ui.BaseFragment;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogBuilder;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogView;

/**
 * @author wuyajiang
 * @date 2021/3/22
 */
public class CustomDialog  extends QMUIDialogBuilder {
    private int mLayoutId;
    private View mInflate;

    public CustomDialog(Context context) {
        super(context);
    }

    /**
     * 设置内容区域的 layoutResId
     *
     * @return
     */
    public CustomDialog setLayout(@LayoutRes int layoutResId) {
        mLayoutId = layoutResId;
        return this;
    }

    public View getInflate() {
        return mInflate;
    }

    public CustomDialog setLayout(View view) {
        mInflate = view;
        return this;
    }

    @Nullable
    @Override
    protected View onCreateContent(QMUIDialog dialog, QMUIDialogView parent, Context context) {
        if (mLayoutId != 0) {
            mInflate = LayoutInflater.from(context).inflate(mLayoutId, parent, false);
        }
        return mInflate;
    }
}
