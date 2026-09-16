package com.academic.planning.security;

import com.academic.planning.common.BusinessException;
import com.academic.planning.mapper.PermissionMapper;
import com.academic.planning.service.RedisGateway;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String TOKEN_HEADER = "satoken";
    public static final String SESSION_PREFIX = "academic:session:";
    public static final Duration SESSION_TTL = Duration.ofHours(2);

    private final RedisGateway redisGateway;
    private final PermissionMapper permissionMapper;

    public AuthInterceptor(RedisGateway redisGateway, PermissionMapper permissionMapper) {
        this.redisGateway = redisGateway;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || token.isBlank() || token.length() > 100) {
            throw new AuthException(401, "请先登录");
        }
        String userIdText;
        try {
            userIdText = redisGateway.get(SESSION_PREFIX + token);
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "权限服务暂不可用");
        }
        if (userIdText == null) {
            throw new AuthException(401, "登录已过期");
        }
        try {
            long userId = Long.parseLong(userIdText);
            List<String> permissions = permissionMapper.selectPermissionCodesByUserId(userId);
            List<String> roles = permissionMapper.selectRoleCodesByUserId(userId);
            RequirePermission required = handlerMethod.getMethodAnnotation(RequirePermission.class);
            if (required != null && !permissions.contains(required.value())) {
                throw new AuthException(403, "无权访问该接口");
            }
            redisGateway.expire(SESSION_PREFIX + token, SESSION_TTL);
            AuthContext.set(new SessionUser(userId, token, permissions, roles));
            return true;
        } catch (NumberFormatException exception) {
            throw new AuthException(401, "登录状态无效");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        AuthContext.clear();
    }
}
