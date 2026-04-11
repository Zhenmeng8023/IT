package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ProjectUserInfoLite")
@Table(name = "user_info")
public class UserInfoLite {

    @Id
    private Long id;

    private String username;

    private String nickname;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "role_id")
    private Integer roleId;

    private String status;
}
