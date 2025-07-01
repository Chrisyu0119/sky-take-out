package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 插入訂單數據
     * @param orders
     * @return
     */
    void insert(Orders orders);
}
