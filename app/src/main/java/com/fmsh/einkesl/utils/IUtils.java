package com.fmsh.einkesl.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;

import com.fmsh.base.utils.FMUtil;
import com.fmsh.base.utils.HintDialog;
import com.fmsh.base.utils.LogUtil;
import com.fmsh.base.utils.UIUtils;
import com.fmsh.einkesl.App;
import com.fmsh.einkesl.R;
import com.fmsh.einkesl.bean.DeviceInfo;
import com.fmsh.einkesl.tools.image.BmpUtils;
import com.fmsh.einkesl.tools.image.GlideCacheEngine;
import com.fmsh.einkesl.tools.image.GlideEngine;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.tools.SdkVersionUtils;
import com.yalantis.ucrop.UCrop;

import java.io.File;

/**
 * @author wuyajiang
 * @date 2021/4/15
 */
public class IUtils {

    public static void showDialogErrorInfo(Context context) {
        HintDialog.faileDialog(context, UIUtils.getString(context, R.string.text_error));
    }

    /**
     * 设备信息解析
     *
     * @param mContext
     * @param info
     * @return
     */
    public static DeviceInfo loadDeviceInfo(Context mContext, String info) {
      String s =
              "A007" +//0
              "F003" +//1
              "3001" +//2
              "9001" +//3
              "2CA1" +//4
              "0700" +//5
              "2300" +//6
              "3048" +//7
              "FFFF" +//8
              "B101" +//9
              "05B2" +//10
              "0114" +//11
              "B301" +//12
              "00C0" +//13
              "0420" +//14
              "1EA0" +//15
              "01C1" +//16
              "0460" +//17
              "EBFB" +//18
              "7ED1" +//19
              "0701" +//20
              "1000" +//21
              "0000" +//22
              "0000" +//23
              "9000" +//24
              "5330" +//25
              "5648" +//26
              "5454" +//27
              "4130" +//28
              "4D6A" +//29
              "4177" +//30
              "4D56" +
              "9000";


      String s1 = "A007F0072001E001A0A10700120030FFFFFFB10103B20114B30100C004201EA001C10460EBFB7ED10701100000000000900053305648545441304D6A41774D569000";
      String s2 = "A007F003300190012CA1070023003048FFFFB10105B20114B30100C004201EA001C10460EBFB7ED10701100000000000900053305648545441304D6A41774D569000";
        boolean color_ex = false;
        DeviceInfo deviceInfo = new DeviceInfo();
        try {
            byte[] toByte = FMUtil.hexToByte(info);
            int tagLength = 0;
            if (toByte[0] == Constants.TLV_SCREEN_INFO) {
                String manufacturer = manufacturer = UIUtils.getString(mContext, R.string.text_other);
                ;
                switch (toByte[2]) {
                    case 0x00:
                        manufacturer = UIUtils.getString(mContext, R.string.text_jiaxian);
                        break;
                    case 0x10:
                        manufacturer = UIUtils.getString(mContext, R.string.text_yuantai);
                        break;
                    case 0x20:
                        manufacturer = UIUtils.getString(mContext, R.string.text_aoyi);
                        break;
                    case 0x30:
                        manufacturer = UIUtils.getString(mContext, R.string.text_weifeng);
                        break;
                    case 0x40:
                        manufacturer = UIUtils.getString(mContext, R.string.text_pdi);
                        break;
                    case 0x60:
                        manufacturer = UIUtils.getString(mContext, R.string.text_jdf);
                        break;
                    case 0x70:
                        manufacturer = UIUtils.getString(mContext, R.string.text_dke);
                        break;
                    case (byte) 0x80:
                        manufacturer = UIUtils.getString(mContext, R.string.text_weixinnuo);
                        break;
                    case (byte) 0xF0:
                        manufacturer = UIUtils.getString(mContext, R.string.text_other);
                        break;
                    default:
                        break;
                }
                deviceInfo.setManufacturer(String.format("%s(%02x)", manufacturer, toByte[2]).toUpperCase());
                String color = "";
                String en_color = "";
                switch (toByte[4]) {
                    case 0x20:
                        color = UIUtils.getString(mContext, R.string.text_color_20_cn);
                        en_color = UIUtils.getString(mContext, R.string.text_color_20);
                        break;
                    case 0x30:
                        color = UIUtils.getString(mContext, R.string.text_color_30_cn);
                        en_color = UIUtils.getString(mContext, R.string.text_color_30);
                        break;
                    case 0x31:
                        color = UIUtils.getString(mContext, R.string.text_color_31_cn);
                        en_color = UIUtils.getString(mContext, R.string.text_color_31);
                        break;
                    default:
                        color = String.format("%d 色", toByte[4]>>4);
                        en_color = String.format("%d Color", toByte[4]>>4);
                        break;
                }
//                deviceInfo.setColor(String.format("%s(%02x)", color, toByte[4]));
                deviceInfo.setColor(String.format("%s", color));
                deviceInfo.setEN_Color(String.format("%s", en_color));


                int x = Integer.parseInt(info.substring(10, 14), 16);
                int y = Integer.parseInt(info.substring(14, 18), 16);
                deviceInfo.setScreen(y + "x" + x);
                deviceInfo.setWidth(y);
                deviceInfo.setHeight(x);
            }
            tagLength = tagLength+ toByte[1+ tagLength] +2;
            if (toByte[tagLength] == Constants.TLV_COLOR_INFO ) {
                String screenScan = "";
                switch (toByte[tagLength+2]) {
                    case 0x00:
                        screenScan = UIUtils.getString(mContext, R.string.text_screen_scan_vertical);
                        break;
                    case 0x01:
                        screenScan = UIUtils.getString(mContext, R.string.text_screen_scan_level);
                        break;
                    default:
                        break;
                }
                deviceInfo.setRefreshScan(toByte[tagLength+2]);
                deviceInfo.setScanType(String.format("(%s)", screenScan));
                // 23 00 30 48 FFFF
                if(0xE0 == toByte[tagLength+3])
                {//扩展参数
                    color_ex = true;
                }
                else
                {
                    int size = (toByte[tagLength+3] >> 4) & 0xff;
                    deviceInfo.setSize(size);
                    int colorCount = toByte[tagLength+3] & 0x0F;
//                int colorCount = 3;
                    deviceInfo.setColorCount(colorCount);
                    StringBuilder colorType = new StringBuilder();
                    for (int i = 0; i < colorCount; i++) {
                        String type = "";
                        byte value = (byte) ((toByte[tagLength+4 + i] >> 5) & 0xff);
                        switch (value) {
                            case 0x00:
                                int blackColorValue = (toByte[tagLength+4 + i] >> (3 - colorCount + 3)) & (colorCount == 2 ? 1 : 3);
                                deviceInfo.setBlack(blackColorValue );
                                type = String.format("%s(%s)", UIUtils.getString(mContext, R.string.text_color_black), blackColorValue);
                                break;
                            case 0x01:
                                int whiteColorValue = (toByte[tagLength+4 + i] >> (3 - colorCount + 3)) & (colorCount == 2 ? 1 : 3);
                                deviceInfo.setWhite(whiteColorValue );
                                type = String.format("%s(%s)", UIUtils.getString(mContext, R.string.text_color_white), whiteColorValue);
                                break;
                            case 0x02:
                                int redColorValue = (toByte[tagLength+4 + i] >> (3 - colorCount + 3)) & (colorCount == 2 ? 1 : 3);
                                deviceInfo.setRed(redColorValue );
                                deviceInfo.setDeviceType(1);
                                type = String.format("%s(%s)", UIUtils.getString(mContext, R.string.text_color_red), redColorValue);
                                break;
                            case 0x03:
                                int yellowColorValue = (toByte[tagLength+4 + i] >> (3 - colorCount + 3)) & (colorCount == 2 ? 1 : 3);
                                deviceInfo.setYellow(yellowColorValue );
                                deviceInfo.setDeviceType(2);
                                type = String.format("%s(%s)", UIUtils.getString(mContext, R.string.text_color_yellow), yellowColorValue);
                                break;
                            default:
                                break;
                        }
                        colorType.append(type);
                    }
                    deviceInfo.setColorType(colorType.toString());
                }
            }

            tagLength = tagLength+toByte[tagLength+1]+2;
            if (toByte[tagLength] == Constants.TLV_IMAGE_COUNT) {
                deviceInfo.setPictureCapacity(toByte[tagLength+2]);
            }
            tagLength = tagLength+toByte[tagLength+1]+2;
            if (toByte[tagLength] == Constants.TLV_USER_DATA ) {
                deviceInfo.setUserData(toByte[tagLength+2]);
            }
            tagLength = tagLength+toByte[tagLength+1]+2;
            if (toByte[tagLength] == Constants.TLV_POWER_MODE ) {
                if (toByte[tagLength+2] == 0x00) {
                    deviceInfo.setBattery(false);
                } else {
                    deviceInfo.setBattery(true);
                }
            }
            tagLength = tagLength+toByte[tagLength+1]+2;
            if (toByte[tagLength] == Constants.TLV_APPID ) {
                byte[] appId = new byte[4];
                System.arraycopy(toByte, tagLength+2, appId, 0, 4);
                deviceInfo.setAppID(FMUtil.byteToHex(appId));
            }
            tagLength = tagLength+toByte[tagLength+1]+2;
            if (toByte[tagLength] == Constants.TLV_UUID ) {
                byte[] UID = new byte[4];
                System.arraycopy(toByte, tagLength+2, UID, 0, 4);
                deviceInfo.setUID(FMUtil.byteToHex(UID));
            }
            tagLength = tagLength+toByte[tagLength+1]+2;
            if (toByte[tagLength] == Constants.TLV_RFU ) {
                if (toByte[tagLength+2] == 0x00) {
                    deviceInfo.setCompress(false);
                } else {
                    deviceInfo.setCompress(true);
                }
                if(toByte[tagLength+3] == (byte) 0x20){
                    deviceInfo.setCosVersion(2);
                }
            }
            tagLength = tagLength+toByte[tagLength+1]+2;
            if(toByte[tagLength] == Constants.TLV_COLOR_INFO_EX)
            {
                int size = (toByte[tagLength+2] >> 4) & 0x0f;
                deviceInfo.setSize(size);
                int colorCount = toByte[tagLength+2] & 0x0F;

                deviceInfo.setColorCount(colorCount);
                StringBuilder colorType = new StringBuilder();
                deviceInfo.setDeviceType(6);
                for (int i = 0; i < colorCount; i++) {
                    String type = "";
                    byte value = (byte) ((toByte[tagLength + 4 + i * 2]) & 0xff);
                    switch (value) {
                        case 0x00:
                            int blackColorValue = toByte[tagLength + 5 + i * 2];
                            deviceInfo.setBlack(blackColorValue );
                            type = String.format("%s(%02X)", UIUtils.getString(mContext, R.string.text_color_black), (byte)blackColorValue);
                            break;
                        case 0x01:
                            int whiteColorValue = toByte[tagLength + 5 + i * 2];
                            deviceInfo.setWhite(whiteColorValue );
                            type = String.format("%s(%02X)", UIUtils.getString(mContext, R.string.text_color_white), (byte)whiteColorValue);
                            break;
                        case 0x02:
                            int redColorValue = toByte[tagLength + 5 + i * 2];
                            deviceInfo.setRed(redColorValue );
                            type = String.format("%s(%02X)", UIUtils.getString(mContext, R.string.text_color_red), (byte)redColorValue);
                            break;
                        case 0x03:
                            int yellowColorValue = toByte[tagLength + 5 + i * 2];
                            deviceInfo.setYellow(yellowColorValue );
                            type = String.format("%s(%02X)", UIUtils.getString(mContext, R.string.text_color_yellow), (byte)yellowColorValue);
                            break;
                        case 0x04:
                            int orangeColorValue = toByte[tagLength + 5 + i * 2];
                            deviceInfo.setOrangre(orangeColorValue );
                            type = String.format("%s(%02X)", UIUtils.getString(mContext, R.string.text_color_orange), (byte)orangeColorValue);
                            break;
                        case 0x05:
                            int greenColorValue = toByte[tagLength + 5 + i * 2];
                            deviceInfo.setGreen(greenColorValue );
                            type = String.format("%s(%02X)", UIUtils.getString(mContext, R.string.text_color_green), (byte)greenColorValue);
                            break;
                        case 0x06:
                            int cyanColorValue = toByte[tagLength + 5 + i * 2];
                            deviceInfo.setCyan(cyanColorValue );
                            type = String.format("%s(%02X)", UIUtils.getString(mContext, R.string.text_color_cyan), (byte)cyanColorValue);
                            break;
                        case 0x07:
                            int blueColorValue = toByte[tagLength + 5 + i * 2];
                            deviceInfo.setBlue(blueColorValue );
                            type = String.format("%s(%02X)", UIUtils.getString(mContext, R.string.text_color_blue), (byte)blueColorValue);
                            break;
                        case 0x08:
                            int violetColorValue = toByte[tagLength + 5 + i * 2];
                            deviceInfo.setViolet(violetColorValue );
                            type = String.format("%s(%02X)", UIUtils.getString(mContext, R.string.text_color_violet), (byte)violetColorValue);
                            break;
                        default:
                            break;
                    }
                    colorType.append(type);
                }
                deviceInfo.setColorType(colorType.toString());
            }
            tagLength = tagLength+toByte[tagLength+1]+2;
            if(toByte[toByte.length-2] == (byte)0x90 && toByte[toByte.length-1] == (byte)0x00){
                byte[] color_desc = new byte[14];
                String ColorDesc = null;
                System.arraycopy(toByte, toByte.length - 16, color_desc, 0, 14);
                if("4_color Screen".contentEquals(FMUtil.byteToString(color_desc)))
                {
                    deviceInfo.setColorDesc(FMUtil.byteToString(color_desc));
                    ColorDesc = deviceInfo.getColorDesc();
                }

                if(null != ColorDesc)
                {
                    if((14 == deviceInfo.getColorDesc().length())&&("4_color Screen".contentEquals(deviceInfo.getColorDesc())))
                    {
                        int x = Integer.parseInt(info.substring(10, 14), 16)/2;
                        int y = Integer.parseInt(info.substring(14, 18), 16);
                        deviceInfo.setScreen(y + "x" + x);
                        deviceInfo.setWidth(y);
                        deviceInfo.setHeight(x);
                        deviceInfo.setColor(UIUtils.getString(mContext, R.string.text_color_40_cn));
                        deviceInfo.setEN_Color("4-color");
                        deviceInfo.setDeviceType(3);
                        deviceInfo.setColorCount(4);
                        deviceInfo.setBlack(0);
                        deviceInfo.setWhite(1);
                        deviceInfo.setRed(3);
                        deviceInfo.setYellow(2);
                        String type = String.format("黑(0)白(1)红(3)黄(2)");
                        deviceInfo.setColorType(type);
                    }
                }

                deviceInfo.setPin(false);
            }else if(toByte[toByte.length-2] == (byte)0x69 && toByte[toByte.length-1] == (byte)0x85) {
                deviceInfo.setPin(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deviceInfo;

    }


    /**
     * 图片选择
     *
     * @param activity
     */
    public static void selectPicture(Activity activity) {
        PictureSelector.create(activity)
                .openGallery(PictureMimeType.ofImage())// 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
                .imageEngine(GlideEngine.createGlideEngine())// 外部传入图片加载引擎，必传项
                .theme(R.style.picture_default_style)// 主题样式设置 具体参考 values/styles   用法：R.style.picture.white.style v2.3.3后 建议使用setPictureStyle()动态方式
                .isWeChatStyle(true)// 是否开启微信图片选择风格
                .isUseCustomCamera(true)// 是否使用自定义相机
                //                .setLanguage(language)// 设置语言，默认中文
                .isPageStrategy(true)// 是否开启分页策略 & 每页多少条；默认开启

                .isWithVideoImage(false)// 图片和视频是否可以同选,只在ofAll模式下有效
                .isMaxSelectEnabledMask(true)// 选择数到了最大阀值列表是否启用蒙层效果
                //.isAutomaticTitleRecyclerTop(false)// 连续点击标题栏RecyclerView是否自动回到顶部,默认true
                .loadCacheResourcesCallback(GlideCacheEngine.createCacheEngine())// 获取图片资源缓存，主要是解决华为10部分机型在拷贝文件过多时会出现卡的问题，这里可以判断只在会出现一直转圈问题机型上使用
                //.setOutputCameraPath()// 自定义相机输出目录，只针对Android Q以下，例如 Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) +  File.separator + "Camera" + File.separator;
                //.setButtonFeatures(CustomCameraView.BUTTON_STATE_BOTH)// 设置自定义相机按钮状态
                .maxSelectNum(1)// 最大图片选择数量
                .minSelectNum(1)// 最小选择数量
                .maxVideoSelectNum(1) // 视频最大选择数量
                //.minVideoSelectNum(1)// 视频最小选择数量
                //.closeAndroidQChangeVideoWH(!SdkVersionUtils.checkedAndroid_Q())// 关闭在AndroidQ下获取图片或视频宽高相反自动转换
                .imageSpanCount(4)// 每行显示个数
                .isReturnEmpty(true)// 未选择数据时点击按钮是否可以返回
                .closeAndroidQChangeWH(false)//如果图片有旋转角度则对换宽高,默认为true
                .closeAndroidQChangeVideoWH(!SdkVersionUtils.checkedAndroid_Q())// 如果视频有旋转角度则对换宽高,默认为false
                .isAndroidQTransform(false)// 是否需要处理Android Q 拷贝至应用沙盒的操作，只针对compress(false); && .isEnableCrop(false);有效,默认处理
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)// 设置相册Activity方向，不设置默认使用系统
                .isOriginalImageControl(false)// 是否显示原图控制按钮，如果设置为true则用户可以自由选择是否使用原图，压缩、裁剪功能将会失效
                .selectionMode(PictureConfig.SINGLE)// 多选 or 单选
                .isSingleDirectReturn(true)// 单选模式下是否直接返回，PictureConfig.SINGLE模式下有效
                .isPreviewImage(true)// 是否可预览图片
                .isPreviewVideo(true)// 是否可预览视频
                .isCamera(false) //是否显示拍照按钮
                .isZoomAnim(true)// 图片列表点击 缩放效果 默认true
                //.imageFormat(PictureMimeType.PNG)// 拍照保存图片格式后缀,默认jpeg,Android Q使用PictureMimeType.PNG_Q
                .isEnableCrop(false)// 是否裁剪
                //.basicUCropConfig()//对外提供所有UCropOptions参数配制，但如果PictureSelector原本支持设置的还是会使用原有的设置
                .isCompress(false)// 是否压缩
                //.compressQuality(80)// 图片压缩后输出质量 0~ 100
                .synOrAsy(true)//同步true或异步false 压缩 默认同步

                .minimumCompressSize(100000)// 小于多少kb的图片不压缩

                .forResult(PictureConfig.CHOOSE_REQUEST);
    }

    public static void openCamera(Activity activity) {
        PictureSelector.create(activity)
                .openCamera(PictureMimeType.ofImage())
                .loadImageEngine(GlideEngine.createGlideEngine())
                .forResult(PictureConfig.CHOOSE_REQUEST);

    }

    public static void  uCrop(AppCompatActivity activity,String path){
        UCrop.Options options = new UCrop.Options();
        String[] split = path.split("\\.");
        Uri destinationUri = Uri.fromFile(new File(BmpUtils.getImagePath("uCrop."+split[split.length-1])));
        Uri sourceUri = Uri.fromFile(new File(path));
        UCrop uCrop = UCrop.of(sourceUri, destinationUri);//第一个参数是裁剪前的uri,第二个参数是裁剪后的uri
        uCrop.withAspectRatio(App.getDeviceInfo().getWidth(), App.getDeviceInfo().getHeight());//设置裁剪框的宽高比例
        uCrop.withMaxResultSize(App.getDeviceInfo().getWidth(),App.getDeviceInfo().getHeight());
        //下面参数分别是缩放,旋转,裁剪框的比例
//        options.setAllowedGestures(com.yalantis.ucrop.UCropActivity.ALL, com.yalantis.ucrop.UCropActivity.NONE, com.yalantis.ucrop.UCropActivity.ALL);
        options.setToolbarTitle(UIUtils.getString(activity, R.string.string_res_38));//设置标题栏文字
        options.setCropGridStrokeWidth(2);//设置裁剪网格线的宽度(我这网格设置不显示，所以没效果)
        //options.setCropFrameStrokeWidth(1);//设置裁剪框的宽度
//        options.setMaxScaleMultiplier(2);//设置最大缩放比例
        options.setHideBottomControls(false);//隐藏下边控制栏
        options.setShowCropGrid(true);  //设置是否显示裁剪网格
        //options.setOvalDimmedLayer(true);//设置是否为圆形裁剪框
        options.setShowCropFrame(true); //设置是否显示裁剪边框(true为方形边框)
        options.setToolbarWidgetColor(Color.parseColor("#ffffff"));//标题字的颜色以及按钮颜色
        options.setDimmedLayerColor(Color.parseColor("#AA000000"));//设置裁剪外颜色
        options.setToolbarColor(Color.parseColor("#000000")); // 设置标题栏颜色
        options.setStatusBarColor(Color.parseColor("#000000"));//设置状态栏颜色
        options.setCropGridColor(Color.parseColor("#ffffff"));//设置裁剪网格的颜色
        options.setCropFrameColor(Color.parseColor("#ffffff"));//设置裁剪框的颜色
        uCrop.withOptions(options);
        uCrop.start(activity,UCrop.REQUEST_CROP);

    }

    public static void sendMessage(String msg, int what) {
        Message message = new Message();
        message.what = what;
        message.obj = msg;
        if (App.getHandler() != null) {
            App.getHandler().sendMessage(message);
        }
    }

    public static boolean isCN(Context context){
        Configuration configuration = UIUtils.getResources(context).getConfiguration();
        String country = configuration.locale.getLanguage();
        LogUtil.d(country);
        if("zh".equals(country)){
            return true;
        }
        return false;

    }

}
