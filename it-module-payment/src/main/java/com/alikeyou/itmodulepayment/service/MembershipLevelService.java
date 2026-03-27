package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulepayment.dto.MembershipLevelDTO;
import com.alikeyou.itmodulepayment.entity.MembershipLevel;

import java.util.List;

public interface MembershipLevelService {

    // 创建会员等级
    MembershipLevel createMembershipLevel(MembershipLevelDTO dto);

    // 更新会员等级
    MembershipLevel updateMembershipLevel(Long id, MembershipLevelDTO dto);

    // 删除会员等级
    void deleteMembershipLevel(Long id);

    // 根据ID查询会员等级
    MembershipLevel getMembershipLevelById(Long id);

    // 查询所有会员等级
    List<MembershipLevel> getAllMembershipLevels();

    // 根据状态查询会员等级
    List<MembershipLevel> getMembershipLevelsByStatus(String status);

    // 根据等级顺序排序查询
    List<MembershipLevel> getMembershipLevelsByOrder();
}