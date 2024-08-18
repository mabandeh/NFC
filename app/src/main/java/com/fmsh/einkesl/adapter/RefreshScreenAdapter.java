package com.fmsh.einkesl.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.fmsh.base.adapter.BaseRecyclerAdapter;
import com.fmsh.base.utils.UIUtils;
import com.fmsh.einkesl.R;
import com.fmsh.einkesl.tools.crop.util.UIUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author wuyajiang
 * @date 2021/4/23
 */
public class RefreshScreenAdapter extends BaseRecyclerAdapter {
    private int[] imageIds = {R.mipmap.anti_color,R.mipmap.reversal,R.mipmap.horizontal_mirror,R.mipmap.mirror_vertically,R.mipmap.grayscale,
    R.mipmap.binarization,R.mipmap.black_and_white,R.mipmap.reduction,R.mipmap.import_screen};
    private int[] titlesIds = {R.string.string_res_53,R.string.string_res_52,R.string.string_res_51,R.string.string_res_50,
            R.string.string_res_49,R.string.string_res_48,R.string.string_res_47,R.string.text_reduction,
            R.string.text_import};
    public RefreshScreenAdapter(Context context) {
        super(context);
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_main;
    }

    @Override
    public RecyclerView.ViewHolder getViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public void setBindHolder(RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.image.setImageResource(imageIds[position]);
        viewHolder.tvContent.setText(UIUtils.getString(mContext,titlesIds[position]));

    }

    static
    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.image)
        ImageView image;
        @BindView(R.id.tvContent)
        TextView tvContent;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
