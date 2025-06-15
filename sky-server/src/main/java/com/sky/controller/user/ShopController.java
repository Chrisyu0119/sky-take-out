package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "店鋪相關接口")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP STATUS";

    @Autowired
    RedisTemplate redisTemplate;


    /**
     * 獲取店鋪營業狀態
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("獲取店鋪營業狀態")
    public Result<Integer> getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("獲取店鋪營業狀態為: {}",status == 1?"營業中":"打烊中");
        return Result.success(status);
    }
}
