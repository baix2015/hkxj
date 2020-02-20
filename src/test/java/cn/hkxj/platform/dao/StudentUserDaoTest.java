package cn.hkxj.platform.dao;

import cn.hkxj.platform.PlatformApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PlatformApplication.class)
@Slf4j
public class StudentUserDaoTest {
    @Resource
    private StudentUserDao studentUserDao;

    @Test
    public void isStudentAccountExistsTest(){
        System.out.println(studentUserDao.isStudentAccountExists(2017025151));
    }

}
