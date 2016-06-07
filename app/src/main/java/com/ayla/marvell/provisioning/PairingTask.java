package com.ayla.marvell.provisioning;

import android.os.AsyncTask;
import android.os.Handler;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;

public class PairingTask extends AsyncTask<String, String, String> {

    private String networkName;
    private String networkPassword;
    private int networkType;
    private String devicePin;
    private String sessionID;
    private String devicePublicKey;
    private byte[] privateKey;
    private byte[] publicKey;
    private byte[] agreementKey;

    public ProvisioningHandler handler;


    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getNetworkPassword() {
        return networkPassword;
    }

    public void setNetworkPassword(String networkPassword) {
        this.networkPassword = networkPassword;
    }

    public int getNetworkType() {
        return networkType;
    }

    public void setNetworkType(int networkType) {
        this.networkType = networkType;
    }

    public String getDevicePin() {
        return devicePin;
    }

    public void setDevicePin(String devicePin) {
        this.devicePin = devicePin;
    }

    @Override
    protected String doInBackground(String... uri) {
        String responseString = null;
        try {
            responseString = this.setupSecureSession();

        } catch (Exception e) {
            e.printStackTrace();
            this.handler.error(e.getMessage());
        }
        return responseString;
    }

    private String setupSecureSession() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        this.privateKey = new byte[32];
        this.publicKey = new byte[32];
        this.agreementKey = new byte[32];
        byte[] randomGenBytes = new byte[32];
        new Random().nextBytes(randomGenBytes);
        Curve25519.keygen(publicKey, privateKey, randomGenBytes);
        this.agreementKey = randomGenBytes;
        byte[] randomSignatureBytes = new byte[64];
        new Random().nextBytes(randomSignatureBytes);
        String randomKey = this.bytesToHex(randomSignatureBytes);
        System.out.println("Marvell randomKey length: " + randomKey.length());
        JSONObject requestData = new JSONObject();
        requestData.put("client_pub_key", this.bytesToHex(publicKey));
        requestData.put("random_sig", randomKey);
        HttpPost post = new HttpPost("http://192.168.10.1/prov/secure-session");
        StringEntity se = new StringEntity(requestData.toString());
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded"));
        post.setEntity(se);
        response = httpclient.execute(post);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            responseString = out.toString();
            JSONObject deviceResponse = new JSONObject(responseString);
            if (deviceResponse.has("session_id")) {
                this.sessionID = deviceResponse.getString("session_id");
                this.devicePublicKey = deviceResponse.getString("device_pub_key");
                System.out.println("Marvell sessionID: " + this.sessionID);
                System.out.println("Marvell devicePublicKey: " + this.devicePublicKey);
                //System.out.println("Marvell privateKey agreement: " + android.util.Base64.encodeToString(this.agreementKey, Base64.DEFAULT));
                System.out.println("Marvell decrypted setup session: " + this.decryptJSONData(deviceResponse));
                //this.handler.error("Could not establish session");
                this.configureNetwork();
            } else {
                System.out.println("Marvell error setup keys");
                this.handler.error("Could not establish session");
            }
            System.out.println(responseString);
            out.close();
        } else {
            //Closes the connection.
            response.getEntity().getContent().close();
            System.out.println("Call did not work" + statusLine.getStatusCode());
            //throw new IOException(statusLine.getReasonPhrase());
        }
        return responseString;
    }

    private void configureNetwork() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;

        JSONObject clearJSON = new JSONObject();
        clearJSON.put("ssid", this.networkName);
        clearJSON.put("key", this.networkPassword);
        clearJSON.put("security", this.networkType);
        JSONObject requestData = this.getEncryptedJSON(clearJSON);
        HttpPost post = new HttpPost("http://192.168.10.1/prov/network?session_id=" + this.sessionID);
        System.out.println("Marvell network post data: " + clearJSON);
        System.out.println("Marvell network post data encrypted: " + requestData.toString());
        StringEntity se = new StringEntity(requestData.toString());
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded"));
        post.setEntity(se);
        response = httpclient.execute(post);
        StatusLine statusLine = response.getStatusLine();
        System.out.println("Marvell network configured: " + response.getStatusLine());
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        response.getEntity().writeTo(out1);
        responseString = out1.toString();
        //I can't get decrypt the result of this call or have a sucessful checksum here
        System.out.println("Marvell network out of check: " + responseString);
        System.out.println("Marvell network response decrypted: " + this.decryptJSONData(new JSONObject(responseString)));
        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            responseString = out.toString();
            JSONObject deviceResponse = new JSONObject(responseString);

            System.out.println("Hunter network: " + responseString);
            out.close();
            this.checkAccessoryStatus();
        } else {
            //Closes the connection.
            //response.getEntity().getContent().close();
            System.out.println("Marvell network call did not work" + statusLine.getStatusCode());
            this.handler.error("Could not configure network");
            //throw new IOException(statusLine.getReasonPhrase());
        }
    }

    private void checkAccessoryStatus() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;

        HttpGet get = new HttpGet("http://192.168.10.1/prov/net-info?session_id=" + this.sessionID);
        response = httpclient.execute(get);
        StatusLine statusLine = response.getStatusLine();
        System.out.println("Marvell accesory status: " + response.getStatusLine());
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        response.getEntity().writeTo(out1);
        responseString = out1.toString();
        System.out.println("Marvell accessory status data: " + responseString);
        System.out.println("Marvell network response decrypted: " + this.decryptJSONData(new JSONObject(responseString)));

        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            responseString = out.toString();
            System.out.println("Marvell accessory status data: " + responseString);
            JSONObject deviceResponse = new JSONObject(responseString);
            if (deviceResponse.getInt("status") == 0) {
                this.handler.error(deviceResponse.getString("failure"));
            } else if (deviceResponse.getInt("status") == 2) {
                this.confirmProvisioning();
            } else if (deviceResponse.getInt("status") == 1 && deviceResponse.getInt("failure_cnt") < 3) { //we retry 3 times
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            checkAccessoryStatus();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, 1000);
            } else {
                this.handler.error("Could not configure network");
            }

            this.handler.success(responseString);
            out.close();
        } else {
            //Closes the connection.
            //response.getEntity().getContent().close();
            System.out.println("Marvell network status call did not work" + statusLine.getStatusCode());
            this.handler.error("Could not configure network");
            //throw new IOException(statusLine.getReasonPhrase());
        }
    }

    private void confirmProvisioning() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        JSONObject clearJSON = new JSONObject();
        clearJSON.put("prov_client_ack", 1);
        HttpPost post = new HttpPost("http://192.168.10.1/prov/net-info?session_id=" + this.sessionID);
        StringEntity se = new StringEntity(clearJSON.toString());
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded"));
        post.setEntity(se);
        response = httpclient.execute(post);

        StatusLine statusLine = response.getStatusLine();

        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            responseString = out.toString();
            this.handler.success(responseString);
            out.close();
        } else {
            //Closes the connection.
            //response.getEntity().getContent().close();
            System.out.println("Marvell confirm provisining call did not work" + statusLine.getStatusCode());
            this.handler.error("Could not configure network");
            //throw new IOException(statusLine.getReasonPhrase());
        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        //Do anything with response..
    }

    private JSONObject getEncryptedJSON(JSONObject clear) throws Exception {
        byte[] randomGenBytes = new byte[16];
        new Random().nextBytes(randomGenBytes);
        JSONObject requestData = new JSONObject();
        requestData.put("iv", this.bytesToHex(randomGenBytes));
        requestData.put("data", this.getEncryptedJSONData(clear, randomGenBytes));
        requestData.put("checksum", this.bytesToHex(SHA512.encrypt(clear.toString())));
        return requestData;
    }

    private String getEncryptedJSONData(JSONObject clear, byte[] iv) throws Exception {
        /*
        X = curve25519(client_priv_key, device_pub_key)
        Y = first 16 bytes of SHA512(prov_pin)
        shared_secret = X XOR Y
        */
        byte[] curve = new byte[32];
        Curve25519.curve(curve, this.agreementKey, this.hextoBytes(this.devicePublicKey));
        byte[] pin = SHA512.encrypt(this.devicePin);
        System.out.println("Hunter device pin: " + pin.toString());
        byte[] croppedPin = java.util.Arrays.copyOfRange(pin, 0, 16);
        byte[] hashedCurve = SHA512.encrypt(curve);
        byte[] sharedSecret = new byte[16];
        for (int n = 0; n < 16; n++) {
            sharedSecret[n] = (byte) ((int) croppedPin[n] ^ (int) hashedCurve[n]);
        }
        byte[] encrypted = AES.encrypt(clear.toString().getBytes(), iv, sharedSecret);
        return this.bytesToHex(encrypted);
    }

    private String decryptJSONData(JSONObject encrypted) throws Exception {
        byte[] curve = new byte[32];
        Curve25519.curve(curve, this.agreementKey, this.hextoBytes(this.devicePublicKey));
        System.out.println("Hunter devicePublicKey byte array size: " + this.hextoBytes(this.devicePublicKey).length);
        byte[] pin = SHA512.encrypt(this.devicePin);
        System.out.println("Hunter device pin byte array size: " + pin.length);
        byte[] croppedPin = java.util.Arrays.copyOfRange(pin, 0, 16);
        byte[] hashedCurve = SHA512.encrypt(curve);
        System.out.println("Hunter hashed curve byte array size: " + hashedCurve.length);
        byte[] sharedSecret = new byte[16];
        for (int n = 0; n < 16; n++) {
            sharedSecret[n] = (byte) ((int) croppedPin[n] ^ (int) hashedCurve[n]);
        }
        System.out.println("Hunter data byte array size: " + this.hextoBytes(encrypted.getString("data")).length);
        byte[] decrypted = AES.decrypt(this.hextoBytes(encrypted.getString("data")), this.hextoBytes(encrypted.getString("iv")), sharedSecret);
        String result = new String(decrypted, "UTF-8");
        return result;
    }

    final private char[] hexArray = "0123456789ABCDEF".toCharArray();

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private byte[] hextoBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public interface ProvisioningHandler {
        public void error(String message);

        public void success(String message);
    }

}

