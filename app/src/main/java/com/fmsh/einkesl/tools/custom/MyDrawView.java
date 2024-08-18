package com.fmsh.einkesl.tools.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;

public class MyDrawView extends AppCompatImageView {
    // 上次触屏的位置
    private int mLastX, mLastY;
    // 当前触屏的位置
    private int mCurrX, mCurrY;
    // 保存每次绘画的结果
    public Bitmap mBitmap=null;
    public Bitmap mSaveBimtp=null;

    //绘图的笔
    private Paint mPaint =null;
    public int m_SaveBmpwidth;
    public int  m_Saveheight;
    Canvas tmpCanvas =null;
    Canvas SaveTempCanvas = null;
    public int fontcolor= Color.BLACK;
    public int m_StrokeWidth=5;
    //构造函数
    public MyDrawView(Context context) {
        super(context);
        //初始化画笔
        mPaint = new Paint();
        mPaint.setStrokeWidth(m_StrokeWidth);



    }



    public MyDrawView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        //初始化画笔
        mPaint = new Paint();
        mPaint.setStrokeWidth(m_StrokeWidth);
    }



    //当前view显示的时候自动回调ondraw方法
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);



        //如果bitmap为空的话，就初始化bitmap
        if (mBitmap == null) {
            //得到当前view的宽度和高度

            mSaveBimtp = Bitmap.createBitmap(m_SaveBmpwidth, m_Saveheight, Bitmap.Config.ARGB_8888);;
            int width = getWidth();
            int height = getHeight();
            mBitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);

        }
        if(tmpCanvas==null) {
            //将之前的bitmap的结果画到当前的页面上
            tmpCanvas = new Canvas(mBitmap);
            SaveTempCanvas = new   Canvas(mSaveBimtp);
            SaveTempCanvas.drawColor(Color.WHITE); //白色
            tmpCanvas.drawColor(Color.WHITE); //白色
        }

        mPaint.setColor(fontcolor);
        float scaleWidth = ((float) m_SaveBmpwidth/ getWidth());
        float scaleHeight = ((float) m_Saveheight /  getHeight());



        mPaint.setStrokeWidth(m_StrokeWidth);
        //在当前的页面上划线
        tmpCanvas.drawLine(mLastX, mLastY, mCurrX, mCurrY, mPaint);




        mPaint.setStrokeWidth( (int)( m_StrokeWidth * ( ( scaleWidth)))  );
        SaveTempCanvas.drawLine((int)( mLastX*scaleWidth), (int)(mLastY*scaleHeight), (int)(mCurrX*scaleWidth), (int)(mCurrY*scaleHeight), mPaint);

        //再把Bitmap画到canvas上
        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
    }

    //当用户触摸此view时自动回调
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //记录当前的x,y坐标
        mLastX = mCurrX;
        mLastY = mCurrY;
        //获取当前点击的位置
        mCurrX = (int) event.getX();
        mCurrY = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = mCurrX;
                mLastY = mCurrY;
                break;
            default:
                break;
        }
        //重绘view
        invalidate();

        return true; // 必须返回true
    }

}
