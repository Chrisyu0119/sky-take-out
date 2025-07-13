package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 插入訂單數據
     * @param orders
     * @return
     */
    void insert(Orders orders);

    /**
     * 根據訂單號查詢訂單
     * @param orderNumber
     */
    @Select("select * from sky_take_out.orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分頁查詢並按下單時間排序
     * @param ordersPageQueryDTO
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根據id查詢訂單
     * @param id
     * @return
     */
    @Select("select * from sky_take_out.orders where id=#{id}")
    Orders getById(Long id);

    /**
     * 根據狀態統計訂單數量
     * @param toBeConfirmed
     * @return
     */
    @Select("select count(id) from sky_take_out.orders where status = #{status}")
    Integer countStatus(Integer toBeConfirmed);

    /**
     * 根據訂單狀態和下單時間查詢訂單
     * @param status
     * @param orderTime
     * @return
     */
    @Select("select * from sky_take_out.orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);

    /**
     * 根據動態條件統計營業額數據
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 根據動態條件統計訂單數量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
