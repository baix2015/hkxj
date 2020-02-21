package cn.hkxj.platform.utils;

import cn.hkxj.platform.MDCThreadPool;
import cn.hkxj.platform.PlatformApplication;
import cn.hkxj.platform.dao.StudentDao;
import cn.hkxj.platform.dao.StudentUserDao;
import cn.hkxj.platform.pojo.Student;
import cn.hkxj.platform.pojo.StudentUser;
import cn.hkxj.platform.service.wechat.StudentBindService;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PlatformApplication.class)
@Component
public class StudentUserMigrationUtilTest{

    @Resource
    private StudentDao studentDao;
    @Resource
    private StudentUserDao studentUserDao;
    @Resource
    private StudentBindService studentBindService;

    private int count=0;
    private int fail = 0;


    @Test
    public void studentMigration() throws Exception {
        ArrayList<Student> studentList = Lists.newArrayList(studentDao.selectStudentByAccount(2014025838));
        //查询数据库所有的班级信息，并存入Map集合
        ExecutorService pool = new MDCThreadPool(4, 4,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> new Thread(r, "student"));

        Vector<String> vector = new Vector<>();
        CountDownLatch latch = new CountDownLatch(studentList.size());

        for (Student student : studentList) {
            pool.submit(() -> {
                try {
                    StudentUser userInfo = studentBindService.getStudentUserInfo(student.getAccount().toString(), student.getPassword());
                    userInfo.setGmtCreate(student.getGmtCreate());
                    userInfo.setGmtModified(student.getGmtModified());

                    System.out.println(userInfo);
                    studentUserDao.insertStudentSelective(userInfo);
                }catch (Exception e){
                    e.printStackTrace();
                    vector.add(student.getAccount().toString());
                }finally {
                    latch.countDown();
                }

            });

        }

        latch.await();
        System.out.println(vector);
    }



}
