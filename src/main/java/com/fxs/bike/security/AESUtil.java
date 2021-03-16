package com.fxs.bike.security;


import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class AESUtil {

    public static final String KEY_ALGORITHM = "AES";
    public static final String KEY_ALGORITHM_MODE = "AES/CBC/PKCS5Padding";

    /**
     * AES对称加密
     * @param data
     * @param key key需要16位
     * @return
     */
    public static String encrypt(String data , String key) {
        try {
            SecretKeySpec spec = new SecretKeySpec(key.getBytes("UTF-8"),KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM_MODE);
            cipher.init(Cipher.ENCRYPT_MODE , spec,new IvParameterSpec(new byte[cipher.getBlockSize()]));
            byte[] bs = cipher.doFinal(data.getBytes("UTF-8"));
            //base64防止乱码,通过网络传输时可能出现问题,转码方便传输
            return Base64Util.encode(bs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  null;
    }

    /**
     * AES对称解密 key需要16位
     * @param data
     * @param key
     * @return
     */
    public static String decrypt(String data, String key) {
        try {
            SecretKeySpec spec = new SecretKeySpec(key.getBytes("UTF-8"), KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM_MODE);
            cipher.init(Cipher.DECRYPT_MODE , spec , new IvParameterSpec(new byte[cipher.getBlockSize()]));
            byte[] originBytes = Base64Util.decode(data);
            byte[] result = cipher.doFinal(originBytes);
            return new String(result,"UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  null;
    }

    public static void main(String[] args) throws Exception {
        /**AES对称加密数据**/
        String key = "0123456789asbced";//应该在客户端随机生成
        String data = "{'mobile':'15549053122','code':'8060','platform':'ios','channelId':12454348}";
        String enResult = encrypt(data, key);
        System.out.println(enResult);
//        String deResult = decrypt(enResult, key);
//        System.out.println(deResult);
        /**客户端RSA非对称加密AES密钥**/
        byte[] enKey = RSAUtil.encryptByPublicKey(key.getBytes(StandardCharsets.UTF_8), RSAUtil.PUBLIC_KEY);
        System.out.println(new String(enKey, "UTF-8"));
        String baseKey = Base64Util.encode(enKey);
        System.out.println(baseKey);
        /**服务端RSA解密AES的key**/
        byte[] deKey = RSAUtil.decryptByPrivateKey(Base64Util.decode(baseKey));
        String AESKey = new String(deKey, "UTF-8");
        System.out.println(AESKey);
        //解密数据
        String deResult = decrypt(enResult, AESKey);
        System.out.println(deResult);

    }
}
