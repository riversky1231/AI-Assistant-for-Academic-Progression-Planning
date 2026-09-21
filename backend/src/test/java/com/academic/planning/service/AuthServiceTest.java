package com.academic.planning.service;

import com.academic.planning.dto.AdminCreateUserRequest;
import com.academic.planning.dto.RegisterRequest;
import com.academic.planning.dto.WechatLoginRequest;
import com.academic.planning.entity.SysUser;
import com.academic.planning.mapper.PermissionMapper;
import com.academic.planning.mapper.UserMapper;
import com.academic.planning.security.PasswordHasher;
import com.academic.planning.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserMapper users = mock(UserMapper.class);
    private final PermissionMapper permissions = mock(PermissionMapper.class);
    private final PasswordHasher passwords = mock(PasswordHasher.class);
    private final RedisGateway redis = mock(RedisGateway.class);
    private final AuthServiceImpl service = new AuthServiceImpl(users, permissions, passwords, redis);
    private static final List<String> DEFAULT_PERMISSIONS = List.of("recommend:use", "school:read");

    @BeforeEach
    void setUp() {
        when(passwords.hash(anyString())).thenReturn("hashed");
        when(users.insert(any(SysUser.class))).thenAnswer(call -> {
            call.<SysUser>getArgument(0).setId(42L);
            return 1;
        });
        // Permissions become visible only after the ordinary role is assigned.
        doAnswer(call -> {
            when(permissions.selectPermissionCodesByUserId(42L)).thenReturn(DEFAULT_PERMISSIONS);
            return null;
        }).when(users).insertRoleByCode(42L, "user");
    }

    @ParameterizedTest
    @ValueSource(strings = {"register", "wechat", "admin"})
    void everyNewAccountInitializesAndReceivesTheOrdinaryRole(String entry) {
        switch (entry) {
            case "register" -> assertEquals(DEFAULT_PERMISSIONS, service.register(
                    new RegisterRequest("student", "password88", null, null, null)).permissions());
            case "wechat" -> assertEquals(DEFAULT_PERMISSIONS, service.wechatLogin(
                    new WechatLoginRequest("test-code", null)).permissions());
            case "admin" -> service.createUser(
                    new AdminCreateUserRequest("student", "password88", null, null, null, true));
            default -> fail("Unexpected entry");
        }
        var order = inOrder(users);
        order.verify(users).insert(any(SysUser.class));
        order.verify(users).initializeDefaultRole();
        order.verify(users).initializeDefaultPermissions();
        order.verify(users).initializeDefaultRolePermissions();
        order.verify(users).insertRoleByCode(42L, "user");
        verify(users, never()).insertRoleByCode(anyLong(), eq("admin"));
    }

    @Test
    void failedPermissionInitializationCannotReturnASuccessfulRegistration() {
        doThrow(new IllegalStateException("database unavailable")).when(users).initializeDefaultRolePermissions();
        assertThrows(IllegalStateException.class, () -> service.register(
                new RegisterRequest("student", "password88", null, null, null)));
        verify(users, never()).insertRoleByCode(anyLong(), anyString());
        verifyNoInteractions(redis);
    }

    @Test
    void existingWechatAccountIsNotReassignedRolesOnLogin() {
        SysUser existing = new SysUser();
        existing.setId(42L);
        existing.setUsername("existing");
        existing.setEnabled(true);
        when(users.selectOne(any())).thenReturn(existing);
        service.wechatLogin(new WechatLoginRequest("test-code", null));
        verify(users, never()).insert(any(SysUser.class));
        verify(users, never()).initializeDefaultRole();
        verify(users, never()).initializeDefaultPermissions();
        verify(users, never()).initializeDefaultRolePermissions();
        verify(users, never()).insertRoleByCode(anyLong(), anyString());
    }
}
