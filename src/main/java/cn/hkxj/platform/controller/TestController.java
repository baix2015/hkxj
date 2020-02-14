package cn.hkxj.platform.controller;

import cn.hkxj.platform.dao.ClassDao;
import cn.hkxj.platform.dao.GradeDao;
import cn.hkxj.platform.dao.StudentDao;
import cn.hkxj.platform.pojo.Grade;
import cn.hkxj.platform.pojo.Student;
import cn.hkxj.platform.pojo.WebResponse;
import cn.hkxj.platform.service.CourseTimeTableService;
import cn.hkxj.platform.service.NewGradeSearchService;
import cn.hkxj.platform.spider.NewUrpSpider;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

@RestController
@Slf4j
public class TestController {
    @Resource
    private CourseTimeTableService courseTimeTableService;
    @Resource
    private GradeDao gradeDao;
    @Resource
    private ClassDao classDao;
    @Resource
    private StudentDao studentDao;
    @Resource
    private NewGradeSearchService newGradeSearchService;


    @GetMapping(value = "/testhtml")
    public String courseTimeTable(){
        return "LoginWeb/test";
    }

    @RequestMapping("/testEverGrade")
    public WebResponse testGrade(@RequestParam("account") int account){

        return WebResponse.success(newGradeSearchService.getSchemeGrade(studentDao.selectStudentByAccount(account)));
    }


    @RequestMapping("tests")
    public String test(String echostr){
        System.out.println("echostr:----------" + echostr);
        return echostr;
    }


    @RequestMapping("/exam")
    public String exam(){
        NewUrpSpider spider = new NewUrpSpider("2017021517", "1");
        return "ok";
    }


    @RequestMapping("/log")
    public String testLog(){
        throw new NullPointerException();
    }

    @RequestMapping("/update")
    public String testGrade(){
        ArrayList<Integer> integers = Lists.newArrayList(2017022634, 2018020867, 2017020742, 2017026102, 2016020963, 2016020963,
                2016025330, 2018020760, 2018024539, 2017024189, 2017020988, 2017020993, 2017020630, 2017021063,
                2017020998, 2018022260, 2017021304, 2015028008, 2018026052);

        LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>(integers);

        Integer account;
        while ((account = queue.poll()) != null){
            System.out.println(account);
            try {
                Student student = studentDao.selectStudentByAccount(account);
                for (Grade grade : newGradeSearchService.getSchemeGradeFromSpider(student)) {
                    gradeDao.updateByUniqueIndex(grade);
                }
            }catch (Exception e){
                log.error("account {} error ", account, e);
                queue.add(account);
            }

        }
        return "ok";
    }




    public static void main(String[] args) throws IOException {



    }
}
