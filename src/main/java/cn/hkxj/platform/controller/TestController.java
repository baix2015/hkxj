package cn.hkxj.platform.controller;

import cn.hkxj.platform.MDCThreadPool;
import cn.hkxj.platform.dao.StudentDao;
import cn.hkxj.platform.dao.StudentUserDao;
import cn.hkxj.platform.exceptions.PasswordUnCorrectException;
import cn.hkxj.platform.exceptions.UrpException;
import cn.hkxj.platform.pojo.Student;
import cn.hkxj.platform.pojo.StudentUser;
import cn.hkxj.platform.service.wechat.StudentBindService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class TestController {

    @Resource
    private StudentDao studentDao;
    @Resource
    private StudentUserDao studentUserDao;
    @Resource
    private StudentBindService studentBindService;

    @RequestMapping("tests")
    public String test(String echostr){
        return echostr;
    }


    @RequestMapping("studentUser")

    public String test2(){
        ArrayList<Student> studentList = Lists.newArrayList(studentDao.selectStudentByAccount(2014025838));
        //查询数据库所有的班级信息，并存入Map集合
        ExecutorService pool = new MDCThreadPool(4, 4,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> new Thread(r, "student"));

        Vector<String> vector = new Vector<>();

        for (Student student : studentList) {
            pool.submit(() -> {
                try {
                    StudentUser userInfo = studentBindService.getStudentUserInfo(student.getAccount().toString(), student.getPassword());
                    userInfo.setGmtCreate(student.getGmtCreate());
                    userInfo.setGmtModified(student.getGmtModified());

                    System.out.println(userInfo);
                    studentUserDao.insertStudentSelective(userInfo);

                }catch (Exception e){
                    if( (e instanceof PasswordUnCorrectException) || (e instanceof UrpException)){
                        return;
                    }
                    log.error("account {} fetch error {}", student.getAccount(), e.getMessage());
                    vector.add(student.getAccount().toString());
                }

            });

        }

        return  "ok";
    }
}
