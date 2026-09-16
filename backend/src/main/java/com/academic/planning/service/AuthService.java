package com.academic.planning.service;

import com.academic.planning.dto.LoginRequest;
import com.academic.planning.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginRequest request);
    void logout();
}
