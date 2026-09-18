package com.academic.planning.mapper;

import com.academic.planning.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<SysUser> {
    void insertRoleByCode(@Param("userId") long userId, @Param("roleCode") String roleCode);
}
