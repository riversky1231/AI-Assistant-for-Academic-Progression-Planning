package com.academic.planning.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "只能包含字母、数字和下划线") String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Size(max = 50) String nickname,
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        @Email @Size(max = 100) String email,
        Boolean enabled
) {}
