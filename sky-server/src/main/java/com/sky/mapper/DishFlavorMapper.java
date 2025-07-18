package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 批量插入口味數據
     * @param flavors
     */
    void InsertBatch(List<DishFlavor> flavors);

    /**
     * 根據菜品id刪除對應的口味數據
     * @param dishId
     */
    @Delete("delete from sky_take_out.dish_flavor where dish_id = #{dishId}")
    void deleteByDishId(Long dishId);

    /**
     * 根據菜品id集合批量刪除關聯的口味數據
     * @param dishIds
     */
    void deleteByDishIds(List<Long> dishIds);

    /**
     * 根據菜品id查詢對應的口味數據
     * @param dishId
     * @return
     */
    @Select("select * from sky_take_out.dish_flavor where dish_id = #{dishId}")
    List<DishFlavor> getByDishId(Long dishId);
}
