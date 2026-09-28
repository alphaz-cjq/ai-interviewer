package com.cjq.mapper;

import com.cjq.pojo.PO.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 基础 CRUD 已被 BaseMapper 提供
    // 如果以后需要自定义 SQL，在这里加方法即可
    // 例如：
    // @Select("SELECT * FROM user WHERE email = #{email}")
    // User selectByEmail(String email);
}
