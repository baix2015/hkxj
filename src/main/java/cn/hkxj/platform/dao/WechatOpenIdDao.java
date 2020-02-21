package cn.hkxj.platform.dao;


import cn.hkxj.platform.mapper.WechatOpenidMapper;
import cn.hkxj.platform.pojo.WechatOpenid;
import cn.hkxj.platform.pojo.WechatOpenidExample;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class WechatOpenIdDao {
    @Resource
    private WechatOpenidMapper wechatOpenidMapper;

    public List<WechatOpenid> selectByPojo(WechatOpenid wechatOpenid) {
        WechatOpenidExample example = new WechatOpenidExample();
        WechatOpenidExample.Criteria criteria = example.createCriteria();

        if (wechatOpenid.getOpenid() != null) {
            criteria.andOpenidEqualTo(wechatOpenid.getOpenid());
        }
        if (wechatOpenid.getAppid() != null) {
            criteria.andAppidEqualTo(wechatOpenid.getAppid());
        }

        return wechatOpenidMapper.selectByExample(example);
    }

    public WechatOpenid selectByUniqueKey(String appid, String openid) {
        WechatOpenid wechatOpenid = new WechatOpenid().setOpenid(openid).setAppid(appid);
        return selectByPojo(wechatOpenid).stream().findFirst().orElse(null);
    }


    public void insertSelective(WechatOpenid wechatOpenid){
        wechatOpenidMapper.insertSelective(wechatOpenid);
    }

    public void updateByPrimaryKeySelective(WechatOpenid wechatOpenid){
        wechatOpenidMapper.updateByPrimaryKeySelective(wechatOpenid);
    }

    public List<WechatOpenid> getOpenid(String openid) {
        WechatOpenidExample wechatOpenidExample = new WechatOpenidExample();
        wechatOpenidExample.createCriteria().andOpenidEqualTo(openid);
        return wechatOpenidMapper.selectByExample(wechatOpenidExample);
    }


    public void openIdUnbind(String openid, String appid) {
        WechatOpenidExample example = new WechatOpenidExample();
        example.createCriteria()
                .andAppidEqualTo(appid)
                .andOpenidEqualTo(openid);

        wechatOpenidMapper.updateByExampleSelective(new WechatOpenid().setIsBind(false), example);
    }


    public void openIdUnbindAllPlatform(int account) {
        WechatOpenidExample wechatOpenidExample = new WechatOpenidExample();
        wechatOpenidExample.createCriteria().andAccountEqualTo(account);
        wechatOpenidMapper.updateByExampleSelective(new WechatOpenid().setIsBind(false), wechatOpenidExample);
    }


    public List<String> getAllOpenidsFromOneClass(int classId, String openid, String appid) {
        return wechatOpenidMapper.getAllOpenidsFromOneClass(classId, openid);
    }


}
