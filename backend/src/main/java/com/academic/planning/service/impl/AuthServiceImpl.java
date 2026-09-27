package com.academic.planning.service.impl;

import com.academic.planning.common.BusinessException;
import com.academic.planning.dto.AdminCreateUserRequest;
import com.academic.planning.dto.AdminUpdateUserRequest;
import com.academic.planning.dto.ChangePasswordRequest;
import com.academic.planning.dto.ForgotPasswordRequest;
import com.academic.planning.dto.LoginRequest;
import com.academic.planning.dto.RegisterRequest;
import com.academic.planning.dto.ResetPasswordRequest;
import com.academic.planning.dto.UpdateProfileRequest;
import com.academic.planning.dto.WechatLoginRequest;
import com.academic.planning.entity.SysUser;
import com.academic.planning.mapper.PermissionMapper;
import com.academic.planning.mapper.UserMapper;
import com.academic.planning.security.PasswordHasher;
import com.academic.planning.security.AuthContext;
import com.academic.planning.security.AuthInterceptor;
import com.academic.planning.service.AuthService;
import com.academic.planning.service.RedisGateway;
import com.academic.planning.vo.LoginVO;
import com.academic.planning.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * 认证与账号管理核心实现：登录/注册/微信登录/找回密码/资料与密码管理。
 * 安全要点：PBKDF2 慢哈希存密码、登录与找回密码做 Redis 限流、登录成功签发 Redis 会话 token。
 */
@Service
public class AuthServiceImpl implements AuthService {

    // 登录限流：5 分钟窗口最多 10 次，超过返回 429
    private static final int MAX_LOGIN_ATTEMPTS = 10;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(5);
    // 找回密码限流：10 分钟窗口最多 5 次
    private static final int MAX_RESET_ATTEMPTS = 5;
    private static final Duration RESET_WINDOW = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

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
        // 用 SHA-256(用户名) 作限流键，避免把明文用户名写进 Redis
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
        // 统一返回「用户名或密码错误」，不区分账号不存在/停用/密码错，避免枚举用户
        SysUser user = findByUsername(request.username());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())
                || !passwordHasher.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        LoginVO login = createSession(user);
        // 登录成功后清除该用户的失败计数
        redisGateway.delete(rateLimitKey);
        return login;
    }

    @Transactional
    public LoginVO register(RegisterRequest request) {
        String username = cleanRequired(request.username());
        assertUsernameAvailable(username, null);
        assertPhoneAvailable(clean(request.phone()), null);
        assertEmailAvailable(clean(request.email()), null);
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordHasher.hash(request.password()));
        user.setNickname(defaultText(request.nickname(), username));
        user.setPhone(clean(request.phone()));
        user.setEmail(clean(request.email()));
        user.setEnabled(true);
        userMapper.insert(user);
        initializeUserPermissions(user);
        return createSession(user);
    }

    @Transactional
    public LoginVO wechatLogin(WechatLoginRequest request) {
        // 演示实现：未接真实微信 code2session，用 SHA-256(code) 伪造稳定 openid 标识同一用户
        String openid = "mock_" + sha256(cleanRequired(request.code())).substring(0, 32);
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getWechatOpenid, openid)
                .last("LIMIT 1"));
        if (user == null) {
            // 首次微信登录自动注册，生成随机用户名与不可登录的随机密码
            user = new SysUser();
            user.setUsername(nextWechatUsername());
            user.setPasswordHash(passwordHasher.hash("wx-" + java.util.UUID.randomUUID()));
            user.setNickname(defaultText(request.nickname(), "微信用户"));
            user.setWechatOpenid(openid);
            user.setEnabled(true);
            userMapper.insert(user);
            initializeUserPermissions(user);
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号已被停用");
        }
        return createSession(user);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        String keySeed = cleanRequired(request.username()).toLowerCase();
        enforceRateLimit("academic:password-reset:" + sha256(keySeed), RESET_WINDOW, MAX_RESET_ATTEMPTS, "找回密码尝试过多，请稍后再试");
        SysUser user = findByUsername(request.username());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "账号不存在或已停用");
        }
        String phone = clean(request.phone());
        String email = clean(request.email());
        boolean phoneMatched = phone != null && phone.equals(user.getPhone());
        boolean emailMatched = email != null && email.equalsIgnoreCase(Objects.toString(user.getEmail(), ""));
        if (!phoneMatched && !emailMatched) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "手机号或邮箱与账号不匹配");
        }
        user.setPasswordHash(passwordHasher.hash(request.newPassword()));
        userMapper.updateById(user);
    }

    public void changePassword(ChangePasswordRequest request) {
        SysUser user = requireCurrentUser();
        if (!passwordHasher.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "原密码不正确");
        }
        if (request.oldPassword().equals(request.newPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "新密码不能与原密码相同");
        }
        user.setPasswordHash(passwordHasher.hash(request.newPassword()));
        userMapper.updateById(user);
    }

    public UserVO currentUser() {
        return UserVO.from(requireCurrentUser());
    }

    public UserVO updateProfile(UpdateProfileRequest request) {
        SysUser user = requireCurrentUser();
        String phone = clean(request.phone());
        String email = clean(request.email());
        assertPhoneAvailable(phone, user.getId());
        assertEmailAvailable(email, user.getId());
        user.setNickname(defaultText(request.nickname(), user.getUsername()));
        user.setPhone(phone);
        user.setEmail(email);
        userMapper.updateById(user);
        return UserVO.from(user);
    }

    public List<UserVO> listUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId))
                .stream()
                .map(UserVO::from)
                .toList();
    }

    @Transactional
    public UserVO createUser(AdminCreateUserRequest request) {
        String username = cleanRequired(request.username());
        assertUsernameAvailable(username, null);
        assertPhoneAvailable(clean(request.phone()), null);
        assertEmailAvailable(clean(request.email()), null);
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordHasher.hash(request.password()));
        user.setNickname(defaultText(request.nickname(), username));
        user.setPhone(clean(request.phone()));
        user.setEmail(clean(request.email()));
        user.setEnabled(request.enabled() == null || request.enabled());
        userMapper.insert(user);
        initializeUserPermissions(user);
        return UserVO.from(user);
    }

    private void initializeUserPermissions(SysUser user) {
        userMapper.initializeDefaultRole();
        userMapper.initializeDefaultPermissions();
        userMapper.initializeDefaultRolePermissions();
        userMapper.insertRoleByCode(user.getId(), "user");
    }

    public UserVO updateUser(long userId, AdminUpdateUserRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "账号不存在");
        }
        String phone = clean(request.phone());
        String email = clean(request.email());
        assertPhoneAvailable(phone, userId);
        assertEmailAvailable(email, userId);
        user.setNickname(defaultText(request.nickname(), user.getUsername()));
        user.setPhone(phone);
        user.setEmail(email);
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        userMapper.updateById(user);
        return UserVO.from(user);
    }

    public void resetPassword(long userId, ResetPasswordRequest request) {
        int updated = userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getPasswordHash, passwordHasher.hash(request.newPassword())));
        if (updated == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "账号不存在");
        }
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

    private void enforceRateLimit(String key, Duration window, int maxAttempts, String message) {
        long attempts;
        try {
            attempts = redisGateway.increment(key, window);
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "认证服务暂不可用");
        }
        if (attempts > maxAttempts) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, message);
        }
    }

    private LoginVO createSession(SysUser user) {
        // 生成随机 UUID 作为会话 token，存入 Redis：academic:session:{token} = userId，TTL 2 小时
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
        // 返回 token 头名、token、有效期秒数，以及该用户的权限码列表供前端感知角色
        return new LoginVO(
                AuthInterceptor.TOKEN_HEADER,
                token,
                AuthInterceptor.SESSION_TTL.toSeconds(),
                permissionMapper.selectPermissionCodesByUserId(user.getId()),
                UserVO.from(user)
        );
    }

    private SysUser requireCurrentUser() {
        long userId = AuthContext.requireUser().userId();
        SysUser user = userMapper.selectById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录状态无效");
        }
        return user;
    }

    private SysUser findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, cleanRequired(username))
                .last("LIMIT 1"));
    }

    private void assertUsernameAvailable(String username, Long currentUserId) {
        SysUser existed = findByUsername(username);
        if (existed != null && !Objects.equals(existed.getId(), currentUserId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "账号已存在");
        }
    }

    private void assertPhoneAvailable(String phone, Long currentUserId) {
        if (phone == null) {
            return;
        }
        SysUser existed = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone)
                .last("LIMIT 1"));
        if (existed != null && !Objects.equals(existed.getId(), currentUserId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "手机号已被使用");
        }
    }

    private void assertEmailAvailable(String email, Long currentUserId) {
        if (email == null) {
            return;
        }
        SysUser existed = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email)
                .last("LIMIT 1"));
        if (existed != null && !Objects.equals(existed.getId(), currentUserId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "邮箱已被使用");
        }
    }

    private String nextWechatUsername() {
        for (int i = 0; i < 10; i++) {
            String username = "wx_" + Long.toUnsignedString(RANDOM.nextLong(), 36).replace("-", "");
            if (userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username).last("LIMIT 1")) == null) {
                return username;
            }
        }
        return "wx_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String cleanRequired(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请求参数不合法");
        }
        return cleaned;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String defaultText(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned == null ? fallback : cleaned;
    }
}
