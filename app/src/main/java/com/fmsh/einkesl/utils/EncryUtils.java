package com.fmsh.einkesl.utils;


import com.bumptech.glide.util.Util;
import com.fmsh.base.utils.FMUtil;
import com.fmsh.base.utils.LogUtil;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * Created by ly on 2017/3/21.
 * 加解密工具类 AES DES DES3
 */
public class EncryUtils {

    /**
     * 生成key
     *
     * @param password
     * @return
     * @throws Exception
     */
    private static Key generateKey(String password) throws Exception {
        DESKeySpec dks = new DESKeySpec(FMUtil.hexToByte(password));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        return keyFactory.generateSecret(dks);
    }

    /**
     * DES加密字符串
     *
     * @param password 加密密码，长度不能够小于8位
     * @param data 待加密字符串
     * @return 加密后内容
     */
    public static String encryptDES(String ivStr, String password, String data) {
        if (password== null || password.length() < 8) {
            throw new RuntimeException("加密失败，key不能小于8位");
        }
        LogUtil.d(ivStr);
        LogUtil.d(password);
        LogUtil.d(data);

        if (data == null)
            return null;
        try {
            Key secretKey = generateKey(password);
            Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
            IvParameterSpec iv = new IvParameterSpec(FMUtil.hexToByte(ivStr));
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            byte[] bytes = cipher.doFinal(FMUtil.hexToByte(data));

            //JDK1.8及以上可直接使用Base64，JDK1.7及以下可以使用BASE64Encoder
            //Android平台可以使用android.util.Base64
            return FMUtil.byteToHex(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            return data;
        }
    }

    /**
     * DES解密字符串
     *
     * @param password 解密密码，长度不能够小于8位
     * @param data 待解密字符串
     * @return 解密后内容
     */
    public static String decryptDES(String strIV, String password, String data) {
        if (password== null || password.length() < 8) {
            throw new RuntimeException("加密失败，key不能小于8位");
        }
        if (data == null)
            return null;
        try {
            Key secretKey = generateKey(password);
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(strIV.getBytes("UTF-8"));
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            byte[] bytes = cipher.doFinal(data.getBytes("UTF-8"));
            return FMUtil.byteToHex(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return data;
        }
    }

}
