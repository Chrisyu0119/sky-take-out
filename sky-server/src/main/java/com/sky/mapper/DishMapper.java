package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from sky_take_out.dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入菜品數據
     * @param dish
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Dish dish);

    /**
     * 菜品分頁查詢
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根據主見查詢菜品
     * @param id
     * @return
     */
    @Select("select * from sky_take_out.dish where id = #{id}")
    Dish getById(Long id);

    /**
     * 根據主鍵刪除菜品數據
     * @param id
     */
    @Delete("delete from sky_take_out.dish where id = #{id}")
    void deletById(Long id);

    /**
     * 根據菜品id集合批量刪除菜品
     * @param ids
     */

    void deleteByIds(List<Long> ids);

    /**
     * 根據id動態修改菜品數據
     * @param dish
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);

    /**
     * 根據條件動態查詢菜品列表
     * @param dish 包含查詢条件的Dish物件
     * @return 符合条件的菜品列表
     */
    List<Dish> list(Dish dish);
}
