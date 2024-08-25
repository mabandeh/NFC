package com.fmsh.base.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wyj on 2018/7/9.
 */
public  abstract class BaseRecyclerAdapter<T> extends RecyclerView.Adapter {
    public Context mContext;
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;


    public static final int TYPE_HEADER = 0;

    public static final int TYPE_FOOTER = 1;

    public static final int TYPE_NORMAL = 2;
    private View mHeaderView;
    private View mFooterView;


    public List<T> mList = new ArrayList<>();
    public BaseRecyclerAdapter(Context context) {
        this.mContext = context;
    }

    public void setList(List<T> list) {
        mList = list;
        notifyDataSetChanged();
    }

    public View getHeaderView() {
        return mHeaderView;
    }
    public void setHeaderView(View headerView) {
        mHeaderView = headerView;
        notifyItemInserted(0);
    }
    public View getFooterView() {
        return mFooterView;
    }
    public void setFooterView(View footerView) {
        mFooterView = footerView;
        notifyItemInserted(getItemCount()-1);
    }

    public abstract int getCount();


    public abstract int getLayoutId();

    public abstract RecyclerView.ViewHolder getViewHolder(View view);

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(mHeaderView != null && viewType == TYPE_HEADER) {
            return new HeaderViewHolder(mHeaderView);
        }
        if(mFooterView != null && viewType == TYPE_FOOTER){
            return new FoogerViewHolder(mFooterView);
        }
        View layout = LayoutInflater.from(parent.getContext()).inflate(getLayoutId(), parent, false);
        return getViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if(mHeaderView != null &&mFooterView != null){
            if(position == 0){
                return;
            }else if (position == getItemCount() -1){
                return;
            }else {
                bindHolder(holder,position-1);
            }
        }else if(mHeaderView !=null &&mFooterView == null){
            if(position == 0){
                return;
            }else {
                bindHolder(holder,position-1);
            }
        }else if(mHeaderView == null &&mFooterView != null){
            if(position == getItemCount() -1){
                return;
            }else {

                bindHolder(holder,position);
            }
        }else {
            bindHolder(holder,position);
        }




    }

    public void bindHolder(RecyclerView.ViewHolder holder, final int position){
        setBindHolder(holder,position);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.itemClickListener(position);
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(mOnItemLongClickListener != null){
                    mOnItemLongClickListener.itemLongClickListener(position);
                }
                return true;
            }
        });
    }
    public abstract void setBindHolder(RecyclerView.ViewHolder holder,int position);

    @Override
    public int getItemViewType(int position) {
        if (mHeaderView == null && mFooterView == null){
            return TYPE_NORMAL;
        }
        if (position == 0){
            //第一个item应该加载Header
            return TYPE_HEADER;
        }
        if (position == getItemCount()-1){
            //最后一个,应该加载Footer
            return TYPE_FOOTER;
        }
        return TYPE_NORMAL;
    }

    @Override
    public int getItemCount() {
        if(mHeaderView == null && mFooterView == null){
            return getCount();
        }else if(mHeaderView == null && mFooterView != null){
            return getCount() + 1;
        }else if (mHeaderView != null && mFooterView == null){
            return getCount() + 1;
        }else {
            return getCount() + 2;
        }
    }

    /**
     * 头部的ViewHolder
     */
    private class HeaderViewHolder extends RecyclerView.ViewHolder
    {
        public HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * 尾部的ViewHolder
     */
    private class FoogerViewHolder extends RecyclerView.ViewHolder
    {
        public FoogerViewHolder(View itemView) {
            super(itemView);
        }
    }


    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        mOnItemLongClickListener = onItemLongClickListener;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if(layoutManager instanceof GridLayoutManager)
        {
            /**
             * getSpanSize的返回值的意思是：position位置的item的宽度占几列
             * 比如总的是4列，然后头部全部显示的话就应该占4列，此时就返回4
             * 其他的只占一列，所以就返回1，剩下的三列就由后面的item来依次填充。
             */
            ((GridLayoutManager) layoutManager).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if(mHeaderView != null && mFooterView != null)
                    {
                        if(position == 0)
                        {
                            return ((GridLayoutManager) layoutManager).getSpanCount();
                        }
                        else if(position == getItemCount() - 1) {
                            return ((GridLayoutManager) layoutManager).getSpanCount();
                        }
                        else
                        {
                            return 1;
                        }
                    }
                    else if(mHeaderView != null) {
                        if (position == 0) {
                            return ((GridLayoutManager) layoutManager).getSpanCount();
                        }
                        return 1;
                    }
                    else if(mFooterView != null)
                    {
                        if(position == getItemCount() - 1)
                        {
                            return ((GridLayoutManager) layoutManager).getSpanCount();
                        }
                        return 1;
                    }
                    return 1;
                }
            });
        }
    }

    public interface OnItemClickListener{
        void itemClickListener(int position);
    }
    public interface OnItemLongClickListener{
        void itemLongClickListener(int position);
    }
}