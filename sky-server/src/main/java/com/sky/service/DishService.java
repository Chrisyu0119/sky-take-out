package com.sky.service;

import com.sky.dto.DishDTO;

public interface DishService {

    /**
     * 新增菜品和對應的口味數據
     * @param dishDTO
     */
    public void saveWithFlavor(DishDTO dishDTO);
}
