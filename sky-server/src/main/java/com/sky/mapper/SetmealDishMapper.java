package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根據菜品id來查詢對應的套餐id
     * @param dishIds
     * @return
     */
    //select setmeal id from setmeal_dish where dish_id in (1,2,3,4)
    List<Long> getSetmealIdsDishIds(List<Long> dishIds);
}
