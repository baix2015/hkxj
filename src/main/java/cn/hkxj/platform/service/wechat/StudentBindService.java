package cn.hkxj.platform.service.wechat;

import cn.hkxj.platform.dao.StudentDao;
import cn.hkxj.platform.dao.WechatBindRecordDao;
import cn.hkxj.platform.dao.WechatOpenIdDao;
import cn.hkxj.platform.exceptions.OpenidExistException;
import cn.hkxj.platform.exceptions.PasswordUnCorrectException;
import cn.hkxj.platform.exceptions.ReadTimeoutException;
import cn.hkxj.platform.pojo.*;
import cn.hkxj.platform.service.ClassService;
import cn.hkxj.platform.service.NewUrpSpiderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author junrong.chen
 * @date 2018/10/13
 */
@Service("studentBindService")
@Slf4j
public class StudentBindService {
    @Resource
    private StudentDao studentDao;
    @Resource
    private WechatOpenIdDao wechatOpenIdDao;
    @Resource
    private NewUrpSpiderService newUrpSpiderService;
    @Value("${domain}")
    private String domain;
    private static final String PATTERN = "<a href=\"%s/bind?openid=%s&appid=%s\">%s</a>";

    private static final String TEXT_LINK = "<a href=\"%s\">%s</a>";
    @Resource
    private WechatBindRecordDao wechatBindRecordDao;
    @Resource
    private ClassService classService;
    /**
     * 学号与微信公众平台openID关联
     * <p>
     * 现在的一个问题是，如果是从一次订阅的接口路由过来的用户，如何帮他们实现快速绑定呢？
     * 点击地址以后将openID存在session中，查看是否已经绑定
     * <p>
     * 1.数据库中有学生信息，并且密码不正确的，重新通过教务网确认密码
     * 2.数据库中有学生信息且密码正确，直接绑定
     * 3.数据库中没有学生数据，查询后绑定
     *
     * @param openid   微信用户唯一标识
     * @param account  学生教务网账号
     * @param password 学生教务网密码
     * @throws PasswordUnCorrectException 密码不正确异常
     * @throws ReadTimeoutException       读取信息超时异常
     * @throws OpenidExistException       Openid已存在
     */
    public Student studentBind(String openid, String account, String password, String appid) throws OpenidExistException {

        Student student = studentLogin(account, password);

        studentBind(student, openid, appid);

        return student;

    }

    /**
     * 用于学生从非微信渠道登录
     *
     * @param account  账号
     * @param password 密码
     * @return 学生信息
     */
    public Student studentLogin(String account, String password) throws PasswordUnCorrectException {
        Student student = studentDao.selectStudentByAccount(Integer.parseInt(account));

        if (student != null && (!student.getIsCorrect() || !student.getPassword().equals(password))) {
            newUrpSpiderService.checkStudentPassword(account, password);
            studentDao.updatePassword(account, password);
        } else if (student == null) {
            student = newUrpSpiderService.getStudentInfo(account, password);
            studentDao.insertStudent(student);
        }

        return student;
    }

    /**
     * 如果已经存在openid则更新关联的学号
     * 并且插入一条更新的记录
     *
     * 否则插入一条新数据再绑定
     *
     * @param student 学生信息
     * @param openid  微信用户唯一标识
     * @param appid   微信平台对应的id
     */
     void studentBind(Student student, String openid, String appid) {
        WechatOpenid wechatOpenid = wechatOpenIdDao.selectByUniqueKey(appid, openid);
        if (wechatOpenid != null) {
            if (!student.getAccount().equals(wechatOpenid.getAccount())){
                wechatOpenIdDao.updateByPrimaryKeySelective(
                        wechatOpenid.setAccount(student.getAccount()).setGmtModified(new Date()));

                wechatBindRecordDao.insertSelective(new WechatBindRecord()
                        .setOriginAccount(wechatOpenid.getAccount().toString())
                        .setUpdateAccount(student.getAccount().toString())
                        .setAppid(appid).setOpenid(openid));
            }
        } else {
            wechatOpenIdDao.insertSelective(new WechatOpenid()
                    .setAccount(student.getAccount())
                    .setOpenid(openid)
                    .setAppid(appid));
        }

    }

    public boolean isStudentBind(String openid, String appid) {
        WechatOpenid wechatOpenid = wechatOpenIdDao.selectByUniqueKey(appid, openid);
        if (wechatOpenid == null) {
            return false;
        } else {
            return wechatOpenid.getIsBind();
        }
    }

    public Student getStudentByOpenID(String openid, String appid) {
        WechatOpenid wechatOpenid = wechatOpenIdDao.selectByUniqueKey(appid, openid);
        if (wechatOpenid != null) {
            return studentDao.selectStudentByAccount(wechatOpenid.getAccount());
        }
        throw new RuntimeException("用户未绑定");

    }

    public StudentUser getStudentUserInfo(String account, String password){
        StudentUser userInfo = newUrpSpiderService.getStudentUserInfo(account, password);
        UrpClass urpClass = classService.getClassByName(userInfo.getClassName(), userInfo.getAccount().toString());
        userInfo.setUrpclassNum(Integer.parseInt(urpClass.getClassNum()));

        return userInfo;

    }

    public String getBindUrlByOpenid(String fromUser, String appId, String content) {
        return String.format(PATTERN, domain, fromUser, appId, content);
    }

    public String getTextLink(String url, String content) {
        return String.format(TEXT_LINK, url, content);
    }

}
