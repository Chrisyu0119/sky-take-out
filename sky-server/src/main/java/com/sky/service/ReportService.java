package com.sky.service;
import com.sky.vo.TurnoverReportVO;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public interface ReportService {

    /**
     * 統計指定時間內的營業額數據
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);
}
