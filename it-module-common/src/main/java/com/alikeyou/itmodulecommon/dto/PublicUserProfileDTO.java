package com.alikeyou.itmodulecommon.dto;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicUserProfileDTO {
    private Long id;
    private String username;
    private String avatarUrl;
    private String bio;
    private String nickname;
    private Long regionId;
    private String regionName;
    private Long authorTagId;
    private String authorTagName;

    public PublicUserProfileDTO() {
    }

    public PublicUserProfileDTO(UserInfo userInfo) {
        this.id = userInfo.getId();
        this.username = userInfo.getUsername();
        this.avatarUrl = userInfo.getAvatarUrl();
        this.bio = userInfo.getBio();
        this.nickname = userInfo.getNickname();

        if (userInfo.getRegion() != null) {
            this.regionId = userInfo.getRegion().getId();
            this.regionName = userInfo.getRegion().getName();
        }

        if (userInfo.getAuthorTag() != null) {
            this.authorTagId = userInfo.getAuthorTag().getId();
            this.authorTagName = userInfo.getAuthorTag().getName();
        }
    }
}
