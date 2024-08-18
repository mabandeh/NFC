package com.fmsh.einkesl.utils;

/**
 * =author wuyajiang
 * =date 2021/4/15
 */
public class Constants {

    //TLV标签
    public static final byte TLV_SCREEN_INFO = (byte) (byte) 0xA0;             //屏幕信息
    public static final byte TLV_COLOR_INFO = (byte) 0xA1;              //颜色参数
    public static final byte TLV_IMAGE_COUNT = (byte) 0xB1;             //图片存储数量
    public static final byte TLV_USER_DATA = (byte) 0xB2;            //用户数据长度
    public static final byte TLV_POWER_MODE = (byte) 0xB3;            //供电模式
    public static final byte TLV_APPID = (byte) 0xC0;            //APPID
    public static final byte TLV_UUID = (byte) 0xC1;           //UUID
    public static final byte TLV_RFU = (byte) 0xD1;           //RFU


    /**
     * 临时存储的图片的名字
     */
    public static final String IMAGE_NAME = "/fmtemp.bmp";
    public static final String IMAGE_BLACK_WHITE_NAME = "/fmtempblackwhite.bmp";

    public static final int CUT_REQUEST_CODE = 100;

    /**
     * 16进制字符串正则表达式
     */
    public static final String HEX_REGULAR = "^[A-Fa-f0-9]+$";
}
