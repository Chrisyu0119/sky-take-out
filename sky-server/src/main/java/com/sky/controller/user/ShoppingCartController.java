package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Api(tags = "c端購物車相關接口")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加購物車
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("添加購物車")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("添加購物車,商品信息為: {}",shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    /**
     * 查詢購物車
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查詢購物車")
    public Result<List<ShoppingCart>> list(){
//        log.info("添加購物車,商品信息為: {}",shoppingcart);
        List<ShoppingCart> list = shoppingCartService.showShoppingCart();
        return Result.success(list);
    }
}
