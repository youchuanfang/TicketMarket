package com.ticketmarket.controller;

import com.ticketmarket.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/checker")
public class CheckerController {
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(Map.of(
                "checkinToday", 42,
                "successRate", "98.6%",
                "latestResult", "核验通道运行正常"
        ));
    }
}
