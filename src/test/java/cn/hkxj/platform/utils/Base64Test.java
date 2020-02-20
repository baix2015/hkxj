package cn.hkxj.platform.utils;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;



public class Base64Test {
    String s = "测试";

    @Test
    public void b64t(){
        byte[] contentbyte = Base64.encodeBase64(s.getBytes());
        System.out.println(new String(contentbyte));
    }



}
