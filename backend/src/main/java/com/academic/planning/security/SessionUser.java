package com.academic.planning.security;

import java.util.List;

public record SessionUser(long userId, String token, List<String> permissions, List<String> roles) {}
