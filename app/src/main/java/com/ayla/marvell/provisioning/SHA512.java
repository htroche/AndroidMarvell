package com.ayla.marvell.provisioning;

import java.security.MessageDigest;

public class SHA512 {

    public static byte[] encrypt(String message) throws Exception {
        MessageDigest messageDigest = null;
        messageDigest = MessageDigest.getInstance("SHA-512");
        messageDigest.update(message.getBytes("UTF-8"));
        return messageDigest.digest();
    }

    public static byte[] encrypt(byte[] message) throws Exception {
        MessageDigest messageDigest = null;
        messageDigest = MessageDigest.getInstance("SHA-512");
        messageDigest.update(message);
        return messageDigest.digest();
    }

}
