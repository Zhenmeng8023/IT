package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulepayment.dto.MembershipDTO;

import java.util.Optional;

public interface MembershipService {

    // 检查用户是否为会员
    Boolean checkUserIsVip(Long userId);

    // 获取用户的会员信息
    Optional<MembershipDTO> getUserMembershipInfo(Long userId);

    // 获取用户当前有效的会员信息
    Optional<MembershipDTO> getUserActiveMembership(Long userId);
}
