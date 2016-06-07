package com.ayla.marvell.provisioning;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by hugotroche on 5/24/16.
 */
public class AES {

    public static byte[] encrypt(byte[] buffer, byte[] ivData, byte[] key) throws GeneralSecurityException
    {
        SecretKey secret = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");


        cipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(ivData));
        byte[] ciphertext = cipher.doFinal(buffer);

        return ciphertext;
    }

    public static byte[] decrypt(byte[] buffer, byte[] ivData, byte[] key) throws GeneralSecurityException
    {
        SecretKey secret = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");

        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivData));
        byte[] clear = cipher.doFinal(buffer);


        return clear;
    }

}

