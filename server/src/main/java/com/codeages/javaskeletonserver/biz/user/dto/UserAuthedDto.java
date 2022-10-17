package com.codeages.javaskeletonserver.biz.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class UserAuthedDto {
    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private List<String> roles;

    private String token;
}
