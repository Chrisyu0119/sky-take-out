package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用戶下單
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //1.處理業務各種異常 (地址簿為空,購物車為空)
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            //拋出業務異常
            throw new OrderBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        ShoppingCart shoppingCart = new ShoppingCart();
        //查詢用戶當前購物車數據
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            //拋出業務異常
            throw new OrderBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //2.向訂單表插入一條數據
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orderMapper.insert(orders);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        //3.向訂單明細表插入多條數據
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();//訂單明細
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());//設置當前訂單明細關聯地的訂單ID
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        //4.清空購物車
        shoppingCartMapper.deleteByUserId(userId);
        //5.封裝VO返回結果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount()).build();
        return orderSubmitVO;
    }

    /**
     * 訂單支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 當前登錄用戶id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

//        //調用微信支付接口，生成預支付交易單
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商戶訂單號
//                new BigDecimal(0.01), //支付金額，單位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用戶的openid
//        );
        JSONObject jsonObject = new JSONObject();
        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("該訂單已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改訂單狀態
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        //根據訂單編號查詢訂單
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        //根據訂單ID更新訂單的狀態、支付方式、支付狀態、結帳時間
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //通過WebSokcet向客戶端瀏覽器推送消息 type orderId content
        Map map = new HashMap();
        map.put("type",1);//1.表示來單提醒 2.表示客戶催單
        map.put("orderId",ordersDB.getId());
        map.put("content","訂單號碼: "+ outTradeNo);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json) ;
    }
    /**
     * 用户端订单分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        // 設置分頁
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分頁條件查詢
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList();

        //查出訂單明細，並封裝OrderVO進行響應
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();// 訂單id

                // 查詢訂單明細
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
        // 根據id查詢訂單
        Orders orders = orderMapper.getById(id);

        // 查詢該訂單對應的菜品/套餐明細
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 將該訂單及其詳情封裝到OrderVO並返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用戶取消訂單
     *
     * @param id
     */
    public void userCancelById(Long id) throws Exception {
        // 根據id查詢訂單
        Orders ordersDB = orderMapper.getById(id);

        // 校驗訂單是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //訂單狀態 1待付款 2待接單 3已接單 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 訂單處於待接單狀態下取消，需要進行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //調用微信支付退款介面
            weChatPayUtil.refund(
                    ordersDB.getNumber(), //商戶訂單號
                    ordersDB.getNumber(), //商戶退款單號
                    new BigDecimal(0.01),//退款金額，單位 元
                    new BigDecimal(0.01));//原訂單金額

            //支付狀態修改為 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新訂單狀態、取消原因、取消時間
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用戶取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再來一單
     *
     * @param id
     */
    public void repetition(Long id) {
        // 查詢當前用戶id
        Long userId = BaseContext.getCurrentId();

        // 根據訂單id查詢當前訂單詳情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 將訂單詳情物件轉換為購物車物件
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 將原訂單詳情裡的菜品資訊重新複製到購物車物件中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        //  將購物車物件批次新增到資料庫
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 訂單搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分訂單狀態，需要額外返回訂單菜品資訊，將Orders轉換為OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回訂單菜品資訊，自訂OrderVO回應結果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 將共同欄位複製到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 將訂單菜品資訊封裝到orderVO中，並新增到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根據訂單id獲取菜品資訊字串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查詢訂單菜品詳情資訊（訂單中的菜品和數量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 將每一條訂單菜品資訊拼湊為字串（格式：宮保雞丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 將該訂單對應的所有菜品資訊拼湊在一起
        return String.join("", orderDishList);
    }

    /**
     * 各狀態的訂單數量統計
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接單
     *
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }

    /**
     * 拒單
     *
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 根據id查詢訂單
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        //  訂單只有存在且裝態為2(待接單)才可拒單
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付狀態
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            //用戶已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        //拒單需要退款，根據訂單id更新訂單狀態、拒單原因、取消時間
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 取消訂單
     *
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根據id查詢訂單
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        // 支付狀態
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            //用戶已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        // 館李端取消訂單需要退款，根據訂單id更新訂單狀態、取消原因、取消時間
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 派送訂單
     *
     * @param id
     */
    public void delivery(Long id) {
        // 根據id查詢訂單
        Orders ordersDB = orderMapper.getById(id);

        // 校驗訂單是否存在，並狀態為3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新訂單狀態，狀態轉為派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 完成訂單
     *
     * @param id
     */
    public void complete(Long id) {
        // 根據id查詢訂單
        Orders ordersDB = orderMapper.getById(id);

        // 校驗訂單是否存在，並狀態為4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新訂單狀態，狀態轉為完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 客戶催單
     *
     * @param id
     */
    @Override
    public void reminder(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        // 校驗訂單是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Map map = new HashMap<>();
        map.put("type",2);//1表示來單提醒 2代表客戶催單
        map.put("orderId",id);
        map.put("content","訂單號碼"+ordersDB.getNumber());
        //通過WebSocket向Clint端發送消息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }
}

