package cn.hkxj.platform.utils;

import cn.hkxj.platform.PlatformApplication;
import cn.hkxj.platform.dao.StudentDao;
import cn.hkxj.platform.dao.StudentUserDao;
import cn.hkxj.platform.dao.UrpClassDao;
import cn.hkxj.platform.pojo.Student;
import cn.hkxj.platform.pojo.StudentUser;
import cn.hkxj.platform.pojo.UrpClass;
import cn.hkxj.platform.service.NewUrpSpiderService;
import cn.hkxj.platform.spider.NewUrpSpider;
import cn.hkxj.platform.spider.model.UrpStudentInfo;
import cn.hkxj.platform.spider.newmodel.SearchResult;
import cn.hkxj.platform.spider.newmodel.searchclass.ClassInfoSearchResult;
import cn.hkxj.platform.spider.newmodel.searchclass.SearchClassInfoPost;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.hkxj.platform.utils.DESUtil.encrypt;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PlatformApplication.class)
@Component
public class StudentUserMigrationUtilTest{

    @Resource
    private StudentDao studentDao;
    @Resource
    private StudentUserDao studentUserDao;
    @Resource
    private UrpClassDao urpClassDao;
    @Resource
    private NewUrpSpiderService newUrpSpiderService;

    private int count=0;
    private int fail = 0;


    @Test
    public void studentMigration() throws Exception {
        List<Student> studentList = studentDao.selectAllStudent();
        //查询数据库所有的班级信息，并存入Map集合
        Map<String,UrpClass> urpClassMap = new HashMap<>();
        List<UrpClass> urpClasses = urpClassDao.selectAllClass();
        for (UrpClass urpClass : urpClasses) {
            urpClassMap.put(urpClass.getClassName().trim(),urpClass);
        }

        for (Student student : studentList) {
            StudentUser studentUser = new StudentUser();
            //首先将student原有字段赋给studentUser
            studentUser.setAccount(student.getAccount());
            studentUser.setPassword(encrypt(student.getAccount().toString(), student.getPassword()));
            studentUser.setName(student.getName());
            studentUser.setSex(student.getSex());
            studentUser.setEthnic(student.getEthnic());
            studentUser.setIsCorrect(student.getIsCorrect());
            studentUser.setGmtCreate(student.getGmtCreate());
            studentUser.setGmtModified(student.getGmtModified());

            //通过爬虫查询班级号、学院、专业、班级
            try {
                NewUrpSpider newUrpSpider = new NewUrpSpider(student.getAccount().toString(), student.getPassword());
                UrpStudentInfo urpStudentInfo = newUrpSpider.getStudentInfo();

                studentUser.setAcademyName(urpStudentInfo.getAcademy());
                studentUser.setSubjectName(urpStudentInfo.getMajor());
                studentUser.setClassName(urpStudentInfo.getClassname());
                studentUser.setUrpclassNum(getUrpClassNum(urpStudentInfo.getClassname()));

            } catch (Exception e) {
                studentUser.setUrpclassNum(0000000000);
                studentUser.setAcademyName(null);
                studentUser.setSubjectName(null);
                studentUser.setClassName(null);
            }
            //尝试写入数据库
            try {
                if (studentUserDao.isStudentAccountExists(student.getAccount())) {
                    //写入成功后尝试删除原表数据，此处暂时不删除，避免出错
                    count += 1;
                } else {
                    studentUserDao.insertStudentSelective(studentUser);
                    count += 1;
                }
            } catch (Exception e) {
                fail += 1;
                count += 1;
                throw new RuntimeException("写入数据时失败:" + studentUser.toString());
            }
        }
        System.out.println("完成，共影响了" + count + "条数据," + fail + "条失败");
    }

    public Integer getUrpClassNum(String className) {
        //先查询数据库，数据库有的话就不放爬虫
        try {
            return Integer.parseInt(urpClassDao.selectByName(className).getClassNum());
        } finally {
            //数据库没有内容，构建表单查询urp，然后对应的值写入数据库
            SearchClassInfoPost post = new SearchClassInfoPost();
            Pattern pattern = Pattern.compile("[0-9]{2}");
            Matcher matcher = pattern.matcher(className);
            if (matcher.find()) {
                post.setYearNum("20" + matcher.group());
            }
            List<SearchResult<ClassInfoSearchResult>> classInfoSearchResult = newUrpSpiderService.getClassInfoSearchResult(post);
            for (SearchResult<ClassInfoSearchResult> classInfoSearchResultSearchResult : classInfoSearchResult) {
                List<ClassInfoSearchResult> records = classInfoSearchResultSearchResult.getRecords();
                for (ClassInfoSearchResult record : records) {
                    try {
                        //写回数据库
                        UrpClass urpClass = new UrpClass();
                        urpClass.setAcademyName(record.getDepartmentName());
                        urpClass.setAcademyNum(record.getDepartmentNum());
                        urpClass.setAdmissionGrade(record.getAdmissionGrade());
                        urpClass.setClassName(record.getClassName());
                        urpClass.setSubjectName(record.getSubjectName());
                        urpClass.setSubjectNum(record.getSubjectNum());
                        urpClass.setClassNum(record.getId().getClassNum());
                    } finally {
                        //传给上层调用
                        className.trim().equals(record.getClassName().trim());
                        String[] str = String.valueOf(record.getId()).split("Id");
                        String s = str[1].split(",")[0].split("=")[1];
                        return Integer.parseInt(s);
                    }
                }
            }
        }
    }


}
