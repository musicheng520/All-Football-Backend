package com.msc.controller.admin;

import com.msc.model.vo.DashboardSummaryVO;
import com.msc.result.Result;
import com.msc.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public Result<DashboardSummaryVO> summary() {
        return Result.success(dashboardService.summary());
    }
}