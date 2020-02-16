package cn.hkxj.platform.utils;

import cn.hkxj.platform.MDCThreadPool;
import cn.hkxj.platform.PlatformApplication;
import cn.hkxj.platform.exceptions.UrpEvaluationException;
import cn.hkxj.platform.pojo.Student;
import cn.hkxj.platform.pojo.StudentUser;
import cn.hkxj.platform.pojo.StudentUserExample;
import cn.hkxj.platform.pojo.example.StudentExample;
import cn.hkxj.platform.mapper.StudentMapper;
import cn.hkxj.platform.mapper.StudentUserMapper;
import cn.hkxj.platform.service.NewUrpSpiderService;
import cn.hkxj.platform.service.UrpSearchService;
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
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.lucene.index.TwoPhaseCommitTool.execute;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PlatformApplication.class)
@Component
public class StudentUserMigrationUtilTest {

    @Resource
    private StudentMapper studentMapper;
    @Resource
    private StudentUserMapper studentUserMapper;
    @Resource
    private NewUrpSpiderService newUrpSpiderService;

    private int count=0;
    private int fail = 0;

    @Test
    public void studentMigration() throws Exception {
        List<Student> studentList = studentMapper.selectByExample(new StudentExample());

        for (Student student : studentList) {
            StudentUser studentUser = new StudentUser();
            studentUser.setAccount(student.getAccount());
            studentUser.setPassword(student.getPassword());
            System.out.println(studentUser.getAccount());
            System.out.println(studentUser.getPassword());
            //try {
                NewUrpSpider newUrpSpider = new NewUrpSpider(student.getAccount().toString(), student.getPassword());
                UrpStudentInfo urpStudentInfo = newUrpSpider.getStudentInfo();
                int i = 0;
                while (i<3 && (studentUser.getAcademyName() == null || studentUser.getSubjectName() == null || studentUser.getClassName() == null)) {
                    studentUser.setAcademyName(urpStudentInfo.getAcademy());
                    studentUser.setSubjectName(urpStudentInfo.getMajor());
                    studentUser.setClassName(urpStudentInfo.getClassname());
                    Integer urpClassNum = getUrpClassNum(urpStudentInfo.getClassname());
                    studentUser.setUrpclassNum(urpClassNum);
                    i+=1;
                }
                DESUtil desUtil = new DESUtil();
                studentUser.setPassword(desUtil.DESEncrypt(student.getAccount().toString(),student.getPassword()));
                studentUser.setName(student.getName());
                studentUser.setSex(student.getSex());
                studentUser.setEthnic(student.getEthnic());
                studentUser.setIsCorrect(student.getIsCorrect());
                studentUser.setGmtCreate(student.getGmtCreate());
                studentUser.setGmtModified(student.getGmtModified());
                System.out.println(studentUser);

            //}
            //catch (Exception e){
            //    System.out.println(e.getMessage());
            //}
//            catch (UrpEvaluationException e) {
//                //当用户未评教时就不再抓取信息，直接传入原表信息
//                DESUtil desUtil = new DESUtil();
//                studentUser.setPassword(desUtil.DESEncrypt(student.getAccount().toString(),student.getPassword()));
//                studentUser.setName(student.getName());
//                studentUser.setSex(student.getSex());
//                studentUser.setEthnic(student.getEthnic());
//                studentUser.setIsCorrect(student.getIsCorrect());
//                studentUser.setGmtCreate(student.getGmtCreate());
//                studentUser.setGmtModified(student.getGmtModified());
//                studentUser.setUrpclassNum(0000000000);
//            }
//            catch (Exception e){
//                System.err.println(e.getMessage());
//            }
            //try {
                StudentUserExample studentUserExample = new StudentUserExample();
                studentUserExample.createCriteria().andAccountEqualTo(student.getAccount());
                int x = studentUserMapper.countByExample(studentUserExample);
                if(x>0){
                    try{
                        StudentExample studentExample = new StudentExample();
                        studentExample.createCriteria().andAccountEqualTo(student.getAccount());
                        studentMapper.deleteByExample(studentExample);
                    }
                    catch (Exception e){
                        continue;
                    }
                }
                else{
                    studentUserMapper.insertSelective(studentUser);
                    StudentExample studentExample = new StudentExample();
                    studentExample.createCriteria().andAccountEqualTo(student.getAccount());
                    studentMapper.deleteByExample(studentExample);
                    count+=1;
                    Thread.sleep(500);
                }
            //}
           // catch (Exception e){
           //     fail += 1;
            //    count+=1;
           //     System.out.println("完成，共影响了"+count+"条数据,"+fail+"条失败(+1),学号为："+student.getAccount());
           // }
        }
        System.out.println("完成，共影响了"+count+"条数据,"+fail+"条失败");
    }

    public Integer getUrpClassNum(String className) {
        SearchClassInfoPost post = new SearchClassInfoPost();
        post.setPageSize("50");
        post.setPageNum("1");
        Pattern pattern = Pattern.compile("[0-9]{2}");
        Matcher matcher = pattern.matcher(className);
        if (matcher.find()) {
            post.setYearNum("20" + matcher.group());
        }

        List<SearchResult<ClassInfoSearchResult>> classInfoSearchResult = newUrpSpiderService.getClassInfoSearchResult(post);
        for (SearchResult<ClassInfoSearchResult> classInfoSearchResultSearchResult : classInfoSearchResult) {
            List<ClassInfoSearchResult> records = classInfoSearchResultSearchResult.getRecords();
            for (ClassInfoSearchResult record : records) {
                if (className.trim().equals(record.getClassName().trim())) {
                    String[] str = String.valueOf(record.getId()).split("Id");
                    String s = str[1].split(",")[0].split("=")[1];
                    return Integer.parseInt(s);
                } else {
                    System.err.println("异常在这里");
                }
            }
        }
    return -1;
    }
}
