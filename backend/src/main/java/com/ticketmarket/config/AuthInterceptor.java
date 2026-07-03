package com.ticketmarket.config;

import com.ticketmarket.common.ApiException;
import com.ticketmarket.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiException(401, "请先登录");
        }
        try {
            Claims claims = jwtUtil.parse(authorization.substring("Bearer ".length()));
            String username = String.valueOf(claims.get("username"));
            String role = String.valueOf(claims.get("role"));
            AuthContext.set(new AuthContext.AuthUser(Long.valueOf(claims.getSubject()), username, role));
            checkPermission(request.getRequestURI(), role);
            return true;
        } catch (Exception ex) {
            if (ex instanceof ApiException apiException) {
                throw apiException;
            }
            throw new ApiException(401, "登录状态已过期，请重新登录");
        }
    }

    private void checkPermission(String uri, String role) {
        if (uri.startsWith("/api/admin/") && !hasAnyRole(role, "ADMIN", "MANAGER")) {
            throw new ApiException(403, "无权限访问后台");
        }
        if (uri.startsWith("/api/checker/") && !hasAnyRole(role, "CHECKER", "ADMIN", "MANAGER")) {
            throw new ApiException(403, "无权限访问检票功能");
        }
    }

    private boolean hasAnyRole(String role, String... allowedRoles) {
        for (String allowedRole : allowedRoles) {
            if (allowedRole.equals(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
