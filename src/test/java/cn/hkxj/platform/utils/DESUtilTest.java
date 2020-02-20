package cn.hkxj.platform.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class DESUtilTest {
    private static final String salt = "FSOBQ4DE6UFHCPRP";

    public String DESEncrypt(String content) throws Exception {
        if(!(content == null)){
            try {
                SecretKey secretKey = GenerateKey(salt);
                byte[] enCodeFormat = secretKey.getEncoded();
                SecretKeySpec key = new SecretKeySpec(enCodeFormat, "DES");
                Cipher cipher = Cipher.getInstance("DES");// 创建密码器
                byte[] byteContent = content.getBytes("UTF-8");
                cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化
                byte[] result = Base64.getEncoder().encode(cipher.doFinal(byteContent));
                return new String(result);
            }
            catch (Exception e){

            }
            return null;
        }
        return null;
    }

    public String DESDecrypt(String content) throws Exception {
        if(!(content == null)){
            try {
                byte[] contentbyte = Base64.getDecoder().decode(content);
                SecretKey secretKey = GenerateKey(salt);
                byte[] enCodeFormat = secretKey.getEncoded();
                SecretKeySpec key = new SecretKeySpec(enCodeFormat, "DES");
                Cipher cipher = Cipher.getInstance("DES");// 创建密码器
                cipher.init(Cipher.DECRYPT_MODE, key);// 初始化
                byte[] result = cipher.doFinal(contentbyte);
                return new String(result);
            }
            catch (Exception e){

            }
            return null;
        }
        return null;
    }

    private SecretKey GenerateKey(String salt){
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            DESKeySpec keySpec = new DESKeySpec(salt.getBytes("utf-8"));
            SecretKey secretKey = keyFactory.generateSecret(keySpec);
            return keyFactory.generateSecret(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
