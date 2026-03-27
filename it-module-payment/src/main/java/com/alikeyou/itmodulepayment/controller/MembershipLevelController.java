package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.dto.MembershipLevelDTO;
import com.alikeyou.itmodulepayment.entity.MembershipLevel;
import com.alikeyou.itmodulepayment.service.MembershipLevelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membership-levels")
public class MembershipLevelController {

    private final MembershipLevelService membershipLevelService;

    public MembershipLevelController(MembershipLevelService membershipLevelService) {
        this.membershipLevelService = membershipLevelService;
    }

    // 创建会员等级
    @PostMapping
    public ResponseEntity<MembershipLevel> createMembershipLevel(@RequestBody MembershipLevelDTO dto) {
        MembershipLevel membershipLevel = membershipLevelService.createMembershipLevel(dto);
        return new ResponseEntity<>(membershipLevel, HttpStatus.CREATED);
    }

    // 更新会员等级
    @PutMapping("/{id}")
    public ResponseEntity<MembershipLevel> updateMembershipLevel(@PathVariable Long id, @RequestBody MembershipLevelDTO dto) {
        MembershipLevel membershipLevel = membershipLevelService.updateMembershipLevel(id, dto);
        return new ResponseEntity<>(membershipLevel, HttpStatus.OK);
    }

    // 删除会员等级
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMembershipLevel(@PathVariable Long id) {
        membershipLevelService.deleteMembershipLevel(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // 根据ID查询会员等级
    @GetMapping("/{id}")
    public ResponseEntity<MembershipLevel> getMembershipLevelById(@PathVariable Long id) {
        MembershipLevel membershipLevel = membershipLevelService.getMembershipLevelById(id);
        return new ResponseEntity<>(membershipLevel, HttpStatus.OK);
    }

    // 查询所有会员等级
    @GetMapping
    public ResponseEntity<List<MembershipLevel>> getAllMembershipLevels() {
        List<MembershipLevel> membershipLevels = membershipLevelService.getAllMembershipLevels();
        return new ResponseEntity<>(membershipLevels, HttpStatus.OK);
    }

    // 根据是否启用查询会员等级
    @GetMapping("/enabled/{isEnabled}")
    public ResponseEntity<List<MembershipLevel>> getMembershipLevelsByIsEnabled(@PathVariable Boolean isEnabled) {
        List<MembershipLevel> membershipLevels = membershipLevelService.getMembershipLevelsByIsEnabled(isEnabled);
        return new ResponseEntity<>(membershipLevels, HttpStatus.OK);
    }
}