package cn.hkxj.platform.service;

import cn.hkxj.platform.dao.StudentDao;
import cn.hkxj.platform.dao.WechatOpenIdDao;
import cn.hkxj.platform.pojo.Student;
import cn.hkxj.platform.pojo.WechatOpenid;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author junrong.chen
 * @date 2018/10/22
 */
@Service("openIdService")
public class OpenIdService {

    @Resource
    private WechatOpenIdDao wechatOpenIdDao;
    @Resource
    private StudentDao studentDao;

    /**
     * 已修改并在OpenIdServiceTest里面进行了单元测试
     */
    public boolean openidIsBind(String openid, String appid) {
        WechatOpenid wechatOpenid = wechatOpenIdDao.selectByUniqueKey(appid, openid);
        if (wechatOpenid == null){
            return false;
        }
        return wechatOpenid.getIsBind();

    }


    //wechatOpenidExample
    //源码的泛型是：、Openid
    /**
     * 已修改并在OpenIdServiceTest里面进行了单元测试
     */
    public List<WechatOpenid> getOpenid(String openid, String appid) {
       return wechatOpenIdDao.getOpenid(openid);

    }

    /**
     * 已修改并在OpenIdServiceTest里面进行了单元测试
     */
    public Student getStudentByOpenId(String appid, String openid) {
        WechatOpenid wechatOpenid = wechatOpenIdDao.selectByUniqueKey(appid, openid);
        return studentDao.selectStudentByAccount(wechatOpenid.getAccount());


    }

    /**
     * 已修改并在OpenIdServiceTest里面进行了单元测试
     */
    public void openIdUnbind(String openid, String appid) {

        wechatOpenIdDao.openIdUnbind(openid,appid);

    }


    /**
     * 对于密码错误的账号全平台解绑
     * @param account
     */
    public void openIdUnbindAllPlatform(int account) {

        wechatOpenIdDao.openIdUnbindAllPlatform(account);

    }



    /**
     * 已修改并在OpenIdServiceTest里面进行了单元测试
     */
    public List<String> getAllOpenidsFromOneClass(int classId, String openid, String appid) {

        return wechatOpenIdDao.getAllOpenidsFromOneClass(classId,openid,appid);
    }



}
