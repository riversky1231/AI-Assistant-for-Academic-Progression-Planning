package com.academic.planning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WechatLoginRequest(
        @NotBlank @Size(max = 200) String code,
        @Size(max = 50) String nickname
) {}
