package com.fmsh.einkesl.tools.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.os.SystemClock;
import android.os.strictmode.IntentReceiverLeakedViolation;
import android.util.Log;
import android.view.View;

import com.fmsh.base.utils.Constant;
import com.fmsh.base.utils.LogUtil;
import com.fmsh.einkesl.App;
import com.fmsh.einkesl.bean.DeviceInfo;
import com.fmsh.einkesl.utils.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BmpUtils {


    /**
     * 获取文件是否是BMP或者是否是单色
     *
     * @param strFileName
     * @return
     * @author niyijin
     */
    public static boolean GetBmpFormat(String strFileName, DeviceInfo deviceInfo) {

        InputStream in = null;

        int bmpwidth = 0;
        int bmpheight = 0;
        try {
            byte[] bmpdata1 = new byte[64];
            in = new FileInputStream(strFileName);
            int tempbyte = in.read(bmpdata1, 0, bmpdata1.length);
            in.close();
            bmpwidth = (int) ((bmpdata1[0x15]) << 0x24)
                    + (int) ((bmpdata1[0x14]) << 0x16)
                    + (int) (bmpdata1[0x13] << 0x8)
                    + (int) (bmpdata1[0x12] & 0xff);
            bmpheight = (int) ((bmpdata1[0x19]) << 0x24)
                    + (int) ((bmpdata1[0x18]) << 0x16)
                    + (int) (bmpdata1[0x17] << 0x8)
                    + (int) (bmpdata1[0x16] & 0xff);
           if (bmpwidth != deviceInfo.getWidth() || bmpheight != deviceInfo.getHeight()) {
                return false;
            }
            // 格式错
            if (bmpdata1[0] != (byte) 0x42 || bmpdata1[1] != (byte) 0x4d
                    || bmpdata1[0xe] != (byte) 0x28
                    || bmpdata1[0xf] != (byte) 0x0
                    || bmpdata1[0x10] != (byte) 0x0
                    || bmpdata1[0x11] != (byte) 0x0

            ) { ///不是bmp图片
                return false;
            }
            int color = -1;
            if (bmpdata1[0x1c] == 0x18) {
                //24色
                color = 3;
            } else if (bmpdata1[0x1c] == 1) {
                //单色
                color = 2;
            }
            if (deviceInfo.getColorCount() != color) {
                String ColorDesc = App.getDeviceInfo().getColorDesc();
                if(null != ColorDesc)
                {
                    if((4 == deviceInfo.getColorCount())&&(14 == deviceInfo.getColorDesc().length())&&("4_color Screen".contentEquals(deviceInfo.getColorDesc())))
                    {
                        return true;
                    }
                }
                else if(deviceInfo.getColorCount() > 4)
                {
                    return true;
                }
                return false;
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }
    /***/
    /**
     * 图片去色,返回灰度图片
     *
     * @param bmpOriginal 传入的图片
     * @return 去色后的图片
     */
    public static Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    /**
     * 将彩色图转换为灰度图
     *
     * @param img 位图
     * @return 返回转换好的位图
     */
    public static Bitmap convertGreyImg(Bitmap img) {
        int width = img.getWidth();         //获取位图的宽
        int height = img.getHeight();       //获取位图的高

        int[] pixels = new int[width * height]; //通过位图的大小创建像素点数组

        img.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                grey = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    /**
     * 垂直镜像
     *
     * @param a
     * @return
     */
    public static Bitmap convertVerMirro(Bitmap a) {
        int w = a.getWidth();
        int h = a.getHeight();
        Bitmap newb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
        Canvas cv = new Canvas(newb);
        Matrix m = new Matrix();
        m.postScale(1, -1);   //镜像垂直翻转
        // m.postScale(-1, 1);   //镜像水平翻转
        // m.postRotate(-90);  //旋转-90度
        Bitmap new2 = Bitmap.createBitmap(a, 0, 0, w, h, m, true);
        cv.drawBitmap(new2, new Rect(0, 0, new2.getWidth(), new2.getHeight()), new Rect(0, 0, w, h), null);
        return newb;
    }

    /**
     * 水平镜像
     *
     * @param a
     * @return
     */
    public static Bitmap convertHorMirro(Bitmap a) {
        int w = a.getWidth();
        int h = a.getHeight();
        Bitmap newb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
        Canvas cv = new Canvas(newb);
        Matrix m = new Matrix();
        //m.postScale(1, -1);   //镜像垂直翻转
        m.postScale(-1, 1);   //镜像水平翻转
        // m.postRotate(-90);  //旋转-90度
        Bitmap new2 = Bitmap.createBitmap(a, 0, 0, w, h, m, true);
        cv.drawBitmap(new2, new Rect(0, 0, new2.getWidth(), new2.getHeight()), new Rect(0, 0, w, h), null);
        return newb;
    }

    /**
     * 将彩色图转换为黑白图
     *
     * @param
     * @return 返回转换好的位图
     */
    public static Bitmap convertToBlackWhite(Bitmap bmp) {
        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高
        int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组

        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);

        //Bitmap resizeBmp = ThumbnailUtils.extractThumbnail(newBmp, 380, 460);
        return newBmp;
    }


    /**
     * 图片进行二值化黑白
     */
    public static Bitmap zeroAndOne(Bitmap bm) {
        int width = bm.getWidth();//原图像宽度
        int height = bm.getHeight();//原图像高度
        int color;//用来存储某个像素点的颜色值
        int r, g, b, a;//红，绿，蓝，透明度
        //创建空白图像，宽度等于原图宽度，高度等于原图高度，用ARGB_8888渲染，这个不用了解，这样写就行了
        Bitmap bmp = Bitmap.createBitmap(width, height
                , Bitmap.Config.ARGB_8888);

        int[] oldPx = new int[width * height];//用来存储原图每个像素点的颜色信息
        int[] newPx = new int[width * height];//用来处理处理之后的每个像素点的颜色信息
        /**
         * 第一个参数oldPix[]:用来接收（存储）bm这个图像中像素点颜色信息的数组
         * 第二个参数offset:oldPix[]数组中第一个接收颜色信息的下标值
         * 第三个参数width:在行之间跳过像素的条目数，必须大于等于图像每行的像素数
         * 第四个参数x:从图像bm中读取的第一个像素的横坐标
         * 第五个参数y:从图像bm中读取的第一个像素的纵坐标
         * 第六个参数width:每行需要读取的像素个数
         * 第七个参数height:需要读取的行总数
         */
        bm.getPixels(oldPx, 0, width, 0, 0, width, height);//获取原图中的像素信息

        for (int i = 0; i < width * height; i++) {//循环处理图像中每个像素点的颜色值
            color = oldPx[i];//取得某个点的像素值
            r = Color.red(color);//取得此像素点的r(红色)分量
            g = Color.green(color);//取得此像素点的g(绿色)分量
            b = Color.blue(color);//取得此像素点的b(蓝色分量)
            a = Color.alpha(color);//取得此像素点的a通道值

            //此公式将r,g,b运算获得灰度值，经验公式不需要理解
            int gray = (int) ((float) r * 0.3 + (float) g * 0.59 + (float) b * 0.11);
            //下面前两个if用来做溢出处理，防止灰度公式得到到灰度超出范围（0-255）
            if (gray > 255) {
                gray = 255;
            }

            if (gray < 0) {
                gray = 0;
            }

            if (gray != 0) {//如果某像素的灰度值不是0(黑色)就将其置为255（白色）
                gray = 255;
            }

            newPx[i] = Color.argb(a, gray, gray, gray);//将处理后的透明度（没变），r,g,b分量重新合成颜色值并将其存储在数组中
        }
        /**
         * 第一个参数newPix[]:需要赋给新图像的颜色数组//The colors to write the bitmap
         * 第二个参数offset:newPix[]数组中第一个需要设置给图像颜色的下标值//The index of the first color to read from pixels[]
         * 第三个参数width:在行之间跳过像素的条目数//The number of colors in pixels[] to skip between rows.
         * Normally this value will be the same as the width of the bitmap,but it can be larger(or negative).
         * 第四个参数x:从图像bm中读取的第一个像素的横坐标//The x coordinate of the first pixels to write to in the bitmap.
         * 第五个参数y:从图像bm中读取的第一个像素的纵坐标//The y coordinate of the first pixels to write to in the bitmap.
         * 第六个参数width:每行需要读取的像素个数The number of colors to copy from pixels[] per row.
         * 第七个参数height:需要读取的行总数//The number of rows to write to the bitmap.
         */
        bmp.setPixels(newPx, 0, width, 0, 0, width, height);//将处理后的像素信息赋给新图
        return bmp;//返回处理后的图像
    }

    public static Bitmap invertBitmap(Bitmap bitmap) {
        int sWidth; //width
        int sHeight;  //height
        int sRow; //Row--height
        int sCol; //col--width
        int sPixel = 0;
        int sIndex;

        //ARGB values
        int sA = 0;
        int sR = 0;
        int sG = 0;
        int sB = 0;

        int[] sPixels;

        sWidth = bitmap.getWidth();
        sHeight = bitmap.getHeight();
        sPixels = new int[sWidth * sHeight];
        bitmap.getPixels(sPixels, 0, sWidth, 0, 0, sWidth, sHeight);

        sIndex = 0;
        for (sRow = 0; sRow < sHeight; sRow++) {
            sIndex = sRow * sWidth;
            for (sCol = 0; sCol < sWidth; sCol++) {
                sPixel = sPixels[sIndex];
                sA = (sPixel >> 24) & 0xff;
                sR = (sPixel >> 16) & 0xff;
                sG = (sPixel >> 8) & 0xff;
                sB = sPixel & 0xff;

                sR = 255 - sR;
                sG = 255 - sG;
                sB = 255 - sB;

                sPixel = ((sA & 0xff) << 24 | (sR & 0xff) << 16 | (sG & 0xff) << 8 | sB & 0xff);
                sPixels[sIndex] = sPixel;
                sIndex++;
            }
        }
        bitmap.setPixels(sPixels, 0, sWidth, 0, 0, sWidth, sHeight);
        return bitmap;
    }

    /**
     * 把一个View的对象转换成bitmap
     */
    public static Bitmap getViewBitmap(View v) {
        v.clearFocus();
        v.setPressed(false);

        //能画缓存就返回false
        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);
        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null) {
            Log.e("BtPrinter", "failed getViewBitmap(" + v + ")", new RuntimeException());
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);
        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);
        return bitmap;
    }

    /**
     * 将指定的图，缩放到相应的宽高图
     *
     * @param oldbitmap
     * @param w
     * @param h
     * @return
     */
    public static Bitmap zoomBitmap(Bitmap oldbitmap, int w, int h) {
        int width = oldbitmap.getWidth();
        int height = oldbitmap.getHeight();
        //        Log.d("kkk","1");
        //        Log.d("kkk",String.format("%d",width));
        //        Log.d("kkk",String.format("%d",height));
        //        Log.d("kkk",String.format("%d",w));
        //        Log.d("kkk",String.format("%d",h));


        Matrix matrix = new Matrix();
        float scaleWidth = ((float) w / width);
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newbmp = Bitmap.createBitmap(oldbitmap, 0, 0, width, height,
                matrix, false);
        Log.d("kkk", "3");

        return newbmp;
    }


    /**
     * 通过缩略图的方式
     *
     * @param oldbitmap
     * @param w
     * @param h
     * @return
     */
    public static Bitmap zoomBitmapEx(Bitmap oldbitmap, int w, int h) {
        Bitmap newbmp = ThumbnailUtils.extractThumbnail(oldbitmap, w, h);
        return newbmp;
    }


    /**
     * 计算字体的宽度
     *
     * @param paint
     * @param str
     * @return
     */
    public static int getTextWidth(Paint paint, String str) {
        int iRet = 0;
        if (str != null && str.length() > 0) {
            int len = str.length();
            float[] widths = new float[len];
            paint.getTextWidths(str, widths);
            for (int j = 0; j < len; j++) {
                iRet += (int) Math.ceil(widths[j]);
            }
        }
        return iRet;
    }


    /**
     * 获取图片临时存放的目录路径
     *
     * @return
     */
    public static String GetSdDirPath() {
        String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        sdcardPath = sdcardPath + "/data";
        File file = new File(sdcardPath);

        //如果文件夹不存在的话就创建一下
        if (!file.exists()) {
            file.mkdir();
        }
        if (file.exists()) {
            sdcardPath = sdcardPath + "/com.fm";
            file = new File(sdcardPath);
            file.mkdir();
            if (file.exists()) {
                return sdcardPath;
            } else {
                return null;
            }
        }
        return null;
    }

    public static String getImagePath() {
        return GetSdDirPath() + Constants.IMAGE_NAME;
    }

    public static String getImagePath(String path) {
        return GetSdDirPath() + path;
    }

    /**
     * 缩放到指定的大小
     *
     * @param drawable
     * @param w
     * @param h
     * @return
     */
    public static Drawable zoomDrawable(Drawable drawable, int w, int h) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap oldbmp = drawableToBitmap(drawable);

        Matrix matrix = new Matrix();
        float scaleWidth = ((float) w / width);
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height,
                matrix, true);
        return new BitmapDrawable(null, newbmp);
    }


    /**
     * 把
     *
     * @param drawable
     * @return
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    public static boolean saveBmpEx(Bitmap bitmap) {
        if (bitmap == null)
            return false;
        int nBmpWidth = bitmap.getWidth();
        int nBmpHeight = bitmap.getHeight();
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        return true;
    }

    public static void saveBmp(Bitmap bitmap) {
        saveBmp(bitmap, getImagePath());
    }

    /**
     * 将Bitmap存为 .bmp格式图片
     *
     * @param bitmap
     */
    public static void saveBmp(Bitmap bitmap, String filename) {
        if (bitmap == null)
            return;
        // 位图大小
        int nBmpWidth = bitmap.getWidth();
        int nBmpHeight = bitmap.getHeight();
        // 图像数据大小
        int bufferSize = nBmpHeight * (nBmpWidth * 3 + nBmpWidth % 4);
        try {
            // 存储文件名
            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
            } else {
                file.delete();
                file.createNewFile(); //重新创建一下，防止没有覆盖
            }
            FileOutputStream fileos = new FileOutputStream(filename);
            // bmp文件头
            int bfType = 0x4d42;
            long bfSize = 14 + 40 + bufferSize;
            int bfReserved1 = 0;
            int bfReserved2 = 0;
            long bfOffBits = 14 + 40;
            // 保存bmp文件头
            writeWord(fileos, bfType);
            writeDword(fileos, bfSize);
            writeWord(fileos, bfReserved1);
            writeWord(fileos, bfReserved2);
            writeDword(fileos, bfOffBits);
            // bmp信息头
            long biSize = 40L;
            long biWidth = nBmpWidth;
            long biHeight = nBmpHeight;
            int biPlanes = 1;
            int biBitCount = 24;
            long biCompression = 0L;
            long biSizeImage = 0L;
            long biXpelsPerMeter = 0L;
            long biYPelsPerMeter = 0L;
            long biClrUsed = 0L;
            long biClrImportant = 0L;
            // 保存bmp信息头
            writeDword(fileos, biSize);
            writeLong(fileos, biWidth);
            writeLong(fileos, biHeight);
            writeWord(fileos, biPlanes);
            writeWord(fileos, biBitCount);
            writeDword(fileos, biCompression);
            writeDword(fileos, biSizeImage);
            writeLong(fileos, biXpelsPerMeter);
            writeLong(fileos, biYPelsPerMeter);
            writeDword(fileos, biClrUsed);
            writeDword(fileos, biClrImportant);

            byte bmpData[] = new byte[bufferSize];
            int wWidth = (nBmpWidth * 3 + nBmpWidth % 4);
            for (int nCol = 0, nRealCol = nBmpHeight - 1; nCol < nBmpHeight; ++nCol, --nRealCol) {
                for (int wRow = 0, wByteIdex = 0; wRow < nBmpWidth; wRow++, wByteIdex += 3) {


                    int clr = bitmap.getPixel(wRow, nCol);
                    bmpData[nRealCol * wWidth + wByteIdex] = (byte) Color.blue(clr);
                    bmpData[nRealCol * wWidth + wByteIdex + 1] = (byte) Color.green(clr);
                    bmpData[nRealCol * wWidth + wByteIdex + 2] = (byte) Color.red(clr);
                }
            }
            fileos.write(bmpData);
            fileos.flush();
            fileos.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String binarization(int radius) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //默认值为false，如果设置成true，那么在解码的时候就不会返回bitmap，即bitmap = null。
        options.inJustDecodeBounds = false;
        //可以复用之前用过的bitmap
        options.inBitmap = null;
        //是该bitmap缓存是否可变，如果设置为true，将可被inBitmap复用
        options.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeFile(getImagePath(), options);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // 图像数据大小
        int bufferSize = height * (width * 3 + width % 4);
        //        int p = height % 8;
        //        if(p != 0){
        //            p = 8-p;
        //        }
        //        height = height+p;
        //        int bufferSize = width * height * 3;
        int sum;
        int count = 0;
        int bmpData[] = new int[width * height];
        for (int y = 0; y < height; y++) {
            sum = 0;
            for (int x = 0; x < width; x++) {
                int color = bitmap.getPixel(x, y);
                //                int B = Color.blue(color);
                //                int G = Color.green(color);
                //                int R = Color.red(color);

                int B = Color.blue(color);
                int G = Color.green(color);
                int R = Color.red(color);
                int Gray = (R * 30 + G * 59 + B * 11 + 50) / 100;
                sum += Gray;
                if (y == 0) {
                    bmpData[count] = sum;
                } else {
                    bmpData[count] = bmpData[count - width] + sum;
                }
                count++;
            }

        }

        byte[] result = new byte[bufferSize];
        int invertThresholdValue = 100 - 5;
        int x, y, x1, x2, y1, y2, y2y1;
        int IndexOne, IndexTwo;
        int index = 0;

        for (y = 0; y < height; y++) {
            //半径最上方y
            y1 = y - radius;
            //半径最下方y
            y2 = y + radius;
            if (y1 < 0) {
                y1 = 0;
            }
            if (y2 >= height) {
                y2 = height - 1;
            }
            IndexOne = y1 * width;
            IndexTwo = y2 * width;
            y2y1 = (y2 - y1) * 100;
            for (x = 0; x < width; x++) {
                //半径最左方
                x1 = x - radius;
                //半径最右方
                x2 = x + radius;
                if (x1 < 0) {
                    x1 = 0;
                }
                if (x2 >= width) {
                    x2 = width - 1;
                }
                count = (x2 - x1) * y2y1;
                int color = bitmap.getPixel(x, y);
                int B = Color.blue(color);
                int G = Color.green(color);
                int R = Color.red(color);
                int Gray = (R * 30 + G * 59 + B * 11 + 50) / 100;
                sum = bmpData[IndexTwo + x2] - bmpData[IndexOne + x2] - bmpData[IndexTwo + x1] - bmpData[IndexOne + x1];

                if (App.getDeviceInfo().getColorCount() == 2) {
                    if (Gray * count < sum * invertThresholdValue) {

                        result[index + 0] = 0x00;
                        result[index + 1] = 0x00;
                        result[index + 2] = 0x00;
                    } else {
                        result[index + 0] = (byte) 0xFF;
                        result[index + 1] = (byte) 0xFF;
                        result[index + 2] = (byte) 0xFF;

                    }
                } else {
                    if (R > 100 && Gray < 120) {
                        result[index + 0] = (byte) 0x00;
                        result[index + 1] = (byte) 0x00;
                        result[index + 2] = (byte) 0xFF;
                    } else {
                        if (Gray * count < sum * invertThresholdValue) {

                            result[index + 0] = 0x00;
                            result[index + 1] = 0x00;
                            result[index + 2] = 0x00;
                        } else {
                            result[index + 0] = (byte) 0xFF;
                            result[index + 1] = (byte) 0xFF;
                            result[index + 2] = (byte) 0xFF;

                        }
                    }

                }
                index += 3;
            }
        }

        try {
            // 存储文件名
            String name = "binarization.bmp";
            File file = new File(getImagePath(name));
            if (!file.exists()) {
                file.createNewFile();
            } else {
                file.delete();
                file.createNewFile(); //重新创建一下，防止没有覆盖
            }
            FileOutputStream fileos = new FileOutputStream(getImagePath(name));
            // bmp文件头
            int bfType = 0x4d42;
            long bfSize = 14 + 40 + bufferSize;
            int bfReserved1 = 0;
            int bfReserved2 = 0;
            long bfOffBits = 14 + 40;
            // 保存bmp文件头
            writeWord(fileos, bfType);
            writeDword(fileos, bfSize);
            writeWord(fileos, bfReserved1);
            writeWord(fileos, bfReserved2);
            writeDword(fileos, bfOffBits);
            // bmp信息头
            long biSize = 40L;
            long biWidth = bitmap.getWidth();
            long biHeight = bitmap.getHeight();
            int biPlanes = 1;
            int biBitCount = 24;
            long biCompression = 0L;
            long biSizeImage = 0L;
            long biXpelsPerMeter = 0L;
            long biYPelsPerMeter = 0L;
            long biClrUsed = 0L;
            long biClrImportant = 0L;
            // 保存bmp信息头
            writeDword(fileos, biSize);
            writeLong(fileos, biWidth);
            writeLong(fileos, biHeight);
            writeWord(fileos, biPlanes);
            writeWord(fileos, biBitCount);
            writeDword(fileos, biCompression);
            writeDword(fileos, biSizeImage);
            writeLong(fileos, biXpelsPerMeter);
            writeLong(fileos, biYPelsPerMeter);
            writeDword(fileos, biClrUsed);
            writeDword(fileos, biClrImportant);

            //            byte[] bmpdata1 = new byte[bufferSize];
            //
            //            int i = 0;
            //            for (int j = bitmap.getHeight() - 1; j >= 0; j--) {
            //                System.arraycopy(result, j * (wWidth * 3), bmpdata1,
            //                        i * (wWidth * 3), (wWidth * 3));
            //                i += 1;
            //            }

            byte bmpData1[] = new byte[bufferSize];
            int wWidth = (width * 3 + width % 4);
            int i = 0;
            for (int nCol = 0, nRealCol = height - 1; nCol < height; ++nCol, --nRealCol) {
                for (int wRow = 0, wByteIdex = 0; wRow < width; wRow++, wByteIdex += 3) {

                    bmpData1[nRealCol * wWidth + wByteIdex] = result[i];
                    bmpData1[nRealCol * wWidth + wByteIdex + 1] = result[i + 1];
                    bmpData1[nRealCol * wWidth + wByteIdex + 2] = result[i + 2];
                    i = i + 3;
                }
            }
            fileos.write(bmpData1);
            fileos.flush();
            fileos.close();
            if (App.getDeviceInfo().getColorCount() == 2) {

                Convert24bmpToBlackWithebmp(getImagePath(name), getImagePath(Constants.IMAGE_BLACK_WHITE_NAME), true);
                return getImagePath(Constants.IMAGE_BLACK_WHITE_NAME);
            }
            return getImagePath(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * 将Bitmap存为 .bmp格式图片
     *
     * @param bitmap
     */
    public static void saveBmp(Bitmap bitmap, String filename, byte[] data) {
        if (bitmap == null)
            return;
        // 位图大小
        int nBmpWidth = bitmap.getWidth();
        int nBmpHeight = bitmap.getHeight();
        // 图像数据大小
        int bufferSize = nBmpHeight * (nBmpWidth * 3 + nBmpWidth % 4);
        try {
            // 存储文件名
            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
            } else {
                file.delete();
                file.createNewFile(); //重新创建一下，防止没有覆盖
            }
            FileOutputStream fileos = new FileOutputStream(filename);
            // bmp文件头
            int bfType = 0x4d42;
            long bfSize = 14 + 40 + bufferSize;
            int bfReserved1 = 0;
            int bfReserved2 = 0;
            long bfOffBits = 14 + 40;
            // 保存bmp文件头
            writeWord(fileos, bfType);
            writeDword(fileos, bfSize);
            writeWord(fileos, bfReserved1);
            writeWord(fileos, bfReserved2);
            writeDword(fileos, bfOffBits);
            // bmp信息头
            long biSize = 40L;
            long biWidth = nBmpWidth;
            long biHeight = nBmpHeight;
            int biPlanes = 1;
            int biBitCount = 24;
            long biCompression = 0L;
            long biSizeImage = 0L;
            long biXpelsPerMeter = 0L;
            long biYPelsPerMeter = 0L;
            long biClrUsed = 0L;
            long biClrImportant = 0L;
            // 保存bmp信息头
            writeDword(fileos, biSize);
            writeLong(fileos, biWidth);
            writeLong(fileos, biHeight);
            writeWord(fileos, biPlanes);
            writeWord(fileos, biBitCount);
            writeDword(fileos, biCompression);
            writeDword(fileos, biSizeImage);
            writeLong(fileos, biXpelsPerMeter);
            writeLong(fileos, biYPelsPerMeter);
            writeDword(fileos, biClrUsed);
            writeDword(fileos, biClrImportant);
            // 像素扫描
            //            byte bmpData[] = new byte[bufferSize];
            //            int wWidth = (nBmpWidth * 3 + nBmpWidth % 4);
            //            for (int nCol = 0, nRealCol = nBmpHeight - 1; nCol < nBmpHeight; ++nCol, --nRealCol)
            //                for (int wRow = 0, wByteIdex = 0; wRow < nBmpWidth; wRow++, wByteIdex += 3) {
            //                    int clr = bitmap.getPixel(wRow, nCol);
            //                    bmpData[nRealCol * wWidth + wByteIdex] = (byte) Color.blue(clr);
            //                    bmpData[nRealCol * wWidth + wByteIdex + 1] = (byte) Color.green(clr);
            //                    bmpData[nRealCol * wWidth + wByteIdex + 2] = (byte) Color.red(clr);
            //                }

            fileos.write(data);
            fileos.flush();
            fileos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void  saveBlackWitheBmp(String strDestBmpFile, byte[] data){
        byte [] result = new byte[data.length+0x3E];
        System.arraycopy(data,0,result,0x3E,data.length);
        int width = App.getDeviceInfo().getWidth();
        int height = App.getDeviceInfo().getHeight();

        byte[] head = new byte[0x3E];
        result[0] = 0x42;
        result[1] = 0x4D; // 文件标识
        result[2] = (byte) (result.length & 0xff);
        result[3] = (byte) ((result.length & 0xff00) >> 8);
        result[4] = (byte) ((result.length & 0xff0000) >> 16);
        result[5] = (byte) ((result.length & 0xff000000) >> 24);//4个字节的整个文件大小
        result[0xa] = 0x3e;//从文件开始到位图数据开始之间的数据(bitmap data)之间的偏移值

        result[0xe] = 0x28;//位图信息头  一般都是0x28

        result[0x12] = (byte) ((width & 0xff));
        result[0x13] = (byte) ((width & 0xff00) >> 8);
        result[0x14] = (byte) ((width & 0xff0000) >> 16);
        result[0x15] = (byte) ((width & 0xff000000) >> 24);
        result[0x16] = (byte) ((height & 0xff));
        result[0x17] = (byte) ((height & 0xff00) >> 8);
        result[0x18] = (byte) ((height & 0xff0000) >> 16);
        result[0x19] = (byte) ((height & 0xff000000) >> 24);
        result[0x1a] = 0x1; //位图的位面

        result[0x1c] = 0x1;
        result[0x22] = (byte) 0xe0;
        result[0x23] = (byte) 0x0b;

        result[0x3a] = (byte) 0xFF;
        result[0x3b] = (byte) 0xFF;
        result[0x3c] = (byte) 0xFF;

        try {
            //写文件
            FileOutputStream fileos = new FileOutputStream(strDestBmpFile);
            fileos.write(result);
            fileos.flush();
            fileos.close();
        } catch (Exception e) {
           e.printStackTrace();
        }

    }

    public static void writeWord(FileOutputStream stream, int value) throws IOException {
        byte[] b = new byte[2];
        b[0] = (byte) (value & 0xff);
        b[1] = (byte) (value >> 8 & 0xff);
        stream.write(b);
    }

    public static void writeDword(FileOutputStream stream, long value) throws IOException {
        byte[] b = new byte[4];
        b[0] = (byte) (value & 0xff);
        b[1] = (byte) (value >> 8 & 0xff);
        b[2] = (byte) (value >> 16 & 0xff);
        b[3] = (byte) (value >> 24 & 0xff);
        stream.write(b);
    }

    public static void writeLong(FileOutputStream stream, long value) throws IOException {
        byte[] b = new byte[4];
        b[0] = (byte) (value & 0xff);
        b[1] = (byte) (value >> 8 & 0xff);
        b[2] = (byte) (value >> 16 & 0xff);
        b[3] = (byte) (value >> 24 & 0xff);
        stream.write(b);
    }

    /**
     * 读24位真彩色图片  原始的
     *
     * @param StrFileName
     * @param width
     * @param height
     * @return
     */
    public static byte[] ReadBmp24FileOrg(String StrFileName, int width, int height) {
        int p = width % 4;
        int other = 0;
        if (p != 0) {
            other = p * height;
        }
        byte[] bmpdata1 = new byte[width * height * 3 + 0x36 + other];


        InputStream in = null;
        try {
            in = new FileInputStream(StrFileName);
            int tempbyte = in.read(bmpdata1, 0, bmpdata1.length);

            //Toast.makeText(this, "read", Toast.LENGTH_LONG).show();
            in.close();
            // 格式错
            if (bmpdata1[0] != (byte) 0x42 || bmpdata1[1] != (byte) 0x4d
                    || bmpdata1[0xe] != (byte) 0x28
                    || bmpdata1[0xf] != (byte) 0x0
                    || bmpdata1[0x10] != (byte) 0x0
                    || bmpdata1[0x11] != (byte) 0x0

            ) {
                //Toast.makeText(this, "ReadBmpFile error1", Toast.LENGTH_LONG).show();
                return null;
            }
            in.close();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        //Toast.makeText(this, "read end", Toast.LENGTH_LONG).show();
        return bmpdata1;
    }


    /**
     * ReadBmp24File
     *
     * @param StrFileName
     * @param width
     * @param height
     * @return
     */
    public static byte[] ReadBmp24File(String StrFileName, int width, int height) {

        int p = width % 4;
        int other = 0;
        if (p != 0) {
            other = p * height;
        }
        byte[] bmpdata1 = new byte[width * height * 3 + 0x36 + other];
        byte[] bmpdata = new byte[width * height * 3 + 0x36];

        InputStream in = null;
        try {
            in = new FileInputStream(StrFileName);
            int tempbyte = in.read(bmpdata1, 0, bmpdata1.length);

            //Toast.makeText(this, "read", Toast.LENGTH_LONG).show();
            in.close();
            // 格式错
            if (bmpdata1[0] != (byte) 0x42 || bmpdata1[1] != (byte) 0x4d
                    || bmpdata1[0xe] != (byte) 0x28
                    || bmpdata1[0xf] != (byte) 0x0
                    || bmpdata1[0x10] != (byte) 0x0
                    || bmpdata1[0x11] != (byte) 0x0

            ) {
                //Toast.makeText(this, "ReadBmpFile error1", Toast.LENGTH_LONG).show();
                return null;
            }

            int i = 0;
            for (int j = height - 1; j >= 0; j--) {

                System.arraycopy(bmpdata1, 0x36 + j * (width * 3) + j * p, bmpdata,
                        0x36 + i * (width * 3), (width * 3));
                i += 1;

            }

            in.close();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        //Toast.makeText(this, "read end", Toast.LENGTH_LONG).show();
        return bmpdata;
    }


    /**
     * 把24位真彩色的图片转换成  黑白单色的图
     *
     * @return
     */
    public static boolean Convert24bmpToBlackWithebmp(String strSrcBmpFile, String strDestBmpFile, boolean EnableGray) {
        int width = App.getDeviceInfo().getWidth();
        int height = App.getDeviceInfo().getHeight();
        byte[] srcBmpData = ReadBmp24FileOrg(strSrcBmpFile, width, height);
        if (srcBmpData == null) {
            return false;
        }
        //计算目标黑白图的实际的字节快读
        int destRealWidth = (int) Math.ceil(width / 8.0);
        int destWidth = 0;
        int percent = destRealWidth % 4;
        if (percent != 0) {
            destWidth += destRealWidth - percent + 4;
        } else {
            destWidth = destRealWidth;
        }
        //int vbitsRow = (int) Math.ceil(height / 8.0);

        int datasize = 0x3E + destWidth * height;
        byte[] colorData1 = new byte[datasize];
        ; //获取颜色数据
        getColorDataBmp24(srcBmpData, width, height, colorData1, destWidth, destRealWidth, EnableGray);

        colorData1[0] = 0x42;
        colorData1[1] = 0x4D; // 文件标识
        colorData1[2] = (byte) (datasize & 0xff);
        colorData1[3] = (byte) ((datasize & 0xff00) >> 8);
        colorData1[4] = (byte) ((datasize & 0xff0000) >> 16);
        colorData1[5] = (byte) ((datasize & 0xff000000) >> 24);//4个字节的整个文件大小
        colorData1[6] = 0x00;
        colorData1[7] = 0x00;
        colorData1[8] = 0x00;
        colorData1[9] = 0x00; //保留字节
        colorData1[0xa] = 0x3e;//从文件开始到位图数据开始之间的数据(bitmap data)之间的偏移值
        colorData1[0xb] = 0x00;
        colorData1[0xc] = 0x00;
        colorData1[0xd] = 0x00;
        colorData1[0xe] = 0x28;//位图信息头  一般都是0x28
        colorData1[0xf] = 0x00;
        colorData1[0x10] = 0x00;
        colorData1[0x11] = 0x00;
        colorData1[0x12] = (byte) ((width & 0xff));
        colorData1[0x13] = (byte) ((width & 0xff00) >> 8);
        colorData1[0x14] = (byte) ((width & 0xff0000) >> 16);
        colorData1[0x15] = (byte) ((width & 0xff000000) >> 24);
        colorData1[0x16] = (byte) ((height & 0xff));
        colorData1[0x17] = (byte) ((height & 0xff00) >> 8);
        ;
        colorData1[0x18] = (byte) ((height & 0xff0000) >> 16);
        ;
        colorData1[0x19] = (byte) ((height & 0xff000000) >> 24);
        colorData1[0x1a] = 0x1; //位图的位面
        colorData1[0x1b] = 0x00;
        colorData1[0x1c] = 0x1;
        colorData1[0x1d] = 0x00;
        colorData1[0x1e] = 0x00;//压缩说明
        colorData1[0x1f] = 0x00;
        colorData1[0x20] = 0x0;
        colorData1[0x21] = 0x0;
        colorData1[0x22] = (byte) 0xe0;
        colorData1[0x23] = (byte) 0x0b;
        colorData1[0x24] = 0x00;
        ;
        colorData1[0x25] = 0x00;
        colorData1[0x26] = 0x00;
        colorData1[0x27] = 0x00;
        colorData1[0x28] = 0x00;
        colorData1[0x29] = 0x00;
        colorData1[0x2a] = 0x00;
        colorData1[0x2b] = 0x00;
        colorData1[0x2c] = 0x00;
        colorData1[0x2d] = 0x00;
        colorData1[0x2e] = 0x00;
        colorData1[0x2f] = 0x00;
        colorData1[0x30] = 0x0;
        colorData1[0x31] = (byte) 0x0;
        colorData1[0x32] = (byte) 0x0;
        colorData1[0x33] = 0x00;
        colorData1[0x34] = 0x00;
        colorData1[0x35] = 0x00;
        colorData1[0x36] = 0x00;
        colorData1[0x37] = 0x00;
        colorData1[0x38] = 0x00;
        colorData1[0x39] = 0x00;
        colorData1[0x3a] = (byte) 0xFF;
        colorData1[0x3b] = (byte) 0xFF;
        colorData1[0x3c] = (byte) 0xFF;
        colorData1[0x3d] = 0x00;
        try {
            //写文件
            FileOutputStream fileos = new FileOutputStream(strDestBmpFile);
            fileos.write(colorData1);
            fileos.close();
            File file = new File(strSrcBmpFile);
            file.delete();

        } catch (Exception e) {
            return false;
        }

        return true;

    }


    public static void ArrayFillZero(byte[] arrdata) {
        for (int i = 0; i < arrdata.length; i++) {
            arrdata[i] = 0x00;
        }
    }

    /**
     * 根据颜色bit 生成颜色字节
     *
     * @param corlorbits
     * @return
     */
    private static byte GetByteValue(byte[] corlorbits) {
        return (byte) (corlorbits[7] | (corlorbits[6] << 1) |
                (corlorbits[5] << 2) |
                (corlorbits[4] << 3) |
                (corlorbits[3] << 4) |
                (corlorbits[2] << 5) |
                (corlorbits[1] << 6) |
                (corlorbits[0] << 7));
    }

    /**
     * 从24位图片里取色
     *
     * @param bmpdata1
     * @param srcwidth
     * @param srcHeight
     * @param colorData1
     * @param destwidth     黑白文件中的数据宽度字节数
     * @param destRealwidth 实际有效的颜色数据长度宽度
     */
    public static void getColorDataBmp24(byte[] bmpdata1, int srcwidth, int srcHeight,
                                         byte[] colorData1,
                                         int destwidth,
                                         int destRealwidth,
                                         boolean enablegray) {
        final int locoffset = (int) 0x36;//24位真彩色的偏移
        byte[] singlebmpdata1 = new byte[8];
        // byte[] singlebmpdata2 = new byte[8];
        int p = srcwidth % 4;
        int other = 0;
        if (p != 0) {
            other = p * srcHeight;
        }

        int j = 0;
        int datsize = srcwidth * srcHeight * 3 + other;
        int a = 0, b = 0;
        int offset = 0;
        int linecount = 0;
        while (offset < datsize) {
            if (enablegray) {
                int gray = (int) ((float) (bmpdata1[locoffset + offset] & 0xff) * 0.3 + (float) (bmpdata1[locoffset + offset + 1] & 0xff) * 0.59 + (float) (bmpdata1[locoffset + offset + 2] & 0xff) * 0.11);
                if (gray < 180) { //180比较 好的数字，没有意义，就是偏向白色
                    singlebmpdata1[j++] = (byte) (App.getDeviceInfo().getWhite() > 0 ? 1:0);
                } else {
                    singlebmpdata1[j++] = (byte) (App.getDeviceInfo().getBlack() > 0 ? 1:0);
                }
            } else {
                // 取黑白
                if (bmpdata1[locoffset + offset] == (byte) 0xff && bmpdata1[locoffset + offset + 1] == (byte) 0xff && bmpdata1[locoffset + offset + 2] == (byte) 0xff) {
                    singlebmpdata1[j++] = (byte) (App.getDeviceInfo().getBlack() > 0 ? 1:0);
                } else {
                    singlebmpdata1[j++] = (byte) (App.getDeviceInfo().getWhite() > 0 ? 1:0);
                }
            }
            //            // 取黑白
            //            if (bmpdata1[locoffset + offset] == (byte) 0xff && bmpdata1[locoffset +offset + 1] == (byte) 0xff && bmpdata1[locoffset + offset + 2] == (byte) 0xff) {
            //                singlebmpdata1[j++] = 1;
            //            } else {
            //                if(enablegray){
            //                    int gray = (int) ((float) (bmpdata1[locoffset + offset] & 0xff) * 0.3 + (float) (bmpdata1[locoffset +offset + 1] & 0xff) * 0.59 + (float) (bmpdata1[locoffset + offset + 2] & 0xff) * 0.11);
            //                    if (gray < 180) { //180比较 好的数字，没有意义，就是偏向白色
            //                        singlebmpdata1[j++] = 1;
            //                    } else {
            //                        singlebmpdata1[j++] = 0;
            //                    }
            //                }
            //                else{
            //                    singlebmpdata1[j++] = 0;
            //                }
            //            }
            offset += 3;
            linecount += 1;
            if (linecount == srcwidth) {
                linecount = 0;
                offset += p;
                colorData1[0x3E + a + b] = GetByteValue(singlebmpdata1);
                j = 0;
                ;
                a = 0;
                b += destwidth;
                ArrayFillZero(singlebmpdata1);

                continue;
            }


            if (j == 8) {
                colorData1[0x3E + a + b] = GetByteValue(singlebmpdata1);
                //                		(byte) (singlebmpdata1[7] | (singlebmpdata1[6] << 1) |
                //                        (singlebmpdata1[5] << 2) |
                //                        (singlebmpdata1[4] << 3) |
                //                        (singlebmpdata1[3] << 4) |
                //                        (singlebmpdata1[2] << 5) |
                //                        (singlebmpdata1[1] << 6) |
                //                        (singlebmpdata1[0] << 7));
                j = 0;

                a += 1;
                if (a == destRealwidth) {
                    a = 0;
                    b += destwidth;
                }
            }
        }
    }


    /**
     * 根据给定的宽和高进行拉伸
     *
     * @param origin    原图
     * @param newWidth  新图的宽
     * @param newHeight 新图的高
     * @return new Bitmap
     */
    private Bitmap scaleBitmap(Bitmap origin, int newWidth, int newHeight) {
        if (origin == null) {
            return null;
        }
        int height = origin.getHeight();
        int width = origin.getWidth();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);// 使用后乘
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (!origin.isRecycled()) {
            origin.recycle();
        }
        return newBM;
    }

    /**
     * 按比例缩放图片
     *
     * @param origin 原图
     * @param ratio  比例
     * @return 新的bitmap
     */
    private Bitmap scaleBitmap(Bitmap origin, float ratio) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.preScale(ratio, ratio);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    /**
     * 裁剪
     *
     * @param bitmap 原图
     * @return 裁剪后的图像
     */
    private Bitmap cropBitmap(Bitmap bitmap) {
        int w = bitmap.getWidth(); // 得到图片的宽，高
        int h = bitmap.getHeight();
        int cropWidth = w >= h ? h : w;// 裁切后所取的正方形区域边长
        cropWidth /= 2;
        int cropHeight = (int) (cropWidth / 1.2);
        return Bitmap.createBitmap(bitmap, w / 3, 0, cropWidth, cropHeight, null, false);
    }

    public static Bitmap DrawRectOnBitmap(Bitmap bitmap) {
        int w = bitmap.getWidth(); // 得到图片的宽，高
        int h = bitmap.getHeight();
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint();
        p.setStrokeWidth(1);
        p.setColor(Color.BLACK);
        canvas.drawRect(new Rect(1, 1, w - 1, h - 1), p);
        canvas.save();
        canvas.restore();
        return bitmap;
    }


    /**
     * 选择变换
     *
     * @param origin 原图
     * @param alpha  旋转角度，可正可负
     * @return 旋转后的图片
     */
    private Bitmap rotateBitmap(Bitmap origin, float alpha) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(alpha);
        // 围绕原地进行旋转
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    /**
     * 偏移效果
     *
     * @param origin 原图
     * @return 偏移后的bitmap
     */
    private Bitmap skewBitmap(Bitmap origin) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.postSkew(-0.6f, -0.3f);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    /**
     * 素描效果
     *
     * @param bmp
     * @return
     */
    public static Bitmap convertToSketch(Bitmap bmp) {
        int pos, row, col, clr;
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int[] pixSrc = new int[width * height];
        int[] pixNvt = new int[width * height];
        // 先对图象的像素处理成灰度颜色后再取反
        bmp.getPixels(pixSrc, 0, width, 0, 0, width, height);
        for (row = 0; row < height; row++) {
            for (col = 0; col < width; col++) {
                pos = row * width + col;
                pixSrc[pos] = (Color.red(pixSrc[pos])
                        + Color.green(pixSrc[pos]) + Color.blue(pixSrc[pos])) / 3;
                pixNvt[pos] = 255 - pixSrc[pos];
            }
        }
        // 对取反的像素进行高斯模糊, 强度可以设置，暂定为5.0
        gaussGray(pixNvt, 5.0, 5.0, width, height);
        // 灰度颜色和模糊后像素进行差值运算
        for (row = 0; row < height; row++) {
            for (col = 0; col < width; col++) {
                pos = row * width + col;
                clr = pixSrc[pos] << 8;
                clr /= 256 - pixNvt[pos];
                clr = Math.min(clr, 255);
                pixSrc[pos] = Color.rgb(clr, clr, clr);
            }
        }
        bmp.setPixels(pixSrc, 0, width, 0, 0, width, height);
        return bmp;
    }

    private static int gaussGray(int[] psrc, double horz, double vert,
                                 int width, int height) {
        int[] dst, src;
        double[] n_p, n_m, d_p, d_m, bd_p, bd_m;
        double[] val_p, val_m;
        int i, j, t, k, row, col, terms;
        int[] initial_p, initial_m;
        double std_dev;
        int row_stride = width;
        int max_len = Math.max(width, height);
        int sp_p_idx, sp_m_idx, vp_idx, vm_idx;
        val_p = new double[max_len];
        val_m = new double[max_len];
        n_p = new double[5];
        n_m = new double[5];
        d_p = new double[5];
        d_m = new double[5];
        bd_p = new double[5];
        bd_m = new double[5];
        src = new int[max_len];
        dst = new int[max_len];
        initial_p = new int[4];
        initial_m = new int[4];
        // 垂直方向
        if (vert > 0.0) {
            vert = Math.abs(vert) + 1.0;
            std_dev = Math.sqrt(-(vert * vert) / (2 * Math.log(1.0 / 255.0)));
            // 初试化常量
            findConstants(n_p, n_m, d_p, d_m, bd_p, bd_m, std_dev);
            for (col = 0; col < width; col++) {
                for (k = 0; k < max_len; k++) {
                    val_m[k] = val_p[k] = 0;
                }
                for (t = 0; t < height; t++) {
                    src[t] = psrc[t * row_stride + col];
                }
                sp_p_idx = 0;
                sp_m_idx = height - 1;
                vp_idx = 0;
                vm_idx = height - 1;
                initial_p[0] = src[0];
                initial_m[0] = src[height - 1];
                for (row = 0; row < height; row++) {
                    terms = (row < 4) ? row : 4;
                    for (i = 0; i <= terms; i++) {
                        val_p[vp_idx] += n_p[i] * src[sp_p_idx - i] - d_p[i]
                                * val_p[vp_idx - i];
                        val_m[vm_idx] += n_m[i] * src[sp_m_idx + i] - d_m[i]
                                * val_m[vm_idx + i];
                    }
                    for (j = i; j <= 4; j++) {
                        val_p[vp_idx] += (n_p[j] - bd_p[j]) * initial_p[0];
                        val_m[vm_idx] += (n_m[j] - bd_m[j]) * initial_m[0];
                    }
                    sp_p_idx++;
                    sp_m_idx--;
                    vp_idx++;
                    vm_idx--;
                }
                transferGaussPixels(val_p, val_m, dst, 1, height);
                for (t = 0; t < height; t++) {
                    psrc[t * row_stride + col] = dst[t];
                }
            }
        }
        // 水平方向
        if (horz > 0.0) {
            horz = Math.abs(horz) + 1.0;
            if (horz != vert) {
                std_dev = Math.sqrt(-(horz * horz)
                        / (2 * Math.log(1.0 / 255.0)));
                // 初试化常量
                findConstants(n_p, n_m, d_p, d_m, bd_p, bd_m, std_dev);
            }
            for (row = 0; row < height; row++) {
                for (k = 0; k < max_len; k++) {
                    val_m[k] = val_p[k] = 0;
                }
                for (t = 0; t < width; t++) {
                    src[t] = psrc[row * row_stride + t];
                }
                sp_p_idx = 0;
                sp_m_idx = width - 1;
                vp_idx = 0;
                vm_idx = width - 1;
                initial_p[0] = src[0];
                initial_m[0] = src[width - 1];
                for (col = 0; col < width; col++) {
                    terms = (col < 4) ? col : 4;
                    for (i = 0; i <= terms; i++) {
                        val_p[vp_idx] += n_p[i] * src[sp_p_idx - i] - d_p[i]
                                * val_p[vp_idx - i];
                        val_m[vm_idx] += n_m[i] * src[sp_m_idx + i] - d_m[i]
                                * val_m[vm_idx + i];
                    }
                    for (j = i; j <= 4; j++) {
                        val_p[vp_idx] += (n_p[j] - bd_p[j]) * initial_p[0];
                        val_m[vm_idx] += (n_m[j] - bd_m[j]) * initial_m[0];
                    }
                    sp_p_idx++;
                    sp_m_idx--;
                    vp_idx++;
                    vm_idx--;
                }
                transferGaussPixels(val_p, val_m, dst, 1, width);
                for (t = 0; t < width; t++) {
                    psrc[row * row_stride + t] = dst[t];
                }
            }
        }
        return 0;
    }

    private static void transferGaussPixels(double[] src1, double[] src2,
                                            int[] dest, int bytes, int width) {
        int i, j, k, b;
        int bend = bytes * width;
        double sum;
        i = j = k = 0;
        for (b = 0; b < bend; b++) {
            sum = src1[i++] + src2[j++];
            if (sum > 255)
                sum = 255;
            else if (sum < 0)
                sum = 0;
            dest[k++] = (int) sum;
        }
    }

    private static void findConstants(double[] n_p, double[] n_m, double[] d_p,
                                      double[] d_m, double[] bd_p, double[] bd_m, double std_dev) {
        double div = Math.sqrt(2 * 3.141593) * std_dev;
        double x0 = -1.783 / std_dev;
        double x1 = -1.723 / std_dev;
        double x2 = 0.6318 / std_dev;
        double x3 = 1.997 / std_dev;
        double x4 = 1.6803 / div;
        double x5 = 3.735 / div;
        double x6 = -0.6803 / div;
        double x7 = -0.2598 / div;
        int i;
        n_p[0] = x4 + x6;
        n_p[1] = (Math.exp(x1)
                * (x7 * Math.sin(x3) - (x6 + 2 * x4) * Math.cos(x3)) + Math
                .exp(x0) * (x5 * Math.sin(x2) - (2 * x6 + x4) * Math.cos(x2)));
        n_p[2] = (2
                * Math.exp(x0 + x1)
                * ((x4 + x6) * Math.cos(x3) * Math.cos(x2) - x5 * Math.cos(x3)
                * Math.sin(x2) - x7 * Math.cos(x2) * Math.sin(x3)) + x6
                * Math.exp(2 * x0) + x4 * Math.exp(2 * x1));
        n_p[3] = (Math.exp(x1 + 2 * x0)
                * (x7 * Math.sin(x3) - x6 * Math.cos(x3)) + Math.exp(x0 + 2
                * x1)
                * (x5 * Math.sin(x2) - x4 * Math.cos(x2)));
        n_p[4] = 0.0;
        d_p[0] = 0.0;
        d_p[1] = -2 * Math.exp(x1) * Math.cos(x3) - 2 * Math.exp(x0)
                * Math.cos(x2);
        d_p[2] = 4 * Math.cos(x3) * Math.cos(x2) * Math.exp(x0 + x1)
                + Math.exp(2 * x1) + Math.exp(2 * x0);
        d_p[3] = -2 * Math.cos(x2) * Math.exp(x0 + 2 * x1) - 2 * Math.cos(x3)
                * Math.exp(x1 + 2 * x0);
        d_p[4] = Math.exp(2 * x0 + 2 * x1);
        for (i = 0; i <= 4; i++) {
            d_m[i] = d_p[i];
        }
        n_m[0] = 0.0;
        for (i = 1; i <= 4; i++) {
            n_m[i] = n_p[i] - d_p[i] * n_p[0];
        }
        double sum_n_p, sum_n_m, sum_d;
        double a, b;
        sum_n_p = 0.0;
        sum_n_m = 0.0;
        sum_d = 0.0;
        for (i = 0; i <= 4; i++) {
            sum_n_p += n_p[i];
            sum_n_m += n_m[i];
            sum_d += d_p[i];
        }
        a = sum_n_p / (1.0 + sum_d);
        b = sum_n_m / (1.0 + sum_d);
        for (i = 0; i <= 4; i++) {
            bd_p[i] = d_p[i] * a;
            bd_m[i] = d_m[i] * b;
        }
    }

    /**
     * 图片锐化（拉普拉斯变换）
     *
     * @param bmp
     * @return
     */
    public static Bitmap sharpenImageAmeliorate(Bitmap bmp) {
        // 拉普拉斯矩阵
        int[] laplacian = new int[]{-1, -1, -1, -1, 9, -1, -1, -1, -1};

        int width = bmp.getWidth();
        int height = bmp.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.RGB_565);

        int pixR = 0;
        int pixG = 0;
        int pixB = 0;

        int pixColor = 0;

        int newR = 0;
        int newG = 0;
        int newB = 0;

        int idx = 0;
        float alpha = 0.3F;
        int[] pixels = new int[width * height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 1, length = height - 1; i < length; i++) {
            for (int k = 1, len = width - 1; k < len; k++) {
                idx = 0;
                for (int m = -1; m <= 1; m++) {
                    for (int n = -1; n <= 1; n++) {
                        pixColor = pixels[(i + n) * width + k + m];
                        pixR = Color.red(pixColor);
                        pixG = Color.green(pixColor);
                        pixB = Color.blue(pixColor);

                        newR = newR + (int) (pixR * laplacian[idx] * alpha);
                        newG = newG + (int) (pixG * laplacian[idx] * alpha);
                        newB = newB + (int) (pixB * laplacian[idx] * alpha);
                        idx++;
                    }
                }

                newR = Math.min(255, Math.max(0, newR));
                newG = Math.min(255, Math.max(0, newG));
                newB = Math.min(255, Math.max(0, newB));

                pixels[i * width + k] = Color.argb(255, newR, newG, newB);
                newR = 0;
                newG = 0;
                newB = 0;
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /**
     * 将bitmap转换成bmp格式图片 单色黑白图片
     *
     * @param bitmap 要转换的bitmap
     * @paramfosbmp文件输出流
     */
    public static void formatBMPBlackWhite(Bitmap bitmap) {
        if (bitmap != null) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(getImagePath(Constants.IMAGE_BLACK_WHITE_NAME));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            int w = bitmap.getWidth(), h = bitmap.getHeight();
            int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);//取得BITMAP的所有像素点

            byte[] rgb = addBMP_RGB_888(pixels, w, h);
            byte[] header = addBMPImageHeader(62 + rgb.length);
            byte[] infos = addBMPImageInfosHeader(w, h, rgb.length);
            byte[] colortable = addBMPImageColorTable();

            byte[] buffer = new byte[62 + rgb.length];

            System.arraycopy(header, 0, buffer, 0, header.length);
            System.arraycopy(infos, 0, buffer, 14, infos.length);
            System.arraycopy(colortable, 0, buffer, 54, colortable.length);
            System.arraycopy(rgb, 0, buffer, 62, rgb.length);
            try {
                fos.write(buffer);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    // BMP文件头
    private static byte[] addBMPImageHeader(int size) {
        byte[] buffer = new byte[14];
        buffer[0] = 0x42;
        buffer[1] = 0x4D;
        buffer[2] = (byte) (size >> 0);
        buffer[3] = (byte) (size >> 8);
        buffer[4] = (byte) (size >> 16);
        buffer[5] = (byte) (size >> 24);
        buffer[6] = 0x00;
        buffer[7] = 0x00;
        buffer[8] = 0x00;
        buffer[9] = 0x00;
        //buffer[10] = 0x36;
        buffer[10] = 0x3E;
        buffer[11] = 0x00;
        buffer[12] = 0x00;
        buffer[13] = 0x00;
        return buffer;
    }

    // BMP文件信息头
    private static byte[] addBMPImageInfosHeader(int w, int h, int size) {

        Log.i("_DETEST_", "size=" + size);
        byte[] buffer = new byte[40];
        buffer[0] = 0x28;
        buffer[1] = 0x00;
        buffer[2] = 0x00;
        buffer[3] = 0x00;

        buffer[4] = (byte) (w >> 0);
        buffer[5] = (byte) (w >> 8);
        buffer[6] = (byte) (w >> 16);
        buffer[7] = (byte) (w >> 24);

        buffer[8] = (byte) (h >> 0);
        buffer[9] = (byte) (h >> 8);
        buffer[10] = (byte) (h >> 16);
        buffer[11] = (byte) (h >> 24);

        buffer[12] = 0x01;
        buffer[13] = 0x00;

        buffer[14] = 0x01;
        buffer[15] = 0x00;

        buffer[16] = 0x00;
        buffer[17] = 0x00;
        buffer[18] = 0x00;
        buffer[19] = 0x00;

        buffer[20] = (byte) (size >> 0);
        buffer[21] = (byte) (size >> 8);
        buffer[22] = (byte) (size >> 16);
        buffer[23] = (byte) (size >> 24);

        //buffer[24] = (byte) 0xE0;
        //buffer[25] = 0x01;
        buffer[24] = (byte) 0xC3;
        buffer[25] = 0x0E;
        buffer[26] = 0x00;
        buffer[27] = 0x00;

        //buffer[28] = 0x02;
        //buffer[29] = 0x03;
        buffer[28] = (byte) 0xC3;
        buffer[29] = 0x0E;
        buffer[30] = 0x00;
        buffer[31] = 0x00;

        buffer[32] = 0x00;
        buffer[33] = 0x00;
        buffer[34] = 0x00;
        buffer[35] = 0x00;

        buffer[36] = 0x00;
        buffer[37] = 0x00;
        buffer[38] = 0x00;
        buffer[39] = 0x00;
        return buffer;
    }

    private static byte[] addBMPImageColorTable() {
        byte[] buffer = new byte[8];
        buffer[0] = (byte) 0xFF;
        buffer[1] = (byte) 0xFF;
        buffer[2] = (byte) 0xFF;
        buffer[3] = 0x00;

        buffer[4] = 0x00;
        buffer[5] = 0x00;
        buffer[6] = 0x00;
        buffer[7] = 0x00;
        return buffer;
    }


    private static byte[] addBMP_RGB_888(int[] b, int w, int h) {
        int len = w * h;
        int bufflen = 0;
        byte[] tmp = new byte[3];
        int index = 0, bitindex = 1;

        if (w * h % 8 != 0)//将8字节变成1个字节,不足补0
        {
            bufflen = w * h / 8 + 1;
        } else {
            bufflen = w * h / 8;
        }
        if (bufflen % 4 != 0)//BMP图像数据大小，必须是4的倍数，图像数据大小不是4的倍数时用0填充补足
        {
            bufflen = bufflen + bufflen % 4;
        }

        byte[] buffer = new byte[bufflen];

        for (int i = len - 1; i >= w; i -= w) {
            // DIB文件格式最后一行为第一行，每行按从左到右顺序
            int end = i, start = i - w + 1;
            for (int j = start; j <= end; j++) {

                tmp[0] = (byte) (b[j] >> 0);
                tmp[1] = (byte) (b[j] >> 8);
                tmp[2] = (byte) (b[j] >> 16);

                String hex = "";
                for (int g = 0; g < tmp.length; g++) {
                    String temp = Integer.toHexString(tmp[g] & 0xFF);
                    if (temp.length() == 1) {
                        temp = "0" + temp;
                    }
                    hex = hex + temp;
                }

                if (bitindex > 8) {
                    index += 1;
                    bitindex = 1;
                }

                if (!hex.equals("ffffff")) {
                    buffer[index] = (byte) (buffer[index] | (0x01 << 8 - bitindex));
                }
                bitindex++;
            }
        }

        return buffer;
    }


    public static void adaptiveThreshold(Bitmap bitmap, double sub_thresh) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int x1, x2, y1, y2;
        int s = width / 8;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                x1 = i - s;
                x2 = i + s;
                y1 = j - s;
                y2 = j + s;

                if (x1 < 0) {
                    x1 = 0;
                }
                if (y1 < 0) {
                    y1 = 0;
                }
                if (x2 > width) {
                    x2 = width - 1;
                }
                if (y2 > height) {

                    y2 = height - 1;
                }
                int count = (x2 - x1) * (y2 - y1);

            }
        }

    }

}
