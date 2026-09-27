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

/**
 * 认证与权限拦截器：对每个受保护的接口，从 satoken 头解析会话、加载权限与角色，
 * 校验 @RequirePermission 注解，并把当前用户写入 ThreadLocal 的 AuthContext。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    // 前端携带 token 的请求头名
    public static final String TOKEN_HEADER = "satoken";
    // Redis 会话键前缀：academic:session:{token}
    public static final String SESSION_PREFIX = "academic:session:";
    // 会话有效期 2 小时，每次校验通过后滑动续期
    public static final Duration SESSION_TTL = Duration.ofHours(2);

    private final RedisGateway redisGateway;
    private final PermissionMapper permissionMapper;

    public AuthInterceptor(RedisGateway redisGateway, PermissionMapper permissionMapper) {
        this.redisGateway = redisGateway;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 非控制器方法（如静态资源）直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        String token = request.getHeader(TOKEN_HEADER);
        // token 缺失、空白或超长（>100）一律视为未登录，做基本长度防护
        if (token == null || token.isBlank() || token.length() > 100) {
            throw new AuthException(401, "请先登录");
        }
        String userIdText;
        try {
            // 查 Redis 拿 token 对应的 userId；查不到说明未登录或已过期
            userIdText = redisGateway.get(SESSION_PREFIX + token);
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "权限服务暂不可用");
        }
        if (userIdText == null) {
            throw new AuthException(401, "登录已过期");
        }
        try {
            long userId = Long.parseLong(userIdText);
            // 每次请求实时查库拿最新权限/角色，保证改权限立即生效
            List<String> permissions = permissionMapper.selectPermissionCodesByUserId(userId);
            List<String> roles = permissionMapper.selectRoleCodesByUserId(userId);
            // 校验方法上的 @RequirePermission 注解
            RequirePermission required = handlerMethod.getMethodAnnotation(RequirePermission.class);
            if (required != null && !permissions.contains(required.value())) {
                throw new AuthException(403, "无权访问该接口");
            }
            // 滑动续期：活跃用户每次访问都刷新会话有效期
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
