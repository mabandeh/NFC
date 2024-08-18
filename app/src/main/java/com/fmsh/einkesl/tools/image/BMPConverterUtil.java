package com.fmsh.einkesl.tools.image;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.fmsh.base.utils.LogUtil;
import com.fmsh.einkesl.App;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author wuyajiang
 * @date 2021/6/2
 */
public class BMPConverterUtil {

    /**
     * 黑白屏
     */
    public final static int PALETTE_BW = 0;

    /**
     * 黑白红屏
     */
    public final static int PALETTE_BWR = 1;

    /**
     * 黑白黄屏
     */
    public final static int PALETTE_BWY = 2;

    /**
     * 取二值化屏幕类型
     *
     * @param deviceType 屏幕类型
     * @return RGBTriple[]
     * @see RGBTriple
     */
    public static RGBTriple[] getPalette(int deviceType) {
        final RGBTriple[] palette;

        if (deviceType == 1) {
            //黑白红价签
            palette = new RGBTriple[]{new RGBTriple(0, 0, 0), new RGBTriple(255, 255, 255), new RGBTriple(255, 0, 0)};
        } else if (deviceType == 2) {
            //黑白黄价签
            palette = new RGBTriple[]{new RGBTriple(0, 0, 0), new RGBTriple(255, 255, 255), new RGBTriple(255, 255, 0)};
        } else {
            //黑白价签
            palette = new RGBTriple[]{new RGBTriple(0, 0, 0), new RGBTriple(255, 255, 255)};
        }
        return palette;
    }

    /**
     * 核心算法，二值化处理
     *
     * @param image   图片
     * @param palette 屏幕类型
     * @return 二值化数组， 0表示黑，1表示白，2表示红或黄（假定红和黄色不同时存在）
     */
    public static byte[][] floydSteinbergDither(RGBTriple[][] image, RGBTriple[] palette, boolean isLvl) {
        byte[][] result = new byte[image.length][image[0].length];


        for (int y = 0; y < image.length; y++) {
            for (int x = 0; x < image[y].length; x++) {
                RGBTriple currentPixel = image[y][x];
                byte index = findNearestColor(currentPixel, palette);
                result[y][x] = index;

                if (!isLvl) {
                    for (int i = 0; i < 3; i++) {
                        int error = (currentPixel.channels[i] & 0xff) - (palette[index].channels[i] & 0xff);
                        if (x + 1 < image[0].length) {
                            image[y][x + 1].channels[i] = plus_truncate_uchar(image[y][x + 1].channels[i], (error * 7) >> 4);
                        }
                        if (y + 1 < image.length) {
                            if (x - 1 > 0) {
                                image[y + 1][x - 1].channels[i] = plus_truncate_uchar(image[y + 1][x - 1].channels[i], (error * 3) >> 4);
                            }
                            image[y + 1][x].channels[i] = plus_truncate_uchar(image[y + 1][x].channels[i], (error * 5) >> 4);
                            if (x + 1 < image[0].length) {
                                image[y + 1][x + 1].channels[i] = plus_truncate_uchar(image[y + 1][x + 1].channels[i], (error) >> 4);
                            }
                        }

                    }
                }


            }
        }
        return result;
    }

    /**
     * 转换图片
     *
     * @param bufferedImage 原图
     * @param deviceType    屏幕类型
     */
    public static byte[] floydSteinberg(Bitmap bufferedImage, int deviceType,boolean isLvl) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        RGBTriple[][] image = new RGBTriple[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int pixel = bufferedImage.getPixel(i, j);
                int B = Color.blue(pixel);
                int G = Color.green(pixel);
                int R = Color.red(pixel);
                int rx = (pixel & 0xff0000) >> 16;
                int gx = (pixel & 0xff00) >> 8;
                int bx = (pixel & 0xff);
                RGBTriple rgbTriple = new RGBTriple(R, G, B);
                image[i][j] = rgbTriple;
            }
        }
        Bitmap.Config config = bufferedImage.getConfig();
        LogUtil.d(config.toString());
        int p = width % 4;
        int realWidth = width * 3 + p;
        byte[][] to = floydSteinbergDither(image, getPalette(deviceType),isLvl);
        byte[] result = new byte[width * height * 3 + p * height];

        for (int i = 0, nRealCol = height - 1; i < height; i++, --nRealCol) {
            for (int j = 0, wByteIdex = 0; j < width; j++, wByteIdex += 3) {
                LogUtil.d(to[j][i]);

                if (to[j][i] == 0) {
                    // 处理黑色
                    //                    bufferedImage.setPixel(i, j, 0);
                    result[nRealCol * realWidth + wByteIdex] = 0;
                    result[nRealCol * realWidth + wByteIdex + 1] = 0;
                    result[nRealCol * realWidth + wByteIdex + 2] = 0;
                } else if (to[j][i] == 1) {
                    // 处理白色
                    //                    bufferedImage.setPixel(i, j, (255 << 16) + (255 << 8) + 255);
                    result[nRealCol * realWidth + wByteIdex] = (byte) 0xff;
                    result[nRealCol * realWidth + wByteIdex + 1] = (byte) 0xff;
                    result[nRealCol * realWidth + wByteIdex + 2] = (byte) 0xff;
                } else if (to[j][i] == 2 && deviceType == 1) {
                    // 处理红色
                    //                    bufferedImage.setPixel(i, j, (255 << 16));
                    result[nRealCol * realWidth + wByteIdex] = (byte) 0;
                    result[nRealCol * realWidth + wByteIdex + 1] = (byte) 0;
                    result[nRealCol * realWidth + wByteIdex + 2] = (byte) 0xff;
                } else if (to[j][i] == 2 && deviceType == 2) {
                    // 处理黄色
                    //                    bufferedImage.setPixel(i, j, (255 << 16) + (255 << 8));
                    result[nRealCol * realWidth + wByteIdex] = (byte) 0xff;
                    result[nRealCol * realWidth + wByteIdex + 1] = (byte) 0xff;
                    result[nRealCol * realWidth + wByteIdex + 2] = 0;
                }


            }
        }
        return result;
    }

    /**
     * @param a
     * @param b
     * @return
     */
    private static byte plus_truncate_uchar(byte a, int b) {
        if ((a & 0xff) + b < 0) {
            return 0;
        } else if ((a & 0xff) + b > 255) {
            return (byte) 255;
        } else {
            return (byte) (a + b);
        }
    }

    /**
     * @param color
     * @param palette
     * @return
     */
    private static byte findNearestColor(RGBTriple color, RGBTriple[] palette) {
        int minDistanceSquared = 255 * 255 + 255 * 255 + 255 * 255 + 1;
        byte bestIndex = 0;
        for (byte i = 0; i < palette.length; i++) {
            int Rdiff = (color.channels[0] & 0xff) - (palette[i].channels[0] & 0xff);
            int Gdiff = (color.channels[1] & 0xff) - (palette[i].channels[1] & 0xff);
            int Bdiff = (color.channels[2] & 0xff) - (palette[i].channels[2] & 0xff);
            int distanceSquared = Rdiff * Rdiff + Gdiff * Gdiff + Bdiff * Bdiff;
            if (distanceSquared < minDistanceSquared) {
                minDistanceSquared = distanceSquared;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /**
     * 二值化颜色类
     *
     * @author duxuefu
     * @date 2019-09-16
     */
    public static class RGBTriple {
        public final byte[] channels;

        public RGBTriple(int R, int G, int B) {
            channels = new byte[]{(byte) R, (byte) G, (byte) B};
        }
    }


}
