package com.fmsh.einkesl.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.fmsh.base.adapter.BaseRecyclerAdapter;
import com.fmsh.base.utils.UIUtils;
import com.fmsh.einkesl.R;



/**
 * @author wuyajiang
 * @date 2021/4/14
 */
public class MainAdapter extends BaseRecyclerAdapter {
    private int[] mTitles = {R.string.text_bind, R.string.text_album,  R.string.text_text, R.string.string_res_39,R.string.text_shot,R.string.text_customer,};

    private int[] imageIds = {R.mipmap.cgx,R.mipmap.carme,R.mipmap.image1,R.mipmap.refresh,R.mipmap.import_image,R.mipmap.command,};
    public MainAdapter(Context context) {
        super(context);
    }

    @Override
    public int getCount() {
        return mTitles.length;
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
        viewHolder.tvContent.setText(UIUtils.getString(mContext,mTitles[position]));

    }

    static
    class ViewHolder extends  RecyclerView.ViewHolder{
        ImageView image;
        TextView tvContent;

        ViewHolder(View view) {
            super(view);
             image = view.findViewById(R.id.image);
            tvContent = view.findViewById(R.id.tvContent);
        }
    }
}
