package cn.hkxj.platform.mapper.ext;

import cn.hkxj.platform.mapper.UrpClassMapper;
import cn.hkxj.platform.pojo.UrpClass;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author JR Chan
 */
@Mapper
@Repository
public interface UrpClassExtMapper extends UrpClassMapper {
    void insertBatch(List<UrpClass> list);
}
