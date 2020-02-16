package cn.hkxj.platform.dao;

import cn.hkxj.platform.mapper.MiniProgramOpenidMapper;
import cn.hkxj.platform.mapper.WechatOpenidMapper;
import cn.hkxj.platform.pojo.MiniProgramOpenid;
import cn.hkxj.platform.pojo.MiniProgramOpenidExample;
import cn.hkxj.platform.pojo.WechatOpenid;
import cn.hkxj.platform.pojo.WechatOpenidExample;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
@Service
public class WechatOpenIdDao {



    @Resource
    private WechatOpenidMapper wechatOpenidMapper;

    public List<WechatOpenid> selectByPojo( WechatOpenid wechatOpenid){
        WechatOpenidExample example=new WechatOpenidExample();
        WechatOpenidExample.Criteria criteria = example.createCriteria();

        if(wechatOpenid.getOpenid() != null){
            criteria.andOpenidEqualTo(wechatOpenid.getOpenid());
        }

        return wechatOpenidMapper.selectByExample(example);
    }
}
