package com.fmsh.einkesl.tools;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;

import com.fmsh.base.utils.ActivityUtils;
import com.fmsh.base.utils.BroadcastManager;
import com.fmsh.base.utils.FMUtil;
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
import java.util.ArrayList;

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
        String[] mApdu = resetBig();
       // String[] mApdu = apdu.split(",");

        if (mApdu.length > 0) {
            IUtils.sendMessage("", 2);
        }
        for (int i = 0; i < mApdu.length; i++) {
            IUtils.sendMessage(">>:" + mApdu[i] + "\r\n", 1);
            byte[] bytes = isoDep.transceive(FMUtil.hexToByte(mApdu[i]));
            IUtils.sendMessage("<<:" + FMUtil.byteToHex(bytes) + "\r\n", 1);
        }
    }

    private static void handlerImage(IsoDep isoDep, boolean isLvl) throws IOException {
        byte[] result = isoDep.transceive(FMUtil.hexToByte("00D1000000"));
        byte[] result2 = isoDep.transceive(FMUtil.hexToByte("F0D8000005000000000E"));
        DeviceInfo deviceInfo = IUtils.loadDeviceInfo(ActivityUtils.instance.getCurrentActivity(), FMUtil.byteToHex(result) + FMUtil.byteToHex(result2));
        boolean bmpFormat = BmpUtils.GetBmpFormat(mBmpPath, deviceInfo);
        if (bmpFormat) {
            int color_cnt = App.getDeviceInfo().getColorCount();
            if (color_cnt == 2) {
                writeBlackWhiteScreen(isoDep);
            } else if (color_cnt == 3) {
                write3ColorScreen(isoDep, isLvl);
            } else {
                if ((color_cnt == 4) && (null != App.getDeviceInfo().getColorDesc())) {
                    write4ColorScreen(isoDep, isLvl);
                } else {
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
//byte[] src = new byte[bmpdata.length-0x3E];
//System.arraycopy(bmpdata,0x3E,src,0,src.length);
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
        if (null == ColorDesc) {
            if (App.getDeviceInfo().getRefreshScan() != 0) {
                result = ImageUtils.Color24VerticalScanning(data1);
                result1 = ImageUtils.Color24VerticalScanning(data2);
            } else {
                result = ImageUtils.color24horizontalScanning(data1);
                result1 = ImageUtils.color24horizontalScanning(data2);
            }
        } else if (App.getDeviceInfo().getColor().contentEquals("黑白红黄四色")) {
            if (App.getDeviceInfo().getRefreshScan() == 0) {
                result = ImageUtils.Color4VerticalScanning(data1);
            } else {
                result = ImageUtils.color4horizontalScanning(data1);
            }
        }
// 行数等于总的字节数 / 每次发送的字节数250 / 8 向上取整
        int rowCount = (int) Math.ceil(width * height / 250 / 8.0);

        if (null == ColorDesc) {
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
        } else if (App.getDeviceInfo().getColor().contentEquals("黑白红黄四色")) {
            if (!WriteBmpdataEx(isoDep, result, rowCount * 2, 0)) {
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
        ImageUtils.getColorDataBmp24(bmpdata, data1, data2);

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
            for (int i = 0; i < pic_num; i++) {
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
        } else {
            for (int i = 0; i < pic_num; i++) {
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
        boolean isCompress = false;
//boolean isCompress =false;
        int m_bmpWidth = App.getDeviceInfo().getWidth();
        int m_bmpHeight = App.getDeviceInfo().getHeight();
        int size = 0;
        if (isCompress) {
            try {
//LogUtil.d("压缩", System.currentTimeMillis());
//LogUtil.d(FMUtil.byteToHex(result));
                int p = m_bmpHeight % 8;
                if (p != 0) {
                    m_bmpHeight = (8 - p) + m_bmpHeight;
                }
                String ColorDesc = App.getDeviceInfo().getColorDesc();
                if (null == ColorDesc) {
                    size = m_bmpWidth * m_bmpHeight / 8;
                } else if (App.getDeviceInfo().getColor().contentEquals("黑白红黄四色")) {
                    size = m_bmpWidth * m_bmpHeight / 4;
                }
//if(App.getDeviceInfo().getColorCount() == 2){
//   int width =  (int) Math.ceil(m_bmpWidth / 8.0);
// size = width * m_bmpHeight /8;
//}
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
                    if (strResponse.equals("009000") || strResponse.equals("9000")) {
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

    private static String[] resetBig() {
        ArrayList<String> mApdu = new ArrayList<>();
       // mApdu.add("F0D70000FF0000000053305648545441304D6A41774D5651774D773D3D242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424242424 ");

        mApdu.add("F0DB020000");
        mApdu.add("F0DB000080A00603300190012CA5020014A4010CA502000AA40108A502000AA4010CA502000AA40102A10112A40102A1027454A1027E3BA1032B0463A1050C8E8C853FA104012B0100A1021100A103443100A105452B010000A1023C01A1024E00A1034F2B01A1021880A10222B1A10120A40102A3022426A40102A20222C7A20120A40102");
        mApdu.add("F0DA000003F00330");
        mApdu.add("F0DA00010423003048");
        mApdu.add("F0D20000FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20001FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20002FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20003FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20004FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20005FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20006FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20007FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20008FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20009FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2000AFAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2000BFAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2000CFAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE00000000000000000000FFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2000DFAFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2000EFAFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2000FFAFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20010FAFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20011FAFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20012FAFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20013FAFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20014FAFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE00000000000000000000FFFFFFFFFFFFFFFFFFFF000000000000000000007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20015FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20016FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20017FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20018FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20019FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2001AFAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2001BFAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE0007FFE03FFFCFFFFFFFFFFFF8FFFFFE03FFF003FFCFFFF1FC007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC0003FF000FFF8FFFFFFFFFFFF8F3FFF801FFE0003FC7FFF1F8000FFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2001CFAFFFFFFFFFFFFFFFFFFFFFFFC0007FE0003FF87FFFFFFFFFFF8E3FFE000FFE0001FC7E3F1F80007FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFFC1FC1FF87FFFFFFFFFFF1E3FFC1F87FE1FE1FC7C3F1F87F83FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFFC7FF1FF87FFFFFFFFFFF1E3FFC3FC7FE1FF0FC7C1F1F87FE3FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFF87FF1FF87FFFFFFFFFFE1E3FFEFFC3FE1FF8FC7C1E1F87FE1FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFF87FFFFF87FFFFFFFFFFE3E3FFFFFC3FE1FF8FC7C1E1F87FE1FFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2001DFAFFFFFFFFFFFFFFFFFFFFFFFC7FFFF87FFFFF87FFFFFFFFFFE3E3FFFFFC3FE1FF8FC381E1F87FE1FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFFC3FFFFF87FFFFFFFFFFC3E3FFFFFC3FE1FF0FC388E3F87FE1FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFFC1FFFFF87FFFFFFFFFFC7E3FFFFFC7FE1FF1FC388E3F87FE3FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFFE03FFFF87FFFFFFFFFFC7E3FFFFF87FE1FC1FE388E3F87FC3FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC001FFF007FFF87FFFFFFFFFF8FE3FFFFF8FFE0007FE308E3F80007FFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2001EFAFFFFFFFFFFFFFFFFFFFFFFFC000FFFC00FFF87FFFFE003FF8FE3FFFFF0FFE0007FE31C63F8000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC001FFFF807FF87FFFFC003FF0FE3FFFFE1FFE1F01FE31C63F8003FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFFFFF81FF87FFFFE003FF1FE3FFFFC3FFE1FF0FE31C63F87E3FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFFFFFE1FF87FFFFFFFFFF00003FFFC7FFE1FF8FE31C43F87E1FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFFFFFF0FF87FFFFFFFFFF00001FFF87FFE1FF87E23C03F87F1FFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2001FFAFFFFFFFFFFFFFFFFFFFFFFFC7FFFFFFFF0FF87FFFFFFFFFF80003FFF0FFFE1FF87E23E07F87F0FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFFFFFF8FF87FFFFFFFFFFFFE3FFFE1FFFE1FF87F03E07F87F8FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFF8FFF0FF87FFFFFFFFFFFFE3FFFC3FFFE1FF87F03E07F87F87FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFF8FFF0FF87FFFFFFFFFFFFE3FFF07FFFE1FF87F03E07F87F87FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7FFFF83FE1FF87FFFFFFFFFFFFE3FFE1FFFFE1FF0FF07F07F87FC7FFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20020FAFFFFFFFFFFFFFFFFFFFFFFFC000FFC0F81FF8001FFFFFFFFFFE3FFC0007FE0001FF07F07F87FC3FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC0003FE0007FF8000FFFFFFFFFFE3FFC0003FE0003FF07F07F87FE3FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC0007FF800FFFC000FFFFFFFFFFF3FFC0003FF000FFF0FF0FF8FFE3FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20021FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20022FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20023FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20024FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20025FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20026FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20027FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20028FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20029FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2002AFAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2002BFAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2002CFAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2002DFAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2002EFAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2002FFAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF870C083020C081FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF7AF5FBD7EF7EBFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20030FAFFF7EF5FBD7EF7DBFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF7EF5FBD7EF7DBFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF06F5FBD7EF7BBFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF7AF4083060FB81FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF7AF5FBD7EF7BBFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20031FAFFF7AF5FBD7EF77BFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF7AF5FBD7EF77BFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF7AF5FBD7EF77BFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF870C0837E0F781FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20032FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20033FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20034FAFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20035FAFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20036FAFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20037FAFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20038FAFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE19E07818066066019F867E7E61F9F87E67E61F8186067E19F8186061F81987FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20039FAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2003AFAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D2003BFAFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        mApdu.add("F0D20100FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20101FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20102FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20103FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20104FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20105FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20106FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20107FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20108FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20109FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2010AFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2010BFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2010CFA0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000000");
        mApdu.add("F0D2010DFA0000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000000");
        mApdu.add("F0D2010EFA0000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000000");
        mApdu.add("F0D2010FFA0000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000000");
        mApdu.add("F0D20110FA0000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000000");
        mApdu.add("F0D20111FA0000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000000");
        mApdu.add("F0D20112FA0000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000000");
        mApdu.add("F0D20113FA0000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000000");
        mApdu.add("F0D20114FA0000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20115FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20116FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20117FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20118FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20119FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2011AFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2011BFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2011CFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2011DFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2011EFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2011FFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20120FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20121FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20122FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20123FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20124FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20125FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20126FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20127FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20128FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20129FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2012AFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2012BFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2012CFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2012DFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2012EFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2012FFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20130FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20131FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20132FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20133FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20134FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20135FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20136FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20137FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20138FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D20139FA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2013AFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D2013BFA00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        mApdu.add("F0D4058000");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        mApdu.add("F0DE000001");
        return mApdu.toArray(new String[0]);
    }
}
