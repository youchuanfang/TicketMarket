package com.ticketmarket.controller;

import com.ticketmarket.common.ApiException;
import com.ticketmarket.common.Result;
import com.ticketmarket.config.AuthContext;
import com.ticketmarket.dto.RealNameRequest;
import com.ticketmarket.dto.UserProfileResponse;
import com.ticketmarket.dto.ViewerRequest;
import com.ticketmarket.dto.ViewerUpdateRequest;
import com.ticketmarket.model.UserAccount;
import com.ticketmarket.model.Viewer;
import com.ticketmarket.service.DemoDataService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final DemoDataService dataService;

    public UserController(DemoDataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/me")
    public Result<UserProfileResponse> me() {
        return Result.ok(toProfile(currentAccount()));
    }

    @PostMapping("/real-name")
    public Result<UserProfileResponse> realName(@Valid @RequestBody RealNameRequest request) {
        UserAccount account = currentAccount();
        account.setRealNameVerified(true);
        return Result.ok(toProfile(account));
    }

    @GetMapping("/viewers")
    public Result<List<Viewer>> viewers() {
        return Result.ok(dataService.listViewers(currentAccount().getId()));
    }

    @PostMapping("/viewers")
    public Result<Viewer> addViewer(@Valid @RequestBody ViewerRequest request) {
        return Result.ok(dataService.addViewer(currentAccount().getId(), request.name(), request.idCard(), request.phone()));
    }

    @PutMapping("/viewers/{viewerId}")
    public Result<Viewer> updateViewer(@PathVariable Long viewerId, @Valid @RequestBody ViewerUpdateRequest request) {
        return Result.ok(dataService.updateViewer(currentAccount().getId(), viewerId, request.name(), request.idCard(), request.phone()));
    }

    @DeleteMapping("/viewers/{viewerId}")
    public Result<Void> deleteViewer(@PathVariable Long viewerId) {
        dataService.deleteViewer(currentAccount().getId(), viewerId);
        return Result.ok();
    }

    @PutMapping("/viewers/{viewerId}/default")
    public Result<Viewer> setDefaultViewer(@PathVariable Long viewerId) {
        return Result.ok(dataService.setDefaultViewer(currentAccount().getId(), viewerId));
    }

    private UserAccount currentAccount() {
        Long userId = AuthContext.currentUserId();
        if (userId == null) {
            throw new ApiException(401, "请先登录");
        }
        return dataService.findUserById(userId).orElseThrow(() -> new ApiException(401, "用户不存在"));
    }

    private UserProfileResponse toProfile(UserAccount account) {
        return new UserProfileResponse(
                account.getId(),
                account.getUsername(),
                account.getNickname(),
                account.getRoleCode(),
                account.isRealNameVerified(),
                "138****0000",
                account.getUsername() + "@example.com",
                account.isRealNameVerified() ? "3301**********2245" : ""
        );
    }
}
