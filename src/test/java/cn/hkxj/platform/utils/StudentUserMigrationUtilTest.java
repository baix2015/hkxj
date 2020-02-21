package cn.hkxj.platform.utils;

import cn.hkxj.platform.MDCThreadPool;
import cn.hkxj.platform.PlatformApplication;
import cn.hkxj.platform.dao.StudentDao;
import cn.hkxj.platform.dao.StudentUserDao;
import cn.hkxj.platform.pojo.Student;
import cn.hkxj.platform.pojo.StudentUser;
import cn.hkxj.platform.service.NewUrpSpiderService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;
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
    private NewUrpSpiderService newUrpSpiderService;

    private int count=0;
    private int fail = 0;


    @Test
    public void studentMigration() throws Exception {
        List<Student> studentList = studentDao.selectAllStudent();
        //查询数据库所有的班级信息，并存入Map集合
        ExecutorService pool = new MDCThreadPool(4, 4,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> new Thread(r, "student"));

        LinkedBlockingQueue<Student> queue = new LinkedBlockingQueue<>(studentList);


        Student student;
        Vector<String> vector = new Vector<>();
        CountDownLatch latch = new CountDownLatch(queue.size());
        while ((student = queue.poll()) != null) {
            Student finalStudent = student;
            pool.submit(() -> {
                try {
                    StudentUser userInfo = newUrpSpiderService.getStudentUserInfo(finalStudent.getAccount().toString(), finalStudent.getPassword());
                    userInfo.setGmtCreate(finalStudent.getGmtCreate());
                    userInfo.setGmtModified(finalStudent.getGmtModified());

                    System.out.println(userInfo);
                    studentUserDao.insertStudentSelective(userInfo);
                }catch (Exception e){
                    vector.add(finalStudent.getAccount().toString());
                }finally {
                    latch.countDown();
                }

            });

        }

        latch.await();
        System.out.println(vector);
    }



}
