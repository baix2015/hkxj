package cn.hkxj.platform.dao;

import cn.hkxj.platform.PlatformApplication;
import cn.hkxj.platform.config.wechat.MiniProgramProperties;
import cn.hkxj.platform.config.wechat.WechatMpPlusProperties;
import cn.hkxj.platform.config.wechat.WechatMpProProperties;
import cn.hkxj.platform.mapper.MiniProgramOpenidMapper;
import cn.hkxj.platform.mapper.OpenidMapper;
import cn.hkxj.platform.mapper.OpenidPlusMapper;
import cn.hkxj.platform.mapper.WechatOpenidMapper;
import cn.hkxj.platform.mapper.ext.WechatOpenIdExtMapper;
import cn.hkxj.platform.pojo.MiniProgramOpenid;
import cn.hkxj.platform.pojo.MiniProgramOpenidExample;
import cn.hkxj.platform.pojo.WechatOpenid;
import cn.hkxj.platform.pojo.example.OpenidExample;
import cn.hkxj.platform.pojo.wechat.Openid;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = PlatformApplication.class)
public class WechatOpenidTest {
    @Autowired
    private OpenidMapper openidMapper;

    @Autowired
    private WechatOpenidMapper wechatOpenidMapper;

    @Autowired
    private OpenidPlusMapper openidPlusMapper;

    @Autowired
    private MiniProgramOpenidMapper miniProgramOpenidMapper;

    @Autowired
    private WechatMpPlusProperties wechatMpPlusProperties;

    @Autowired
    private WechatMpProProperties wechatMpProProperties;

    @Autowired
    private MiniProgramProperties miniProgramProperties;


    @Autowired
    private WechatOpenIdExtMapper wechatOpenIdExtMapper;
    
    //测试从oppenid表中拿出数据插入到WeChatopenid表
    //openid表中第5864条数据和上面的某一条数据openid重复
    //已插入的数据学号2018025144   未插入的数据学号2018025838
    //运行结束插入5637条数据，源数据5638条，但是有一条APPID重复为插入成功
    //此次运行的最后一条数据id为5641
    @Test
    public void insertWechat_openidFromopenid(){

        List<Openid> openids = openidPlusMapper.selectByExample(new OpenidExample());
        List<WechatOpenid> collect = openids.stream().map(x -> {
            WechatOpenid wechatOpenid = new WechatOpenid();
            wechatOpenid.setAccount(x.getAccount());
            wechatOpenid.setGmtCreate(x.getGmtCreate());
            wechatOpenid.setGmtModified(x.getGmtModified());
            wechatOpenid.setIsBind(x.getIsBind());
            wechatOpenid.setOpenid(x.getOpenid());
            wechatOpenid.setAppid(wechatMpPlusProperties.getAppId());
            return wechatOpenid;
        }).collect(Collectors.toList());

        for (List<WechatOpenid> openidList : Lists.partition(collect, 200)) {
            wechatOpenIdExtMapper.insertBatch(openidList);
        }



    }

    //1987条数据
    //又遇到一个APPID重复的值：oDUpSs8B8-vr4TPruTy2XPSyPg3U/////oDUpSs8B8-vr4TPruTy2XPSyPg3U
    //wechat_openid:oDUpSs8B8-vr4TPruTy2XPSyPg3U的数据id=1083
    //Openid_plus里面的数据学号是：2014025838
    //两张表插入的最后一条数据id=7630
    @Test//测试从Openid_plus表中拿出数据插入到WeChatopenid表
    public void insertWechat_openidFromOpenid_plus(){
        List<Openid> openids = openidMapper.selectByExample(new OpenidExample());
        List<WechatOpenid> collect = openids.stream().map(x -> {
            WechatOpenid wechatOpenid = new WechatOpenid();
            wechatOpenid.setAccount(x.getAccount());
            wechatOpenid.setGmtCreate(x.getGmtCreate());
            wechatOpenid.setGmtModified(x.getGmtModified());
            wechatOpenid.setIsBind(x.getIsBind());
            wechatOpenid.setOpenid(x.getOpenid());
            wechatOpenid.setAppid(wechatMpProProperties.getAppId());
            return wechatOpenid;
        }).collect(Collectors.toList());

        for (List<WechatOpenid> openidList : Lists.partition(collect, 200)) {
            wechatOpenIdExtMapper.insertBatch(openidList);
        }

    }

    //MiniProgramOpenid表杨新华同学负责导入数据，所以线面的代码没有运行测试过
    @Test//测试从MiniProgramOpenid表中拿出数据插入到WeChatopenid表
    public void insertWechat_openidFromMiniProgramOpenid(){
        List<MiniProgramOpenid> miniProgramOpenids = miniProgramOpenidMapper.selectByExample(new MiniProgramOpenidExample());
        List<WechatOpenid> collect = miniProgramOpenids.stream().map(x -> {
            WechatOpenid wechatOpenid = new WechatOpenid();
            wechatOpenid.setAccount(x.getAccount());
            wechatOpenid.setGmtCreate(x.getGmtCreate());
            wechatOpenid.setGmtModified(x.getGmtModified());
            wechatOpenid.setIsBind(x.getIsBind());
            wechatOpenid.setOpenid(x.getOpenid());
            wechatOpenid.setAppid(miniProgramProperties.getAppId());
            return wechatOpenid;
        }).collect(Collectors.toList());

        for (List<WechatOpenid> openidList : Lists.partition(collect, 200)) {
            wechatOpenIdExtMapper.insertBatch(openidList);
        }

    }

    @Test
    public void getobjectwithtime(){
       /* Openid openid = openidMapper.selectByPrimaryKey(208);
        System.out.println(openid.getGmtCreate());
        Openid openid1 = openidPlusMapper.selectByPrimaryKey(1);
        System.out.println(openid1.getGmtCreate());*/
        List<Openid> openids = openidMapper.selectByExample(new OpenidExample());
        System.out.println("openid总数据："+openids.size());
        System.out.println(openids.get(0).getGmtCreate());


    }



    @Test
    public void test1() {


    }

}
