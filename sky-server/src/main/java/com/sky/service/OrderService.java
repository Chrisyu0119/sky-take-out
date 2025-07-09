package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    /**
     * 用戶下單
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 訂單支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改訂單狀態
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 用戶端訂單分頁查詢
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageResult pageQuery4User(int page, int pageSize, Integer status);

    /**
     * 查詢訂單詳情
     * @param id
     * @return
     */
    OrderVO details(Long id);

    /**
     * 用戶取消訂單
     * @param id
     */
    void userCancelById(Long id) throws Exception;

    /**
     * 再來一單
     * @param id
     */
    void repetition(Long id);

    /**
     * 條件搜索訂單
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     *各狀態的訂單數量統計
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 接單
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 拒單
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO)throws Exception;

    /**
     * 商家取消訂單
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO)throws Exception;

    /**
     * 派送訂單
     * @param id
     */
    void delivery(Long id);

    /**
     * 完成訂單
     * @param id
     */
    void complete(Long id);
}
