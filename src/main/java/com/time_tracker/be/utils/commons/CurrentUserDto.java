package com.time_tracker.be.utils.commons;

import lombok.Data;

@Data
public class CurrentUserDto {
    private String email;
    private String fullName;
    private String token;
    private Integer userId;
    private String role;
    private String photo;

}
