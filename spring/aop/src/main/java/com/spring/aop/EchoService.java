/*
 * Ant Group
 * Copyright (c) 2004-2021 All Rights Reserved.
 */
package com.spring.aop;

import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;

/**
 * @author shenlong
 * @version EchoService.java, v 0.1 2021年07月02日 3:37 下午 shenlong
 */
public class EchoService {

    public String hello(String message){
        return message;
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        String gbk = "iteye问答频道编码转换问题";

        String iso = new String(gbk.getBytes("UTF-8"),"ISO-8859-1");

        System.out.println(iso);

        String utf8 = new String(iso.getBytes("ISO-8859-1"),"UTF-8");
        System.out.println(utf8);

        System.out.println(getUTF8StringFromGBKString(gbk));
        System.out.println(StringUtils.equals(null, "PC"));
    }

    public static String getUTF8StringFromGBKString(String gbkStr) {
        try {
            return new String(getUTF8BytesFromGBKString(gbkStr), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new InternalError();
        }
    }

    public static byte[] getUTF8BytesFromGBKString(String gbkStr) {
        int n = gbkStr.length();
        byte[] utfBytes = new byte[3 * n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            int m = gbkStr.charAt(i);
            if (m < 128 && m >= 0) {
                utfBytes[k++] = (byte) m;
                continue;
            }
            utfBytes[k++] = (byte) (0xe0 | (m >> 12));
            utfBytes[k++] = (byte) (0x80 | ((m >> 6) & 0x3f));
            utfBytes[k++] = (byte) (0x80 | (m & 0x3f));
        }
        if (k < utfBytes.length) {
            byte[] tmp = new byte[k];
            System.arraycopy(utfBytes, 0, tmp, 0, k);
            return tmp;
        }
        return utfBytes;
    }
}