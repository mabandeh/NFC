package com.fmsh.einkesl.bean;

import android.graphics.Bitmap;

import com.fmsh.base.utils.UIUtils;
import com.fmsh.einkesl.R;

/**
 * @author wuyajiang
 * @date 2021/4/15
 */
public class DeviceInfo {
    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getScreen() {
        return screen;
    }

    public void setScreen(String screen) {
        this.screen = screen;
    }

    public String getColorType() {
        return colorType;
    }

    public void setColorType(String colorType) {
        this.colorType = colorType;
    }

    public String getScanType() {
        return scanType ;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public int getPictureCapacity() {
        return pictureCapacity;
    }

    public void setPictureCapacity(int pictureCapacity) {
        this.pictureCapacity = pictureCapacity;
    }

    public boolean isBattery() {
        return isBattery;
    }

    public void setBattery(boolean battery) {
        isBattery = battery;
    }

    public String getAppID() {
        return this.AppID;
    }

    public void setAppID(String appID) {
       this.AppID = appID;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public boolean isCompress() {
        return isCompress;
    }

    public void setCompress(boolean compress) {
        isCompress = compress;
    }

    /**
     * 厂商
     */
    private String manufacturer;
    /**
     * 支持颜色
     */
    private String color = UIUtils.getString(R.string.text_color_30);
    private String EN_Color = UIUtils.getString(R.string.text_color_30);

    public String getEN_Color() {
        return EN_Color;
    }

    public void setEN_Color(String EN_Color) {
        this.EN_Color = EN_Color;
    }

    /**
     * 屏幕分辨率
     */
    private String screen;

    /**
     * 图片宽度
     */
    private int width;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    /**
     * 是否支持PIN码
     */
    private boolean isPin;

    public void setPin(boolean pin) {
        isPin = pin;
    }
    public boolean getPin(){
        return this.isPin;
    }

    /**
     * 图片高度
     */
    private int height;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /**
     * 刷屏所需图片数量
     */
    private int size;

    public int getBlack() {
        return black;
    }

    public void setBlack(int black) {
        this.black = black;
    }

    public int getWhite() {
        return white;
    }

    public void setWhite(int white) {
        this.white = white;
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public int getYellow() {
        return yellow;
    }

    public void setYellow(int yellow) {
        this.yellow = yellow;
    }

    ///
    public int getOrange() {
        return orange;
    }

    public void setOrangre(int orange) {
        this.orange = orange;
    }
    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        this.green = green;
    }
    public int getCyan() {
        return cyan;
    }

    public void setCyan(int cyan) {
        this.cyan = cyan;
    }
    public int getBlue() {
        return blue;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }
    public int getViolet() {
        return violet;
    }

    public void setViolet(int violet) {
        this.violet = violet;
    }
    ///

    public int getColorCount() {
        return colorCount;
    }

    public void setColorCount(int colorCount) {
        this.colorCount = colorCount;
    }

    /**
     * 支持颜色数量 是单色的还是三色的
     */
    private int colorCount = 3;


    /**
     * 颜色参数
     */
    private String colorType = "";

    private int deviceType = 0; //0黑白 1 黑白红价签  2 黑白黄价签

    private int black;
    private int white;
    private int red;
    private int yellow;
    private int orange;
    private int green;
    private int cyan;
    private int blue;
    private int violet;
    public int getCosVersion() {
        return cosVersion;
    }

    public void setCosVersion(int cosVersion) {
        this.cosVersion = cosVersion;
    }


    private String color_desc;
    public void setColorDesc(String cds) {
        this.color_desc = cds;
    }
    public String getColorDesc() {
        return this.color_desc;
    }
    private int cosVersion;
    /**
     * 扫描类型
     */
    private String scanType = "";

    /**
     * 刷屏扫描
     */
    private int refreshScan;

    public int getRefreshScan() {
        return refreshScan;
    }

    public void setRefreshScan(int refreshScan) {
        this.refreshScan = refreshScan;
    }

    /**
     * 图片容量
     */
    private int pictureCapacity;

    public int getUserData() {
        return userData;
    }

    public void setUserData(int userData) {
        this.userData = userData;
    }

    /**
     * 用户数据容量
     */
    private int userData;
    /**
     * 有无电池
     */
    private boolean isBattery;
    private String AppID;
    private String UID;
    /**
     * 是否支持压缩算法
     */
    private boolean isCompress;

    public String getPinCode() {
        return pinCode;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    private String pinCode = "1122334455";

    /**
     * 需要加载的图片
     */
    private Bitmap mBitmap;


    public byte[] getCompressData() {
        return compressData;
    }

    public void setCompressData(byte[] compressData) {
        this.compressData = compressData;
    }

    /**
     * 图片压缩数据
     */
    private byte[] compressData;



}
