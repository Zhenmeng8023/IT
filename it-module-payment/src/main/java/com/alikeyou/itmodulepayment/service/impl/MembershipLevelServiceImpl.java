package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.dto.MembershipLevelDTO;
import com.alikeyou.itmodulepayment.entity.MembershipLevel;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.service.MembershipLevelService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MembershipLevelServiceImpl implements MembershipLevelService {

    private final MembershipLevelRepository membershipLevelRepository;

    public MembershipLevelServiceImpl(MembershipLevelRepository membershipLevelRepository) {
        this.membershipLevelRepository = membershipLevelRepository;
    }

    @Override
    public MembershipLevel createMembershipLevel(MembershipLevelDTO dto) {
        MembershipLevel membershipLevel = new MembershipLevel();
        BeanUtils.copyProperties(dto, membershipLevel);
        membershipLevel.setCreatedAt(LocalDateTime.now());
        membershipLevel.setUpdatedAt(LocalDateTime.now());
        return membershipLevelRepository.save(membershipLevel);
    }

    @Override
    public MembershipLevel updateMembershipLevel(Long id, MembershipLevelDTO dto) {
        MembershipLevel membershipLevel = membershipLevelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("会员等级不存在"));
        BeanUtils.copyProperties(dto, membershipLevel);
        membershipLevel.setUpdatedAt(LocalDateTime.now());
        return membershipLevelRepository.save(membershipLevel);
    }

    @Override
    public void deleteMembershipLevel(Long id) {
        membershipLevelRepository.deleteById(id);
    }

    @Override
    public MembershipLevel getMembershipLevelById(Long id) {
        return membershipLevelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("会员等级不存在"));
    }

    @Override
    public List<MembershipLevel> getAllMembershipLevels() {
        return membershipLevelRepository.findAll();
    }

    @Override
    public List<MembershipLevel> getMembershipLevelsByStatus(String status) {
        return membershipLevelRepository.findByStatus(status);
    }

    @Override
    public List<MembershipLevel> getMembershipLevelsByOrder() {
        return membershipLevelRepository.findAllByOrderByLevelOrderAsc();
    }
}