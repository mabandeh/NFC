package com.fmsh.einkesl.tools;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;

import com.bumptech.glide.util.Util;
import com.fmsh.base.utils.ActivityUtils;
import com.fmsh.base.utils.BroadcastManager;
import com.fmsh.base.utils.Constant;
import com.fmsh.base.utils.FMUtil;
import com.fmsh.base.utils.HintDialog;
import com.fmsh.base.utils.LogUtil;
import com.fmsh.base.utils.NfcConstant;
import com.fmsh.base.utils.UIUtils;
import com.fmsh.compress.CompressUtils;
import com.fmsh.einkesl.App;
import com.fmsh.einkesl.R;
import com.fmsh.einkesl.bean.DeviceInfo;
import com.fmsh.einkesl.tools.image.BmpUtils;
import com.fmsh.einkesl.tools.image.ImageUtils;
import com.fmsh.einkesl.utils.EncryUtils;
import com.fmsh.einkesl.utils.IUtils;

import java.io.IOException;

/**
 * @author wuyajiang
 * @date 2020/5/20
 * 1443-4芯片
 */
public class IsoDepUtils {
    private static String mBmpPath;

    /**
     * 判断是否包含1443-4
     *
     * @param tag
     * @return
     */
    public static boolean isContains(Tag tag) {
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep == null) {
            return false;
        }
        return true;
    }

    /**
     * 获取AST
     *
     * @param tag
     * @return
     */
    public static byte[] getATS(Tag tag) {
        IsoDep isoDep = IsoDep.get(tag);
        byte[] ast = new byte[2];
        if (isoDep != null) {
            ast = isoDep.getHistoricalBytes();
        }
        return ast;
    }

    public static void startIsoDep(Bundle bundle) {
        Tag tag = bundle.getParcelable(NfcConstant.KEY_TAG);
        if (tag == null) {
            return;
        }
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep != null) {
            try {
                isoDep.setTimeout(50000);
                if (!isoDep.isConnected()) {
                    isoDep.connect();
                }
                byte[] transceive = isoDep.transceive(FMUtil.hexToByte("00A4040007D2760000850101"));
                LogUtil.d(FMUtil.byteToHex(transceive));
                LogUtil.d("start", System.currentTimeMillis());
                switch (bundle.getInt("position")) {
                    case 0:
                        getDeviceInfo(isoDep);
                        break;
                    case 1:
                        if (App.getDeviceInfo().getPin()) {
                            if (verifyPinCode(isoDep, bundle.getString("pin"))) {
                                mBmpPath = bundle.getString("path");
                                handlerImage(isoDep, bundle.getBoolean("isLvl"));
                            }
                        } else {
                            mBmpPath = bundle.getString("path");
                            handlerImage(isoDep, bundle.getBoolean("isLvl"));
                        }
                        break;
                    case 2:
                        if (bundle.getBoolean("isPin")) {
                            if (verifyPinCode(isoDep, bundle.getString("pin"))) {
                                sendApdu(isoDep, bundle.getString("apdu"));
                            }
                        } else {
                            sendApdu(isoDep, bundle.getString("apdu"));
                        }
                        break;
                    case 3:
                        updatePin(isoDep, bundle.getString("oldPin"), bundle.getString("pin"));
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                UIUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error), -1, App.getHandler());
            } finally {
                try {
                    isoDep.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private static void sendApdu(IsoDep isoDep, String apdu) throws IOException {
        String[] mApdu = apdu.split(",");
        if(mApdu.length > 0) {
            IUtils.sendMessage("", 2);
        }
        for(int i = 0;i< mApdu.length;i++)
        {
            IUtils.sendMessage(">>:" + mApdu[i]+"\r\n", 1);
            byte[] bytes = isoDep.transceive(FMUtil.hexToByte(mApdu[i]));
            IUtils.sendMessage("<<:" + FMUtil.byteToHex(bytes)+"\r\n", 1);
        }
    }

    private static void handlerImage(IsoDep isoDep, boolean isLvl) throws IOException {
        byte[] result = isoDep.transceive(FMUtil.hexToByte("00D1000000"));
        byte[] result2 = isoDep.transceive(FMUtil.hexToByte("F0D8000005000000000E"));
        DeviceInfo deviceInfo = IUtils.loadDeviceInfo(ActivityUtils.instance.getCurrentActivity(), FMUtil.byteToHex(result)+FMUtil.byteToHex(result2));
        boolean bmpFormat = BmpUtils.GetBmpFormat(mBmpPath, deviceInfo);
        if (bmpFormat) {
            int color_cnt = App.getDeviceInfo().getColorCount();
            if (color_cnt == 2) {
                writeBlackWhiteScreen(isoDep);
            } else if (color_cnt == 3){
                write3ColorScreen(isoDep, isLvl);
            }
            else {
                if((color_cnt == 4)&&(null != App.getDeviceInfo().getColorDesc()))
                {
                    write4ColorScreen(isoDep, isLvl);
                }
                else
                {
                    writeMulColorScreen(isoDep, isLvl);
                }
            }
        } else {
            UIUtils.sendMessage(UIUtils.getString(R.string.bmp_error), -1, App.getHandler());
        }
    }

    /**
     * 黑白图片刷屏
     *
     * @param isoDep
     */
    private static void writeBlackWhiteScreen(IsoDep isoDep) {
        byte[] bmpdata = ImageUtils.ReadBmp8File(mBmpPath);
        if (bmpdata == null) {
            UIUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error), -1, App.getHandler());
            return;
        }
        LogUtil.d(FMUtil.byteToHex(bmpdata));

        int destRealWidth = (int) Math.ceil(App.getDeviceInfo().getWidth() / 8.0);
        int destWidth = 0;
        int percent = destRealWidth % 4;
        if (percent != 0) {
            destWidth += destRealWidth - percent + 4;
        } else {
            destWidth = destRealWidth;
        }
        int vbitsRow = (int) Math.ceil(App.getDeviceInfo().getHeight() / 8.0);

        byte[] result = new byte[bmpdata.length];
        if (App.getDeviceInfo().getRefreshScan() == 0) {
            ImageUtils.VerticalScanning(result, bmpdata, destRealWidth, vbitsRow, destWidth);

            LogUtil.d("转换", System.currentTimeMillis());
        } else {
            //            byte[] src = new byte[bmpdata.length-0x3E];
            //            System.arraycopy(bmpdata,0x3E,src,0,src.length);
            result = ImageUtils.HorizontalScanning(bmpdata, destRealWidth, App.getDeviceInfo().getHeight(), destWidth);
        }
        // 行数等于总的字节数 / 每次发送的字节数250 / 8 向上取整
        int rowCount = (int) Math.ceil(App.getDeviceInfo().getWidth() * App.getDeviceInfo().getHeight() / 250 / 8.0);
        if (!WriteBmpdataEx(isoDep, result, rowCount, 0)) {
            return;
        }
        RefreshScreenAfterWriteBmpdataex(isoDep, 0);
    }
    private static void write4ColorScreen(IsoDep isoDep, boolean isLvl) {
        byte[] bmpdata = ImageUtils.ReadBmp24File(mBmpPath, isLvl);
        if (bmpdata == null) {
            UIUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error), -1, App.getHandler());
            return;
        }
        int width = App.getDeviceInfo().getWidth();
        int height = App.getDeviceInfo().getHeight();
        byte[] data1 = new byte[width * height];
        byte[] data2 = new byte[width * height];
        ImageUtils.getColorDataBmp4(bmpdata, data1);

        byte[] result = null;
        byte[] result1 = null;
        String ColorDesc = App.getDeviceInfo().getColorDesc();
        if(null == ColorDesc)
        {
            if (App.getDeviceInfo().getRefreshScan() == 0) {
                result = ImageUtils.Color24VerticalScanning(data1);
                result1 = ImageUtils.Color24VerticalScanning(data2);
            } else {
                result = ImageUtils.color24horizontalScanning(data1);
                result1 = ImageUtils.color24horizontalScanning(data2);
            }
        }
        else if(App.getDeviceInfo().getColor().contentEquals("黑白红黄四色"))
        {
            if (App.getDeviceInfo().getRefreshScan() == 0) {
                result = ImageUtils.Color4VerticalScanning(data1);
            } else {
                result = ImageUtils.color4horizontalScanning(data1);
            }
        }
        // 行数等于总的字节数 / 每次发送的字节数250 / 8 向上取整
        int rowCount = (int) Math.ceil(width * height / 250 / 8.0);

        if(null == ColorDesc)
        {
            if (App.getDeviceInfo().getSize() == 1) {
                if (!WriteBmpdataEx(isoDep, result, rowCount, 0)) {
                    return;
                }

            } else if (App.getDeviceInfo().getSize() == 2) {
                if (!WriteBmpdataEx(isoDep, result, rowCount, 0)) {
                    return;
                }
                if (!WriteBmpdataEx(isoDep, result1, rowCount, 1)) {
                    return;
                }

            }
        }
        else if(App.getDeviceInfo().getColor().contentEquals("黑白红黄四色"))
        {
            if (!WriteBmpdataEx(isoDep, result, rowCount*2, 0)) {
                return;
            }
        }
        RefreshScreenAfterWriteBmpdataex(isoDep, 0);
    }

    private static void write3ColorScreen(IsoDep isoDep, boolean isLvl) {
        byte[] bmpdata = ImageUtils.ReadBmp24File(mBmpPath, isLvl);
        if (bmpdata == null) {
            UIUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error), -1, App.getHandler());
            return;
        }
        int width = App.getDeviceInfo().getWidth();
        int height = App.getDeviceInfo().getHeight();
        byte[] data1 = new byte[width * height];
        byte[] data2 = new byte[width * height];
        ImageUtils.getColorDataBmp24(bmpdata, data1,data2);

        byte[] result = null;
        byte[] result1 = null;
        if (App.getDeviceInfo().getRefreshScan() == 0) {
            result = ImageUtils.Color24VerticalScanning(data1);
            result1 = ImageUtils.Color24VerticalScanning(data2);
        } else {
            result = ImageUtils.color24horizontalScanning(data1);
            result1 = ImageUtils.color24horizontalScanning(data2);
        }
        // 行数等于总的字节数 / 每次发送的字节数250 / 8 向上取整
        int rowCount = (int) Math.ceil(width * height / 250 / 8.0);


        if (App.getDeviceInfo().getSize() == 1) {
            if (!WriteBmpdataEx(isoDep, result, rowCount, 0)) {
                return;
            }

        } else if (App.getDeviceInfo().getSize() == 2) {
            if (!WriteBmpdataEx(isoDep, result, rowCount, 0)) {
                return;
            }
            if (!WriteBmpdataEx(isoDep, result1, rowCount, 1)) {
                return;
            }

        }
        RefreshScreenAfterWriteBmpdataex(isoDep, 0);

    }
    private static void writeMulColorScreen(IsoDep isoDep, boolean isLvl) {
        byte[] bmpdata = ImageUtils.ReadBmp24File(mBmpPath, isLvl);
        if (bmpdata == null) {
            UIUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error), -1, App.getHandler());
            return;
        }
        int width = App.getDeviceInfo().getWidth();
        int height = App.getDeviceInfo().getHeight();
        int pic_num = App.getDeviceInfo().getSize();
        byte[][] data = new byte[pic_num][width * height];
        ImageUtils.getColorDataBmp24(bmpdata, data);

        byte[][] result = new byte[pic_num][];
        if (App.getDeviceInfo().getRefreshScan() == 0) {
            UIUtils.sendMessage("当前屏幕不支持垂直扫描模式", -1, App.getHandler());
            return;
        } else {
            for(int i = 0; i < pic_num; i++)
            {
                result[i] = ImageUtils.multicolor24horizontalScanning(data[i]);
            }
        }
        // 行数等于总的字节数 / 每次发送的字节数250 / 8 向上取整
        int rowCount = (int) Math.ceil(width * height / 250 / 8.0);


        if (App.getDeviceInfo().getSize() == 1) {
            if (!WriteBmpdataEx(isoDep, result[0], rowCount, 0)) {
                return;
            }

        } else if (App.getDeviceInfo().getSize() == 2) {
            if (!WriteBmpdataEx(isoDep, result[0], rowCount, 0)) {
                return;
            }
            if (!WriteBmpdataEx(isoDep, result[1], rowCount, 1)) {
                return;
            }
        }
        else
        {
            for(int i = 0; i < pic_num; i++)
            {
                if (!WriteBmpdataEx(isoDep, result[i], rowCount, i)) {
                    return;
                }
            }
        }
        RefreshScreenAfterWriteBmpdataex(isoDep, 0);
    }
    /**
     * @param result
     * @param rowCount
     * @return
     */
    private static boolean WriteBmpdataEx(IsoDep isoDep, byte[] result, int rowCount, int ScreenIndex) {
        boolean isCompress = App.getDeviceInfo().isCompress();
        //boolean isCompress =false;
        int m_bmpWidth = App.getDeviceInfo().getWidth();
        int m_bmpHeight = App.getDeviceInfo().getHeight();
        int size=0;
        if (isCompress) {
            try {
                //LogUtil.d("压缩", System.currentTimeMillis());
                //LogUtil.d(FMUtil.byteToHex(result));
                int p = m_bmpHeight % 8;
                if (p != 0) {
                    m_bmpHeight = (8 - p) + m_bmpHeight;
                }
                String ColorDesc = App.getDeviceInfo().getColorDesc();
                if(null == ColorDesc)
                {
                    size = m_bmpWidth * m_bmpHeight / 8;
                }
                else if(App.getDeviceInfo().getColor().contentEquals("黑白红黄四色"))
                {
                    size = m_bmpWidth * m_bmpHeight / 4;
                }
//                                if(App.getDeviceInfo().getColorCount() == 2){
//                                   int width =  (int) Math.ceil(m_bmpWidth / 8.0);
//                                     size = width * m_bmpHeight /8;
//                                }
                byte[] bytes = new byte[size];
                System.arraycopy(result, 0, bytes, 0, bytes.length);
                result = bytes;
                //LogUtil.d(FMUtil.byteToHex(result));
                String compress = CompressUtils.compress(isoDep, result, ScreenIndex);
                LogUtil.d("压缩完成", System.currentTimeMillis());
                if ("9000".equals(compress)) {
                    return true;
                } else {
                    UIUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_compress_error) + compress, -1, App.getHandler());
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                IUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error), -1);
                return false;
            }

        } else {
            LogUtil.d("发送", System.currentTimeMillis());
            if (result.length < rowCount * 0xFA) {
                byte[] temp = new byte[rowCount * 0xFA];
                System.arraycopy(result, 0, temp, 0, result.length);
                result = temp;
            }
            byte[] apdu = new byte[0xFA + 5];
            apdu[0] = (byte) 0xF0;
            apdu[1] = (byte) 0xD2;
            apdu[2] = (byte) ScreenIndex;
            apdu[4] = (byte) 0xFA;
            for (int i = 0; i < rowCount; i++) {
                // 索引号
                apdu[3] = (byte) i;
                for (int j = 0; j < 0xFA; j++) {
                    apdu[5 + j] = (byte) result[i * 0xFA + j];
                }
                try {
                    LogUtil.d(FMUtil.byteToHex(apdu));
                    byte[] datasw = isoDep.transceive(apdu);

                    String strResponse = FMUtil.byteToHex(datasw);
                    if (!strResponse.substring(strResponse.length() - 4).equals("9000")) {
                        IUtils.sendMessage(UIUtils.getString(R.string.hint_error), -1);
                        return false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    IUtils.sendMessage(UIUtils.getString(R.string.hint_error), -1);
                    return false;
                }
            }
        }
        return true;
    }

    private static int sendApdu_back6986_index = 5;

    private static void RefreshScreenAfterWriteBmpdataex(IsoDep isoDep, int ScreenIndex) {
        LogUtil.d("刷屏", System.currentTimeMillis());
        byte[] refreshcmd = new byte[0x5];
        refreshcmd[0] = (byte) 0xF0;
        refreshcmd[1] = (byte) 0xd4;
        refreshcmd[2] = (byte) 0x05;
        refreshcmd[3] = (byte) (ScreenIndex | (byte) 0x80);
        refreshcmd[4] = (byte) 0x00;
        try {
            LogUtil.d(FMUtil.byteToHex(refreshcmd));
            byte[] datasw = isoDep.transceive(refreshcmd);
            String strResponse = FMUtil.byteToHex(datasw);
            LogUtil.d(strResponse);

            if (strResponse.equals("698A")) {
                String strtmp = ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error);
                IUtils.sendMessage(strtmp, -1);
            } else if (strResponse.equals("6986") || "68C6".equals(strResponse)) {
                if (sendApdu_back6986_index > 0) {
                    sendApdu_back6986_index--;
                    //RefreshScreenAfterWriteBmpdataex(isoDep, ScreenIndex);
                    datasw = isoDep.transceive(FMUtil.hexToByte("F0D4850000"));
                    strResponse = FMUtil.byteToHex(datasw);
                    LogUtil.d(strResponse);
                    if(strResponse.equals("009000") || strResponse.equals("9000"))
                    {
                        sendApdu_back6986_index = 5;
                        IUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.text_success), 0);
                    }
                } else {
                        String strtmp = ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.string_res_37);
                        IUtils.sendMessage(strtmp, -1);
                        sendApdu_back6986_index = 5;
                }


            } else if (strResponse.equals("019000")) {
                //有源
                sendApdu_back6986_index = 5;
                IUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.text_success), 0);
            } else if (strResponse.equals("009000") || strResponse.equals("9000")) {
                //无源
                sendApdu_back6986_index = 5;
                GetRefreshResult(isoDep, ScreenIndex);
            } else {
                refreshcmd[3] = (byte) ScreenIndex;
                datasw = isoDep.transceive(refreshcmd);
                strResponse = FMUtil.byteToHex(datasw);
                if (!strResponse.equals("9000")) {
                    // RevEdit.setText("刷屏时发生错误，错误码:" + strResponse);
                    String strtmp = ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.string_res_16);
                    IUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error), -1);
                } else {
                    IUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.text_success), 0);
                }
            }
            LogUtil.d("刷屏", System.currentTimeMillis());
        } catch (IOException e) {
            e.printStackTrace();
            IUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error), -1);
        }

    }


    private static void GetRefreshResult(IsoDep isoDep, int ScreenIndex) {
        byte[] refreshcmd = new byte[0x5];
        refreshcmd[0] = (byte) 0xF0;
        refreshcmd[1] = (byte) 0xd4;
        refreshcmd[2] = (byte) 0x05;
        refreshcmd[3] = (byte) (ScreenIndex | (byte) 0x80);
        refreshcmd[4] = (byte) 0x00;
        String m_strLastErrorMsg = "Error";
        try {
            byte[] datasw = null;
            String strResponse;
            //循环去取刷屏幕的结果
            refreshcmd[1] = (byte) 0xde;
            refreshcmd[2] = (byte) 0x0;
            refreshcmd[3] = (byte) (0x0);
            refreshcmd[4] = (byte) 0x01;


            for (int i = 0; i < 1000; i++) {
                LogUtil.d(FMUtil.byteToHex(refreshcmd));
                datasw = isoDep.transceive(refreshcmd);
                strResponse = FMUtil.byteToHex(datasw);
                Log.d("<-", strResponse);
                if (strResponse.equals("009000")) {
                    IUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.text_success), 0);
                    LogUtil.d("success", System.currentTimeMillis());
                    return;
                } else if (strResponse.equals("019000")) {
                    Thread.sleep(100); //还需要等待
                } else if (strResponse.equals("698A")) {
                    String strtmp = ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error);
                    m_strLastErrorMsg = strtmp + strResponse;
                    IUtils.sendMessage(m_strLastErrorMsg, -1);
                    return;
                } else if (strResponse.equals("6986") || "68C6".equals(strResponse)) {
                    String strtmp = ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.string_res_37);
                    IUtils.sendMessage(strtmp, -1);
                    return;
                } else {
                    String strtmp = ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error);
                    m_strLastErrorMsg = strtmp + strResponse;
                    IUtils.sendMessage(strtmp, -1);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            IUtils.sendMessage(UIUtils.getString(R.string.hint_error), -1);
        }
    }


    /**
     * 获取设备信息
     *
     * @param isoDep
     * @throws IOException
     */
    private static void getDeviceInfo(IsoDep isoDep) throws IOException {

        //获取配置信息
        byte[] result = isoDep.transceive(FMUtil.hexToByte("00D1000000"));
        //检测是否支持pin
        byte[] transceive = isoDep.transceive(FMUtil.hexToByte("F0D8000005000000000E"));
        String toHex = FMUtil.byteToHex(transceive);


        UIUtils.sendMessage(FMUtil.byteToHex(result) + toHex/*.substring(toHex.length())*/, 0, App.getHandler());
    }

    private static void updatePin(IsoDep isodep, String oldPin, String pin) {


        try {
            byte[] appId = FMUtil.hexToByte(App.getDeviceInfo().getAppID());
            byte[] uid = FMUtil.hexToByte(App.getDeviceInfo().getUID());

            for (int i = 0; i < 4; i++) {
                appId[i] = (byte) ~appId[i];
                uid[i] = (byte) ~uid[i];
            }
            String key = FMUtil.byteToHex(uid) + FMUtil.byteToHex(appId);
            LogUtil.d(key);

            //获取随机数
            byte[] randByte = isodep.transceive(new byte[]{0x0, (byte) 0x84, 0x00, 0x00, 0x04});
            byte[] ivRand = new byte[8];
            System.arraycopy(randByte, 0, ivRand, 0, 4);
            String randStr = FMUtil.byteToHex(ivRand);
            LogUtil.d(randStr);
            String command = "80D90101" + String.format("%02x", (oldPin.length() / 2 + pin.length() / 2 + 5)) + oldPin + "FF" + pin;
            String m = command;
            int remainder = m.length() & 16;
            if (remainder == 0) {
                m += "8000000000000000";
            } else {
                m += "80";
                while (m.length() % 16 != 0) {
                    m += "00";
                }
            }
            LogUtil.d(m);
            for (int i = 0; i < m.length() / 16; i++) {
                String cbcDes = EncryUtils.encryptDES(randStr, key, m.substring(i * 16, 16 * (i + 1)));
                randStr = cbcDes;
                LogUtil.d(cbcDes);
            }
            command = command + randStr.substring(0, 8);
            LogUtil.d(command);
            byte[] bytes = isodep.transceive(FMUtil.hexToByte(command));
            String toHex = FMUtil.byteToHex(bytes);
            LogUtil.d(toHex);
            BroadcastManager.getInstance(ActivityUtils.instance.getCurrentActivity()).sendBroadcast("pin", toHex);
        } catch (Exception e) {
            e.printStackTrace();
            BroadcastManager.getInstance(ActivityUtils.instance.getCurrentActivity()).sendBroadcast("pin", e.getMessage());
        }


    }

    private static boolean verifyPinCode(IsoDep isoDep, String pin) throws IOException {
        byte[] transceive = isoDep.transceive(FMUtil.hexToByte(String.format("00200001%02x%s", pin.length() / 2, pin)));
        String result = FMUtil.byteToHex(transceive);
        LogUtil.d(transceive);
        if ("9000".equals(result)) {
            return true;
        } else {
            IUtils.sendMessage(UIUtils.getString(R.string.string_res_5) + "(" + result + ")", -1);
            return false;
        }

    }


}
