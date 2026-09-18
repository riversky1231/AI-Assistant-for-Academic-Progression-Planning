package com.academic.planning.service;

import com.academic.planning.dto.AdminCreateUserRequest;
import com.academic.planning.dto.AdminUpdateUserRequest;
import com.academic.planning.dto.ChangePasswordRequest;
import com.academic.planning.dto.ForgotPasswordRequest;
import com.academic.planning.dto.LoginRequest;
import com.academic.planning.dto.RegisterRequest;
import com.academic.planning.dto.ResetPasswordRequest;
import com.academic.planning.dto.UpdateProfileRequest;
import com.academic.planning.dto.WechatLoginRequest;
import com.academic.planning.vo.LoginVO;
import com.academic.planning.vo.UserVO;

import java.util.List;

public interface AuthService {
    LoginVO login(LoginRequest request);
    LoginVO register(RegisterRequest request);
    LoginVO wechatLogin(WechatLoginRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void changePassword(ChangePasswordRequest request);
    UserVO currentUser();
    UserVO updateProfile(UpdateProfileRequest request);
    List<UserVO> listUsers();
    UserVO createUser(AdminCreateUserRequest request);
    UserVO updateUser(long userId, AdminUpdateUserRequest request);
    void resetPassword(long userId, ResetPasswordRequest request);
    void logout();
}
