package com.fmsh.einkesl.tools.image;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fmsh.base.utils.Constant;
import com.fmsh.base.utils.FMUtil;
import com.fmsh.base.utils.LogUtil;
import com.fmsh.einkesl.App;
import com.fmsh.einkesl.R;
import com.fmsh.einkesl.activity.CropImageActivity;
import com.fmsh.einkesl.activity.MainActivity;
import com.fmsh.einkesl.activity.RefreshScreenActivity;
import com.fmsh.einkesl.bean.DeviceInfo;
import com.fmsh.einkesl.utils.Constants;
import com.fmsh.einkesl.utils.IUtils;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.entity.LocalMedia;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author wuyajiang
 * @date 2021/4/15
 */
public class ImageUtils {


    public static void loadImage(Intent data, DeviceInfo deviceInfo, AppCompatActivity activity) {
        // 图片、视频、音频选择结果回调
        List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
        LocalMedia localMedia = selectList.get(0);
        String fileName = localMedia.getFileName();
        String realPath = localMedia.getRealPath();
        if(realPath == null){
            realPath = localMedia.getPath();
        }
        boolean bmpFormat = BmpUtils.GetBmpFormat(realPath, deviceInfo);
        Bundle bundle = new Bundle();
        if (bmpFormat) {
            Intent intent = new Intent(activity, RefreshScreenActivity.class);
            bundle.putString("configBmpPath", realPath);
            intent.putExtras(bundle);
            activity.startActivity(intent);
        } else {
            //设置图片为符合的bmp图片
            IUtils.uCrop(activity,realPath);
//            Intent intent = new Intent(activity, CropImageActivity.class);
//            bundle.putString("bmpPath", realPath);
//            intent.putExtras(bundle);
//            activity.startActivity(intent);
        }
    }

    /**
     * 图片反色操作
     *
     * @param imageView
     */
    public static void antiColor(ImageView imageView) {
        try {
            Bitmap bitmap = ((BitmapDrawable) ((ImageView) imageView).getDrawable()).getBitmap();
            if (null == bitmap) {
                return;
            }
            bitmap = BmpUtils.convertGreyImg(bitmap);
            bitmap = BmpUtils.invertBitmap(bitmap);
            BmpUtils.saveBmp(bitmap,BmpUtils.getImagePath("/fmtemp1.bmp"));

            imageView.setImageBitmap(bitmap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void reversal(ImageView imageView) {
        try {
            Bitmap bitmap = ((BitmapDrawable) ((ImageView) imageView).getDrawable()).getBitmap();
            if (null == bitmap) {
                return;
            }
            Matrix matrix = new Matrix();
            /*翻转180度*/
            matrix.postRotate(180);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            BmpUtils.saveBmp(bitmap,BmpUtils.getImagePath("/fmtemp2.bmp"));

            imageView.setImageBitmap(bitmap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public static void horizontalMirror(ImageView imageView) {
        try {
            Bitmap bitmap = ((BitmapDrawable) ((ImageView) imageView).getDrawable()).getBitmap();
            if (null == bitmap) {
                return;
            }
            //bitmap = BmpUtils.convertGreyImg(bitmap );
            bitmap = BmpUtils.convertHorMirro(bitmap);
            BmpUtils.saveBmp(bitmap,BmpUtils.getImagePath("/fmtemp3.bmp"));
            imageView.setImageBitmap(bitmap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void verticallyMirror(ImageView imageView) {
        try {
            Bitmap bitmap = ((BitmapDrawable) ((ImageView) imageView).getDrawable()).getBitmap();
            if (null == bitmap) {
                return;
            }
            //bitmap = BmpUtils.convertGreyImg(bitmap );
            bitmap = BmpUtils.convertVerMirro(bitmap);
            BmpUtils.saveBmp(bitmap,BmpUtils.getImagePath("/fmtemp4.bmp"));
            imageView.setImageBitmap(bitmap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void grayscale(ImageView imageView) {
        try {
            Bitmap bitmap = ((BitmapDrawable) ((ImageView) imageView).getDrawable()).getBitmap();
            if (null == bitmap) {
                return;
            }
            bitmap = BmpUtils.convertGreyImg(bitmap);
            //bitmap = BmpUtils.DrawRectOnBitmap(bitmap );

            BmpUtils.saveBmp(bitmap,BmpUtils.getImagePath("/fmtemp5.bmp"));
            imageView.setImageBitmap(bitmap);
        } catch (Exception ex) {

            ex.printStackTrace();
        }

    }

    public static void binarization(ImageView imageView) {
        try {
            Bitmap bitmap = ((BitmapDrawable) ((ImageView) imageView).getDrawable()).getBitmap();
            if (null == bitmap) {
                return;
            }
            //bitmap = BmpUtils.sharpenImageAmeliorate(bitmap );
            bitmap = BmpUtils.zeroAndOne(bitmap);
            BmpUtils.saveBmp(bitmap,BmpUtils.getImagePath("/fmtemp6.bmp"));
            imageView.setImageBitmap(bitmap);
        } catch (Exception ex) {
            ex.printStackTrace();

        }
    }


    public static void blackAndWhite(ImageView imageView) {
        try {
            Bitmap bitmap = ((BitmapDrawable) ((ImageView) imageView).getDrawable()).getBitmap();
            if (null == bitmap) {
                return;
            }
            //bitmap = BmpUtils.sharpenImageAmeliorate(bitmap );
            bitmap = BmpUtils.convertToBlackWhite(bitmap);
            BmpUtils.saveBmp(bitmap,BmpUtils.getImagePath("/fmtemp7.bmp"));
            imageView.setImageBitmap(bitmap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static  boolean isConfig(String bmpFilePath){
        InputStream in = null;
        try {
            byte[] bmpdata1 = new byte[100];
            in = new FileInputStream(bmpFilePath);
            int tempbyte = in.read(bmpdata1, 0, bmpdata1.length);
            in.close();
            // 格式错
            if (bmpdata1[0] != (byte) 0x42 || bmpdata1[1] != (byte) 0x4d
                    || bmpdata1[0xe] != (byte) 0x28
                    || bmpdata1[0xf] != (byte) 0x0
                    || bmpdata1[0x10] != (byte) 0x0
                    || bmpdata1[0x11] != (byte) 0x0

            ){
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();

        }
        return false;
    }

    /**
     * 读 单色Bmp图片数据
     *
     * @return
     */
    public static byte[] ReadBmp8File(String bmpPath) {
        String StrFileName = bmpPath;
        int bmpwidth = App.getDeviceInfo().getWidth();
        int bmpHeight = App.getDeviceInfo().getHeight();
        final int locoffset = (int) 0x3E;
        int widthbytes = 0;
        if (widthbytes % 8 != 0) {
            widthbytes = (bmpwidth / 8 + 1);
        } else {
            widthbytes = bmpwidth / 8;
        }
        //widthbytes必须为4的倍数
        if (widthbytes % 4 != 0) {
            widthbytes = (widthbytes / 4 + 1) * 4;
        }
        int p = 8- bmpHeight % 8;
        byte[] bmpdata = new byte[widthbytes * bmpHeight + locoffset + widthbytes*p];
        InputStream in = null;
        try {
            byte[] bmpdata1 = new byte[bmpdata.length];
            in = new FileInputStream(StrFileName);
            int tempbyte = in.read(bmpdata1, 0, bmpdata1.length);
            in.close();
            // 格式错
            LogUtil.d(FMUtil.byteToHex(bmpdata1));
            int i = 0;
            for (int j = bmpHeight - 1; j >= 0; j--) {
                System.arraycopy(bmpdata1, locoffset + j * widthbytes, bmpdata,
                        locoffset + i * widthbytes, widthbytes);
                i += 1;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return bmpdata;
    }


    /**
     * 读24位真彩色图片文件
     *
     * @return
     */
    public static byte[] ReadBmp24File(String bmpPath,boolean isLvl) {
        String StrFileName = bmpPath;
        BitmapFactory.Options options = new BitmapFactory.Options();

        //默认值为false，如果设置成true，那么在解码的时候就不会返回bitmap，即bitmap = null。
        options.inJustDecodeBounds = false;
        //可以复用之前用过的bitmap
        options.inBitmap = null;
        //是该bitmap缓存是否可变，如果设置为true，将可被inBitmap复用
        options.inMutable = true;
        byte[] bytes = BMPConverterUtil.floydSteinberg(BitmapFactory.decodeFile(bmpPath,options), 1,isLvl);
        int width = App.getDeviceInfo().getWidth();
        int height = App.getDeviceInfo().getHeight();
        int p = width % 4;
        int other = 0;
        if (p != 0) {
            other = p * height;
        }
        byte[] bmpdata1 = new byte[width * height * 3 + 0x36 + other];
        System.arraycopy(bytes,0,bmpdata1,0x36,bytes.length);
        byte[] bmpdata = new byte[width * height * 3 + 0x36];

        InputStream in = null;
        try {
//            in = new FileInputStream(StrFileName);
//            int tempbyte = in.read(bmpdata1, 0, bmpdata1.length);
//            in.close();
//            // 格式错
//            if (bmpdata1[0] != (byte) 0x42 || bmpdata1[1] != (byte) 0x4d
//                    || bmpdata1[0xe] != (byte) 0x28
//                    || bmpdata1[0xf] != (byte) 0x0
//                    || bmpdata1[0x10] != (byte) 0x0
//                    || bmpdata1[0x11] != (byte) 0x0
//
//            ) {
//                return null;
//            }

            int i = 0;
            for (int j = height - 1; j >= 0; j--) {

                System.arraycopy(bmpdata1, 0x36 + j * (width * 3) + j * p, bmpdata,
                        0x36 + i * (width * 3), (width * 3));
                i += 1;

            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return bmpdata;
    }

    public static byte[] verticalScanning(byte[] src){
        int width = App.getDeviceInfo().getWidth();
        int height = App.getDeviceInfo().getHeight();
        int remainder = 0;
        if(height % 8 != 0){
            remainder = 8 - height % 8;
        }
        int destRealWidth = (int) Math.ceil(width / 8.0);
        int destWidth = 0;
        int percent = destRealWidth % 4;
        if (percent != 0) {
            destWidth += destRealWidth - percent + 4;
        } else {
            destWidth = destRealWidth;
        }

        byte[] data = new byte[src.length];
        int p = 0;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < destWidth; j++) {
                data[p++] = src[i + j * width];
                if (j == height-1) {
                    for (int k = 0; k < remainder; k++) {
                        data[p + k] = 0;
                    }
                    p += remainder;
                }
            }
        }
        byte[] result = new byte[destRealWidth *height];
        System.arraycopy(data,0,result,0,result.length);

        return result;
    }

    /**
     * 从左向右纵向
     *
     * @param result      转换后的结果
     * @param bmpdata     源图片数据
     * @param realdatacol 实际数据宽度的字节数
     * @param vbitsRow    纵向扫描的行数   是高度/8的结果，  不是8的倍数要凑的
     * @param rowcolnum   realdatacol不满4 字节倍数时，补充字节后的长度
     */
    public static void VerticalScanning(byte[] result, byte[] bmpdata, int realdatacol, int vbitsRow, int rowcolnum) {
        int row = 0;
        final int locoffset = (int) 0x3E;
        // 总共列，实际的
        for (int i = 0; i < realdatacol; i++) {
            for (int j = 0; j < vbitsRow; j++) {
                result[row * vbitsRow + j] = (byte) ((short) (bmpdata[locoffset + j
                        * (8 * rowcolnum) + i] & 0x80)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum + i] & 0x80) >>> 1)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 2 + i] & 0x80) >>> 2)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 3 + i] & 0x80) >>> 3)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 4 + i] & 0x80) >>> 4)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 5 + i] & 0x80) >>> 5)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 6 + i] & 0x80) >>> 6) + (short) ((bmpdata[locoffset
                        + j * (8 * rowcolnum) + rowcolnum * 7 + i] & 0x80) >>> 7));
            }

            row += 1;
            for (int j = 0; j < vbitsRow; j++) {
                result[row * vbitsRow + j] = (byte) ((short) ((bmpdata[locoffset + j
                        * (8 * rowcolnum) + i] & 0x40) << 1)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum + i] & 0x40))
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 2 + i] & 0x40) >>> 1)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 3 + i] & 0x40) >>> 2)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 4 + i] & 0x40) >>> 3)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 5 + i] & 0x40) >>> 4)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 6 + i] & 0x40) >>> 5) + (short) ((bmpdata[locoffset
                        + j * (8 * rowcolnum) + rowcolnum * 7 + i] & 0x40) >>> 6));
            }

            row += 1;

            for (int j = 0; j < vbitsRow; j++) {
                result[row * vbitsRow + j] = (byte) ((short) ((bmpdata[locoffset + j
                        * (8 * rowcolnum) + i] & 0x20) << 2)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum + i] & 0x20) << 1)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 2 + i] & 0x20))
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 3 + i] & 0x20) >>> 1)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 4 + i] & 0x20) >>> 2)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 5 + i] & 0x20) >>> 3)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 6 + i] & 0x20) >>> 4) + (short) ((bmpdata[locoffset
                        + j * (8 * rowcolnum) + rowcolnum * 7 + i] & 0x20) >>> 5));

            }

            row += 1;

            for (int j = 0; j < vbitsRow; j++) {
                result[row * vbitsRow + j] = (byte) ((short) ((bmpdata[locoffset + j
                        * (8 * rowcolnum) + i] & 0x10) << 3)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum + i] & 0x10) << 2)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 2 + i] & 0x10) << 1)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 3 + i] & 0x10))
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 4 + i] & 0x10) >>> 1)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 5 + i] & 0x10) >>> 2)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 6 + i] & 0x10) >>> 3) + (short) ((bmpdata[locoffset
                        + j * (8 * rowcolnum) + rowcolnum * 7 + i] & 0x10) >>> 4));

            }

            row += 1;

            for (int j = 0; j < vbitsRow; j++) {
                result[row * vbitsRow + j] = (byte) ((short) ((bmpdata[locoffset + j
                        * (8 * rowcolnum) + i] & 0x08) << 4)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum + i] & 0x08) << 3)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 2 + i] & 0x08) << 2)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 3 + i] & 0x08) << 1)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 4 + i] & 0x08))
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 5 + i] & 0x08) >>> 1)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 6 + i] & 0x08) >>> 2) + (short) ((bmpdata[locoffset
                        + j * (8 * rowcolnum) + rowcolnum * 7 + i] & 0x08) >>> 3));


            }


            row += 1;

            for (int j = 0; j < vbitsRow; j++) {
                result[row * vbitsRow + j] = (byte) ((short) ((bmpdata[locoffset + j
                        * (8 * rowcolnum) + i] & 0x04) << 5)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum + i] & 0x04) << 4)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 2 + i] & 0x04) << 3)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 3 + i] & 0x04) << 2)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 4 + i] & 0x04) << 1)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 5 + i] & 0x04))
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 6 + i] & 0x04) >>> 1) + (short) ((bmpdata[locoffset
                        + j * (8 * rowcolnum) + rowcolnum * 7 + i] & 0x04) >>> 2));


            }

            row += 1;

            for (int j = 0; j < vbitsRow; j++) {
                result[row * vbitsRow + j] = (byte) ((short) ((bmpdata[locoffset + j
                        * (8 * rowcolnum) + i] & 0x02) << 6)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum + i] & 0x02) << 5)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 2 + i] & 0x02) << 4)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 3 + i] & 0x02) << 3)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 4 + i] & 0x02) << 2)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 5 + i] & 0x02) << 1)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 6 + i] & 0x02)) + (short) ((bmpdata[locoffset
                        + j * (8 * rowcolnum) + rowcolnum * 7 + i] & 0x02) >>> 1));

            }
            row += 1;
            for (int j = 0; j < vbitsRow; j++) {
                result[row * vbitsRow + j] = (byte) ((short) ((bmpdata[locoffset + j
                        * (8 * rowcolnum) + i] & 0x01) << 7)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum + i] & 0x01) << 6)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 2 + i] & 0x01) << 5)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 3 + i] & 0x01) << 4)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 4 + i] & 0x01) << 3)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 5 + i] & 0x01) << 2)
                        + (short) ((bmpdata[locoffset + j * (8 * rowcolnum)
                        + rowcolnum * 6 + i] & 0x01) << 1) + (short) ((bmpdata[locoffset
                        + j * (8 * rowcolnum) + rowcolnum * 7 + i] & 0x01)));
            }
            row += 1;
        }
    }



    /**
     * 垂直扫描 从左到右
     */
    public static byte[] Color24VerticalScanning(byte[] src) {
        int width = App.getDeviceInfo().getWidth();
        int height = App.getDeviceInfo().getHeight();
        int remainder = 0;
        if(height % 8 != 0){
            remainder = 8 - height % 8;
        }

        byte[] data = new byte[src.length + remainder * width];
        int p = 0;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                data[p++] = src[i + j * width];
                if (j == height-1) {
                    for (int k = 0; k < remainder; k++) {
                        data[p + k] = 0;
                    }
                    p += remainder;
                }

            }
        }
        byte[] temp = new byte[8];
        int n = 0;
        int a = 0;
        int b = 0;
        int destHeight = (height+remainder) / 8;
        byte[] result = new byte[destHeight * width];
        for (int i = 0; i < data.length; i++) {

            temp[n++] = data[i];
            if (n == 8) {
                result[a + b] = (byte) (temp[7] | (temp[6] << 1) |
                        (temp[5] << 2) |
                        (temp[4] << 3) |
                        (temp[3] << 4) |
                        (temp[2] << 5) |
                        (temp[1] << 6) |
                        (temp[0] << 7));
                n = 0;
                a += 1;
                if (a == destHeight) {
                    a = 0;
                    b += destHeight;

                }

            }
        }
        return result;
    }



    /**
     * 横向的扫描转换
     *
     * @param src
     * @return
     */
    public static byte[] horizontalScanning(byte[] src) {
        int width = App.getDeviceInfo().getWidth();
        int height = App.getDeviceInfo().getHeight();
        int remainder = 8-width % 8;
        int destWidth = (width+remainder)/8;
        byte[] result = new byte[ destWidth * height];
        byte[] temp = new byte[8];
        int n = 0;
        int a = 0;
        int b = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                temp[n] =src[(i*j)+j];
                n++;
                if((j+1 == width) && remainder != 0 ){
                    for (int k = 0; k < remainder; k++) {
                        temp[n] = 0;
                        n++;
                    }
                }
                if (n == 8) {
                    result[a + b] = (byte) (temp[7] | (temp[6] << 1) |
                            (temp[5] << 2) |
                            (temp[4] << 3) |
                            (temp[3] << 4) |
                            (temp[2] << 5) |
                            (temp[1] << 6) |
                            (temp[0] << 7));
                    n = 0;
                    a += 1;
                    if (a == destWidth) {
                        a = 0;
                        b += destWidth;

                    }

                }

            }
        }
        return result;
    }

    public static byte[] HorizontalMirrorScanning(byte[] bmpdata, int realdatacol, int bmpheight, int bmpwidth) {
        final int locoffset = (int) 0x3E;
        byte[] result = new byte[locoffset + bmpheight * realdatacol + 0xFA];

        Log.d("转换算法", "水平扫描的");
        Log.d("原来的图片宽度", String.format("%d", realdatacol));
        Log.d("原来的图片高度", String.format("%d", bmpheight));
        Log.d("实际图片真实有效数据宽度", String.format("%d", bmpwidth));

        for (int i = 0; i < bmpheight; i++) {
            //System.arraycopy(bmpdata, i * bmpwidth + locoffset, result, i * realdatacol, realdatacol);

            byte[] rev = reverseArray(bmpdata, i * bmpwidth + locoffset, realdatacol);
            System.arraycopy(rev, 0, result, i * realdatacol, realdatacol);

        }

        return result;
    }


    /**
     * 数组反向
     *
     * @param originArray
     * @param offset
     * @param length
     * @return
     */
    public static byte[] reverseArray(byte[] originArray, int offset, int length) {
        byte[] reverseArray = new byte[length];
        for (int i = 0; i < length; i++) {

            // reverseArray[i] = (byte)( (originArray[offset + (length - i - 1)]&0xff) );
            reverseArray[i] = (byte) (byte_change(originArray[offset + (length - i - 1)]));
        }
        return reverseArray;
    }

    /**
     * 字节按位反序
     *
     * @param data
     * @return
     */
    public static byte byte_change(byte data) {
        byte i = 0;
        byte temp = 0;

        for (i = 0; i < 8; i++) {
            temp = (byte) (temp << 1);
            temp |= (data >>> i) & 0x01;
        }

        return temp;
    }


    /**
     * 获取24位真彩色图片数据
     *
     * @param srcData    图片源数据
     * @param blackColor 黑白
     * @param readColor  红色
     */
    public static void getColorDataBmp24(byte[] srcData, byte[] blackColor, byte[] readColor) {
        //24位真彩色的偏移
        final int locOffset = (int) 0x36;
        int srcWidth = App.getDeviceInfo().getWidth();
        int srcHeight = App.getDeviceInfo().getHeight();
        int dataSize = srcWidth * srcHeight * 3;
        int a = 0;
        for (int i = 0; i < dataSize; i += 3) {
            // 取黑白
            if (srcData[locOffset + i] == (byte) 0 && srcData[locOffset + i + 1] == (byte) 0 && srcData[locOffset + i + 2] == (byte) 0) {
                blackColor[a] = 0;
            } else if (srcData[locOffset + i] == (byte) 0xff && srcData[locOffset + i + 1] == (byte) 0xff && srcData[locOffset + i + 2] == (byte) 0xff) {
                blackColor[a] = 1;
            }
//            else {
//                //灰度值 经验公式，不要问为啥，就这个
//                int gray = (int) ((float) (srcData[locOffset + i] & 0xff) * 0.30 + (float) (srcData[locOffset + i + 1] & 0xff) * 0.59 + (float) (srcData[locOffset + i + 2] & 0xff) * 0.11);
//                //180比较 好的数字，没有意义，就是偏向白色
//                if (gray > 180) {
//                    blackColor[a] = 1;
//                } else {
//
//                    blackColor[a] = 0;
//                }
//
//            }

            //取红或黄
            if ((srcData[locOffset + i] == 0 && srcData[locOffset + i + 1] == 0 && srcData[locOffset + i + 2] == (byte) 0xff)
                    || (srcData[locOffset + i] == 0 && srcData[locOffset + i + 1] == (byte) 0xff && srcData[locOffset + i + 2] == (byte) 0xff)
            ) {

                readColor[a] = 1;
            } else {
                readColor[a] = 0;
            }
            a++;
        }
    }





}
