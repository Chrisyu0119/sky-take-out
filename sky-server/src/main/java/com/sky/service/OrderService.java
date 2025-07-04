package com.sky.service;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;

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
}
