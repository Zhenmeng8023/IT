package com.alikeyou.itmodulecommon.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateUserDTO {
    private String phone;
    private String gender;
    private String bio;
    private String regionId;
    private String avatarUrl;
    private String birthday;
    private String nickname;
    private String authorTagId;
    private String identityCard;
    private List<List<String>> tags;
}