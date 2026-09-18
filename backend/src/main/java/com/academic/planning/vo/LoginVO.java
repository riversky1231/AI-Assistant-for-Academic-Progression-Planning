package com.academic.planning.vo;

import java.util.List;

public record LoginVO(String tokenName, String tokenValue, long timeout, List<String> permissions, UserVO user) {}
