package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.dto.CreatorWithdrawRequestDTO;
import com.alikeyou.itmodulepayment.entity.CreatorWithdrawRequest;
import com.alikeyou.itmodulepayment.service.CreatorWithdrawRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/creator-withdraw-requests")
public class CreatorWithdrawRequestController {

    private final CreatorWithdrawRequestService creatorWithdrawRequestService;

    public CreatorWithdrawRequestController(CreatorWithdrawRequestService creatorWithdrawRequestService) {
        this.creatorWithdrawRequestService = creatorWithdrawRequestService;
    }

    // 创建创作者提现请求
    @PostMapping
    public ResponseEntity<CreatorWithdrawRequest> createCreatorWithdrawRequest(@RequestBody CreatorWithdrawRequestDTO dto) {
        CreatorWithdrawRequest request = creatorWithdrawRequestService.createCreatorWithdrawRequest(dto);
        return new ResponseEntity<>(request, HttpStatus.CREATED);
    }

    // 更新创作者提现请求
    @PutMapping("/{id}")
    public ResponseEntity<CreatorWithdrawRequest> updateCreatorWithdrawRequest(@PathVariable Long id, @RequestBody CreatorWithdrawRequestDTO dto) {
        CreatorWithdrawRequest request = creatorWithdrawRequestService.updateCreatorWithdrawRequest(id, dto);
        return new ResponseEntity<>(request, HttpStatus.OK);
    }

    // 删除创作者提现请求
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCreatorWithdrawRequest(@PathVariable Long id) {
        creatorWithdrawRequestService.deleteCreatorWithdrawRequest(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // 根据ID查询创作者提现请求
    @GetMapping("/{id}")
    public ResponseEntity<CreatorWithdrawRequest> getCreatorWithdrawRequestById(@PathVariable Long id) {
        CreatorWithdrawRequest request = creatorWithdrawRequestService.getCreatorWithdrawRequestById(id);
        return new ResponseEntity<>(request, HttpStatus.OK);
    }

    // 根据用户ID查询创作者提现请求
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CreatorWithdrawRequest>> getCreatorWithdrawRequestsByUserId(@PathVariable Long userId) {
        List<CreatorWithdrawRequest> requests = creatorWithdrawRequestService.getCreatorWithdrawRequestsByUserId(userId);
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }

    // 根据状态查询创作者提现请求
    @GetMapping("/status/{status}")
    public ResponseEntity<List<CreatorWithdrawRequest>> getCreatorWithdrawRequestsByStatus(@PathVariable String status) {
        List<CreatorWithdrawRequest> requests = creatorWithdrawRequestService.getCreatorWithdrawRequestsByStatus(status);
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }

    // 根据用户ID和状态查询创作者提现请求
    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<CreatorWithdrawRequest>> getCreatorWithdrawRequestsByUserIdAndStatus(
            @PathVariable Long userId, @PathVariable String status) {
        List<CreatorWithdrawRequest> requests = creatorWithdrawRequestService.getCreatorWithdrawRequestsByUserIdAndStatus(userId, status);
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }
}