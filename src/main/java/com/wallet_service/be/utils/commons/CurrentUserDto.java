package com.wallet_service.be.utils.commons;

import lombok.Data;

import java.util.Map;

@Data
public class CurrentUserDto {
    private String email;
    private String fullName;
    private String token;
    private Integer userId;
    private String role;
    private Integer roleId;
    private String photo;
    private Map<String, PermissionActionDto> permissions;
}
