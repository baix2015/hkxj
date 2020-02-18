package cn.hkxj.platform.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DESUtil {
    @Value("${secret_salt}")
    private static String salt;

    /**
     * 获取加密后的密文密码
     * @param account 学号
     * @param password 密码
     * @return 返回加密后的密文密码
     * @throws Exception
     */
    public static String DESEncrypt(String account, String password) throws Exception {
        if(StringUtils.isEmpty(account) || StringUtils.isEmpty(password)){
            throw new RuntimeException("DES加密时用户名或密码为空");
        }
        else{
            try {
                SecretKey secretKey = generateKey(account+salt);
                byte[] enCodeFormat = secretKey.getEncoded();
                SecretKeySpec key = new SecretKeySpec(enCodeFormat, "DES");
                Cipher cipher = Cipher.getInstance("DES");// 创建密码器
                byte[] byteContent = password.getBytes(StandardCharsets.UTF_8);
                cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化
                byte[] result = Base64.getEncoder().encode(cipher.doFinal(byteContent));
                return new String(result);
            }
            catch (Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 获取解密后的明文密码
     * @param account 学号
     * @param password 加密后的密码文件（一般从数据库读取）
     * @return 返回解密后的明文密码
     * @throws Exception
     */
    public static String DESDecrypt(String account, String password) throws Exception {
        if(StringUtils.isEmpty(account) || StringUtils.isEmpty(password)) {
            throw new RuntimeException("DES解密时用户名或密码为空");
        }
        else{
            try {
                byte[] contentbyte = Base64.getDecoder().decode(password);
                SecretKey secretKey = generateKey(account+salt);
                byte[] enCodeFormat = secretKey.getEncoded();
                SecretKeySpec key = new SecretKeySpec(enCodeFormat, "DES");
                Cipher cipher = Cipher.getInstance("DES");// 创建密码器
                cipher.init(Cipher.DECRYPT_MODE, key);// 初始化
                byte[] result = cipher.doFinal(contentbyte);
                return new String(result);
            }
            catch (Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 获取加密用的salt
     * @return
     */
    private static String getSalt(){
        return salt;
    }

    /**
     * 传入salt生成SecretKey
     * @param salt 学号+公共salt拼接后的salt
     * @return
     */
    private static SecretKey generateKey(String salt){
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            DESKeySpec keySpec = new DESKeySpec(salt.getBytes(StandardCharsets.UTF_8));
            SecretKey secretKey = keyFactory.generateSecret(keySpec);
            return keyFactory.generateSecret(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
