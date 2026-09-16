package com.academic.planning.controller;

import com.academic.planning.common.ApiResponse;
import com.academic.planning.dto.LoginRequest;
import com.academic.planning.service.AuthService;
import com.academic.planning.security.AuthContext;
import com.academic.planning.security.SessionUser;
import com.academic.planning.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success("退出成功", null);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        SessionUser user = AuthContext.requireUser();
        return ApiResponse.success(Map.of(
                "userId", user.userId(),
                "permissions", user.permissions(),
                "roles", user.roles()
        ));
    }
}
