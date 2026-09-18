package com.academic.planning.controller;

import com.academic.planning.common.ApiResponse;
import com.academic.planning.dto.AdminCreateUserRequest;
import com.academic.planning.dto.AdminUpdateUserRequest;
import com.academic.planning.dto.ChangePasswordRequest;
import com.academic.planning.dto.ForgotPasswordRequest;
import com.academic.planning.dto.LoginRequest;
import com.academic.planning.dto.RegisterRequest;
import com.academic.planning.dto.ResetPasswordRequest;
import com.academic.planning.dto.UpdateProfileRequest;
import com.academic.planning.dto.WechatLoginRequest;
import com.academic.planning.service.AuthService;
import com.academic.planning.security.AuthContext;
import com.academic.planning.security.RequirePermission;
import com.academic.planning.security.SessionUser;
import com.academic.planning.vo.LoginVO;
import com.academic.planning.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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

    @PostMapping("/register")
    public ApiResponse<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    @PostMapping("/wechat-login")
    public ApiResponse<LoginVO> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        return ApiResponse.success("微信登录成功", authService.wechatLogin(request));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success("密码已重置", null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success("退出成功", null);
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.success("密码已修改", null);
    }

    @GetMapping("/profile")
    public ApiResponse<UserVO> profile() {
        return ApiResponse.success(authService.currentUser());
    }

    @PutMapping("/profile")
    public ApiResponse<UserVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("资料已更新", authService.updateProfile(request));
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

    @GetMapping("/users")
    @RequirePermission("account:manage")
    public ApiResponse<List<UserVO>> users() {
        return ApiResponse.success(authService.listUsers());
    }

    @PostMapping("/users")
    @RequirePermission("account:manage")
    public ApiResponse<UserVO> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return ApiResponse.success("账号已创建", authService.createUser(request));
    }

    @PutMapping("/users/{userId}")
    @RequirePermission("account:manage")
    public ApiResponse<UserVO> updateUser(@PathVariable long userId, @Valid @RequestBody AdminUpdateUserRequest request) {
        return ApiResponse.success("账号已更新", authService.updateUser(userId, request));
    }

    @PostMapping("/users/{userId}/reset-password")
    @RequirePermission("account:manage")
    public ApiResponse<Void> resetPassword(@PathVariable long userId, @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(userId, request);
        return ApiResponse.success("密码已重置", null);
    }
}
