package com.academic.planning.vo;

import com.academic.planning.entity.SysUser;

public record UserVO(
        Long id,
        String username,
        String nickname,
        String phone,
        String email,
        boolean wechatBound,
        Boolean enabled
) {
    public static UserVO from(SysUser user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getWechatOpenid() != null && !user.getWechatOpenid().isBlank(),
                user.getEnabled()
        );
    }
}
