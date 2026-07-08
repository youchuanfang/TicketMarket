package com.ticketmarket.controller;

import com.ticketmarket.common.ApiException;
import com.ticketmarket.common.Result;
import com.ticketmarket.config.AuthContext;
import com.ticketmarket.dto.AuthResponse;
import com.ticketmarket.dto.LoginRequest;
import com.ticketmarket.dto.RegisterRequest;
import com.ticketmarket.dto.UserProfileResponse;
import com.ticketmarket.model.UserAccount;
import com.ticketmarket.service.DemoDataService;
import com.ticketmarket.util.JwtUtil;
import com.ticketmarket.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final DemoDataService dataService;
    private final JwtUtil jwtUtil;

    public AuthController(DemoDataService dataService, JwtUtil jwtUtil) {
        this.dataService = dataService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        UserAccount account = dataService.findUserByName(request.username())
                .orElseThrow(() -> new ApiException(401, "用户名或密码错误"));
        if (!PasswordUtil.matches(request.password(), account.getPasswordHash())) {
            throw new ApiException(401, "用户名或密码错误");
        }
        if (request.roleCode() != null && !request.roleCode().isBlank()
                && !request.roleCode().trim().equalsIgnoreCase(account.getRoleCode())) {
            throw new ApiException(403, "请选择正确的登录入口");
        }
        if (isPrivilegedRole(account.getRoleCode()) && !isLocalRequest(servletRequest)) {
            throw new ApiException(403, "后台账号仅允许在服务器本机登录");
        }
        return Result.ok(toAuthResponse(account));
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserAccount account = dataService.createUser(request.username(), request.password(), request.nickname());
        return Result.ok(toAuthResponse(account));
    }

    @GetMapping("/me")
    public Result<UserProfileResponse> me() {
        Long userId = AuthContext.currentUserId();
        UserAccount account = dataService.findUserById(userId)
                .orElseThrow(() -> new ApiException(401, "用户不存在"));
        return Result.ok(toProfile(account));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }

    private AuthResponse toAuthResponse(UserAccount account) {
        String token = jwtUtil.generate(account.getId(), account.getUsername(), account.getRoleCode());
        return new AuthResponse(
                token,
                account.getId(),
                account.getUsername(),
                account.getNickname(),
                account.getRoleCode(),
                account.isRealNameVerified()
        );
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

    private boolean isPrivilegedRole(String roleCode) {
        return "ADMIN".equals(roleCode) || "MANAGER".equals(roleCode) || "CHECKER".equals(roleCode);
    }

    private boolean isLocalRequest(HttpServletRequest request) {
        String host = request.getServerName();
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host) || "[::1]".equals(host);
    }
}
