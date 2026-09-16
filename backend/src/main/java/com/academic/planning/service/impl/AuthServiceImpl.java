package com.academic.planning.service.impl;

import com.academic.planning.common.BusinessException;
import com.academic.planning.dto.LoginRequest;
import com.academic.planning.entity.SysUser;
import com.academic.planning.mapper.PermissionMapper;
import com.academic.planning.mapper.UserMapper;
import com.academic.planning.security.PasswordHasher;
import com.academic.planning.security.AuthContext;
import com.academic.planning.security.AuthInterceptor;
import com.academic.planning.service.AuthService;
import com.academic.planning.service.RedisGateway;
import com.academic.planning.vo.LoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int MAX_LOGIN_ATTEMPTS = 10;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(5);

    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordHasher passwordHasher;
    private final RedisGateway redisGateway;

    public AuthServiceImpl(UserMapper userMapper, PermissionMapper permissionMapper,
                       PasswordHasher passwordHasher, RedisGateway redisGateway) {
        this.userMapper = userMapper;
        this.permissionMapper = permissionMapper;
        this.passwordHasher = passwordHasher;
        this.redisGateway = redisGateway;
    }

    public LoginVO login(LoginRequest request) {
        String rateLimitKey = "academic:login-attempt:" + sha256(request.username().trim().toLowerCase());
        long attempts;
        try {
            attempts = redisGateway.increment(rateLimitKey, LOGIN_WINDOW);
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "登录服务暂不可用");
        }
        if (attempts > MAX_LOGIN_ATTEMPTS) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "登录尝试过多，请稍后再试");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username())
                .last("LIMIT 1"));
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())
                || !passwordHasher.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = java.util.UUID.randomUUID().toString();
        try {
            redisGateway.set(
                    AuthInterceptor.SESSION_PREFIX + token,
                    user.getId().toString(),
                    AuthInterceptor.SESSION_TTL
            );
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "登录服务暂不可用");
        }
        redisGateway.delete(rateLimitKey);
        return new LoginVO(
                AuthInterceptor.TOKEN_HEADER,
                token,
                AuthInterceptor.SESSION_TTL.toSeconds(),
                permissionMapper.selectPermissionCodesByUserId(user.getId())
        );
    }

    public void logout() {
        String token = AuthContext.requireUser().token();
        redisGateway.delete(AuthInterceptor.SESSION_PREFIX + token);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成限流键", exception);
        }
    }
}
