package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.dto.CreatorSettlementAccountDTO;
import com.alikeyou.itmodulepayment.entity.CreatorSettlementAccount;
import com.alikeyou.itmodulepayment.service.CreatorSettlementAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/creator-settlement-accounts")
public class CreatorSettlementAccountController {

    private final CreatorSettlementAccountService creatorSettlementAccountService;

    public CreatorSettlementAccountController(CreatorSettlementAccountService creatorSettlementAccountService) {
        this.creatorSettlementAccountService = creatorSettlementAccountService;
    }

    // 创建创作者结算账户
    @PostMapping
    public ResponseEntity<CreatorSettlementAccount> createCreatorSettlementAccount(@RequestBody CreatorSettlementAccountDTO dto) {
        CreatorSettlementAccount account = creatorSettlementAccountService.createCreatorSettlementAccount(dto);
        return new ResponseEntity<>(account, HttpStatus.CREATED);
    }

    // 更新创作者结算账户
    @PutMapping("/{id}")
    public ResponseEntity<CreatorSettlementAccount> updateCreatorSettlementAccount(@PathVariable Long id, @RequestBody CreatorSettlementAccountDTO dto) {
        CreatorSettlementAccount account = creatorSettlementAccountService.updateCreatorSettlementAccount(id, dto);
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    // 删除创作者结算账户
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCreatorSettlementAccount(@PathVariable Long id) {
        creatorSettlementAccountService.deleteCreatorSettlementAccount(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // 根据ID查询创作者结算账户
    @GetMapping("/{id}")
    public ResponseEntity<CreatorSettlementAccount> getCreatorSettlementAccountById(@PathVariable Long id) {
        CreatorSettlementAccount account = creatorSettlementAccountService.getCreatorSettlementAccountById(id);
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    // 根据用户ID查询创作者结算账户
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CreatorSettlementAccount>> getCreatorSettlementAccountsByUserId(@PathVariable Long userId) {
        List<CreatorSettlementAccount> accounts = creatorSettlementAccountService.getCreatorSettlementAccountsByUserId(userId);
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    // 根据用户ID和状态查询创作者结算账户
    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<CreatorSettlementAccount>> getCreatorSettlementAccountsByUserIdAndStatus(
            @PathVariable Long userId, @PathVariable String status) {
        List<CreatorSettlementAccount> accounts = creatorSettlementAccountService.getCreatorSettlementAccountsByUserIdAndStatus(userId, status);
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }
}