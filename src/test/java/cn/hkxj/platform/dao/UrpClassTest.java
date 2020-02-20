package cn.hkxj.platform.dao;

import cn.hkxj.platform.PlatformApplication;
import cn.hkxj.platform.mapper.UrpClassMapper;
import cn.hkxj.platform.pojo.UrpClass;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PlatformApplication.class)
@Slf4j
public class UrpClassTest {
    @Resource
    private UrpClassDao urpClassDao;

    @Test
    public void findByClassNameTest(){
        UrpClass urpClass = urpClassDao.selectByName("地质20-1");
        System.out.println(urpClass.getClassNum());
    }
}
