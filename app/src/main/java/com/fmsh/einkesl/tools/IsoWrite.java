package com.fmsh.einkesl.tools;

import android.nfc.tech.IsoDep;

import com.fmsh.base.utils.FMUtil;

public class IsoWrite {
    public static byte[] write(IsoDep isodep ,byte[] bytes){

        byte[] transceive = isodep.transceive(FMUtil.hexToByte("F0D8000005000000000E"));
        String toHex = FMUtil.byteToHex(transceive);
        return transceive;

    }
}
