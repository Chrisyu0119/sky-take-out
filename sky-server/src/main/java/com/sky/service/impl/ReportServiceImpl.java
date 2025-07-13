package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper   ;
    /**
     * 統計指定時間內的營業額數據
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        //當前集合用於存放begin到end範圍內的每天的日期
        List<LocalDate> dateList = new ArrayList();
        dateList.add(begin);
        dateList.add(begin.plusDays(1));
        while (!begin.equals(end)){
            //日期計算,計算指定日期的後一天對應的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放每天的營業額
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查詢date日期對應的營業額數據,營業額是指: 狀態為"已完成"的訂單金額合計
            LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
            //select sum(amount) from orders where order_time > beginTime and order_time< endTime and status = 5
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            //判斷是否為空,如果為空轉成0,如果不為空就是他自己.然後賦值給turnover  用三元運算
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        //封裝返回結果
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /**
     * 統計指定時間內用戶數據
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //存放從begin到end之間每天的對應日期
        List<LocalDate> dateList = new ArrayList();

        dateList.add(begin);

        while (!begin.equals(end)){
            dateList.add(begin.plusDays(1));
        }

        //存放每天新增用戶數量  select count(id) from where create_time < ? and create_time > ?
        List<Integer> newUserList = new ArrayList<>();
        //存放每天用戶總量     select count(id) from where create_time < ?
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end", endTime);
            //總用戶數量
            Integer totalUser = userMapper.countByMap(map);
            map.put("begin", beginTime);
            //新增用戶數量
            Integer newUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }
        //封裝數據結果
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }

    /**
     * 統計指定時間內訂單數據
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //存放從begin到end之間每天的對應日期
        List<LocalDate> dateList = new ArrayList();

        dateList.add(begin);

        while (!begin.equals(end)){
            dateList.add(begin.plusDays(1));
        }

        //存放每天訂單總數
        List<Integer> orderCountList = new ArrayList<>();
        //存放每天有效定單數
        List<Integer> validOrderCountList = new ArrayList<>();

        //遍歷dateList查詢每天的有效訂單與訂單總數
        for (LocalDate date : dateList) {
            //查詢每天訂單總數 select count(id) from orders where order_time > ? and order_time < ?
            LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
            Integer orderCount = getOrderCount(beginTime, endTime, null);
            //查詢每天有效訂單數 select count(id) from orders where order_time > ? and order_time < ? and status = ?
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);
            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }
        //計算時間區間內訂單總數量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //計算時間區間內有效訂單數量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            //計算訂單完成率
            orderCompletionRate = (double) (validOrderCount/totalOrderCount);
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 統計時間區間內銷量排名TOP10
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");
        //封裝返回接果數據
        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 根據條件統計定單數量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end,Integer status) {
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }
}
