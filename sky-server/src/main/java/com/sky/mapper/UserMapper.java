package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根據Openid查詢用戶
     *
     * @param openid
     * @return
     */
    @Select("select * from sky_take_out.user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 插入數據
     *
     * @param user
     */
    void insert(User user);

    @Select("select * from sky_take_out.user where id = #{id}")
    User getById(Long userId);

    /**
     * 根據動態條件來統計用戶數量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
