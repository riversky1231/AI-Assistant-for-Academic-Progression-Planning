package com.academic.planning.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PermissionMapper {
    List<String> selectPermissionCodesByUserId(@Param("userId") long userId);
    List<String> selectRoleCodesByUserId(@Param("userId") long userId);
}
