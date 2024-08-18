package com.fmsh.einkesl.tools;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;

import com.bumptech.glide.util.Util;
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
                switch (bundle.getInt("position")) {
                    case 0:
                        getDeviceInfo(isoDep);
                        break;
                    case 1:
                        if(App.getDeviceInfo().getPin()){
                            if(verifyPinCode(isoDep,bundle.getString("pin"))){
                                mBmpPath = bundle.getString("path");
                                handlerImage(isoDep,bundle.getBoolean("isLvl"));
                            }
                        }else {
                            mBmpPath = bundle.getString("path");
                            handlerImage(isoDep,bundle.getBoolean("isLvl"));
                        }
                        break;
                    case 2:
                        sendApdu(isoDep,bundle.getString("apdu"));
                        break;
                    case 3:
                        updatePin(isoDep,bundle.getString("oldPin"),bundle.getString("pin"));
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
    private static void sendApdu(IsoDep isoDep,String apdu) throws IOException {
        byte[] bytes = isoDep.transceive(FMUtil.hexToByte(apdu));
        IUtils.sendMessage(FMUtil.byteToHex(bytes),0);
    }

    private static void handlerImage(IsoDep isoDep,boolean isLvl) throws IOException {
        byte[] result = isoDep.transceive(FMUtil.hexToByte("00D1000000"));
        DeviceInfo deviceInfo = IUtils.loadDeviceInfo(ActivityUtils.instance.getCurrentActivity(), FMUtil.byteToHex(result));
        boolean bmpFormat = BmpUtils.GetBmpFormat(mBmpPath, deviceInfo);
        if (bmpFormat) {
            if (App.getDeviceInfo().getColorCount() == 2) {
                writeBlackWhiteScreen(isoDep);
            } else {
                write24ColorScreen(isoDep,isLvl);
            }
        } else {
            UIUtils.sendMessage(UIUtils.getString(R.string.bmp_error), -1, App.getHandler());
        }
    }

    /**
     * 黑白图片刷屏
     * @param isoDep
     */
    private  static void writeBlackWhiteScreen(IsoDep isoDep){
        byte[]  bmpdata = ImageUtils.ReadBmp8File(mBmpPath);
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
        } else {
            result = ImageUtils.horizontalScanning(result);
        }
        // 行数等于总的字节数 / 每次发送的字节数250 / 8 向上取整
        int rowCount = (int) Math.ceil(App.getDeviceInfo().getWidth() * App.getDeviceInfo().getHeight() / 250 / 8.0);
       if(! WriteBmpdataEx(isoDep,result,rowCount,0)){
           return;
       }
        RefreshScreenAfterWriteBmpdataex(isoDep,0);
    }

    private static void write24ColorScreen(IsoDep isoDep,boolean isLvl) {
        byte[]  bmpdata = ImageUtils.ReadBmp24File(mBmpPath,isLvl);
        if (bmpdata == null) {
            UIUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error), -1, App.getHandler());
            return;
        }
        int width =App.getDeviceInfo().getWidth();
        int height = App.getDeviceInfo().getHeight();
        byte[] data1 = new byte[width * height];
        byte[] data2 = new byte[width * height];
        ImageUtils.getColorDataBmp24(bmpdata, data1, data2);
        byte[] result = null;
        byte[] result1 = null;
        if(App.getDeviceInfo().getRefreshScan() == 0){
            result = ImageUtils.Color24VerticalScanning(data1);
            result1 = ImageUtils.Color24VerticalScanning(data2);
        }else {
            result = ImageUtils.horizontalScanning(data1);
            result1 =ImageUtils.horizontalScanning(data1);
        }
        // 行数等于总的字节数 / 每次发送的字节数250 / 8 向上取整
        int rowCount = (int) Math.ceil(width * height / 250 / 8.0);


        if (!WriteBmpdataEx(isoDep,result, rowCount,0)) {
            return;
        }

        if (!WriteBmpdataEx(isoDep,result1, rowCount,1)) {
            return;
        }
        RefreshScreenAfterWriteBmpdataex(isoDep,0);

    }

    /**
     * @param result
     * @param rowCount
     * @return
     */
    private  static boolean WriteBmpdataEx(IsoDep isoDep, byte[] result, int rowCount, int ScreenIndex) {
        boolean isCompress = App.getDeviceInfo().isCompress();
        int m_bmpWidth = App.getDeviceInfo().getWidth();
        int m_bmpHeight = App.getDeviceInfo().getHeight();
        if (isCompress) {
            try {
                LogUtil.d(FMUtil.byteToHex(result));
                int p = m_bmpHeight % 8;
                if (p != 0) {
                    m_bmpHeight = (8 - p) + m_bmpHeight;
                }
                int size = m_bmpWidth * m_bmpHeight / 8;
                byte[] bytes = new byte[size];
                System.arraycopy(result, 0, bytes, 0, bytes.length);
                result = bytes;
                LogUtil.d(FMUtil.byteToHex(result));
                String compress = CompressUtils.compress(isoDep, result, ScreenIndex);
                if ("9000".equals(compress)) {
                    return true;
                } else {
                    UIUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error), -1, App.getHandler());
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                IUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error),-1);
                return false;
            }
            
        } else {
            if(result.length < rowCount*0xFA){
                byte[] temp = new byte[rowCount*0xFA];
                System.arraycopy(result,0,temp,0,result.length);
                result =temp;
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
                    byte[] datasw = isoDep.transceive(apdu);

                    String strResponse = FMUtil.byteToHex(datasw);
                    if (!strResponse.substring(strResponse.length() - 4).equals("9000")) {
                        IUtils.sendMessage(UIUtils.getString(R.string.hint_error),-1);
                        return false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    IUtils.sendMessage(UIUtils.getString(R.string.hint_error),-1);
                    return false;
                }
            }
        }
        return true;
    }

    private static void RefreshScreenAfterWriteBmpdataex(IsoDep isoDep, int ScreenIndex) {

        byte[] refreshcmd = new byte[0x5];
        refreshcmd[0] = (byte) 0xF0;
        refreshcmd[1] = (byte) 0xd4;
        refreshcmd[2] = (byte) 0x05;
        refreshcmd[3] = (byte) (ScreenIndex | (byte) 0x80);
        refreshcmd[4] = (byte) 0x00;
        try {
            byte[] datasw = isoDep.transceive(refreshcmd);
            String strResponse = FMUtil.byteToHex(datasw);
            LogUtil.d(strResponse);

            if (strResponse.equals("698A")) {
                String strtmp = ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error);
                IUtils.sendMessage(strtmp,-1);
            } else if (strResponse.equals("6986") || "68C6".equals(strResponse)) {
                String strtmp = ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.string_res_37);
                IUtils.sendMessage(strtmp,-1);
            } else if (strResponse.equals("019000")) {
                //有源
                IUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.text_success),0);
            } else if (strResponse.equals("009000") || strResponse.equals("9000")) {
                //无源
                GetRefreshResult(isoDep,ScreenIndex);
            } else {
                refreshcmd[3] = (byte) ScreenIndex;
                datasw = isoDep.transceive(refreshcmd);
                strResponse = FMUtil.byteToHex(datasw);
                if (!strResponse.equals("9000")) {
                    // RevEdit.setText("刷屏时发生错误，错误码:" + strResponse);
                    String strtmp = ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.string_res_16);
                    IUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error),-1);
                }else {
                    IUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.text_success),0);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            IUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error),-1);
        }

    }


    private static void GetRefreshResult(IsoDep isoDep,int ScreenIndex) {
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
                datasw = isoDep.transceive(refreshcmd);
                strResponse = FMUtil.byteToHex(datasw);
                Log.d("<-", strResponse);
                if (strResponse.equals("009000")) {
                    IUtils.sendMessage(ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.text_success),0);
                    return;
                } else if (strResponse.equals("019000")) {
                    Thread.sleep(100); //还需要等待
                } else if (strResponse.equals("698A")) {
                    String strtmp = ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error);
                    m_strLastErrorMsg = strtmp + strResponse;
                    IUtils.sendMessage(m_strLastErrorMsg,-1);
                    return;
                } else if (strResponse.equals("6986") || "68C6".equals(strResponse)) {
                    String strtmp = ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.string_res_37);
                    IUtils.sendMessage(strtmp,-1);
                    return;
                } else {
                    String strtmp = ActivityUtils.instance.getCurrentActivity().getResources().getString(R.string.hint_error);
                    m_strLastErrorMsg = strtmp + strResponse;
                    IUtils.sendMessage(strtmp,-1);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            IUtils.sendMessage(UIUtils.getString(R.string.hint_error),-1);
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
        byte[] transceive = isoDep.transceive(FMUtil.hexToByte("F0D80000050000000004"));
        String toHex = FMUtil.byteToHex(transceive);


        UIUtils.sendMessage(FMUtil.byteToHex(result)+toHex.substring(toHex.length()-4), 0, App.getHandler());
    }

    private static void updatePin(IsoDep isodep,String oldPin,String pin){


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
            BroadcastManager.getInstance(ActivityUtils.instance.getCurrentActivity()).sendBroadcast("pin",toHex);
        }catch (Exception e){
            e.printStackTrace();
            BroadcastManager.getInstance(ActivityUtils.instance.getCurrentActivity()).sendBroadcast("pin",e.getMessage());
        }


    }

    private static boolean verifyPinCode(IsoDep isoDep,String pin) throws IOException {
        byte[] transceive = isoDep.transceive(FMUtil.hexToByte(String.format("00200001%02x%s", pin.length() / 2, pin)));
        String result = FMUtil.byteToHex(transceive);
        LogUtil.d(transceive);
        if("9000".equals(result)){
            return true;
        }else {
            IUtils.sendMessage(UIUtils.getString(R.string.string_res_5)+"("+result+")",-1);
            return false;
        }

    }


}
