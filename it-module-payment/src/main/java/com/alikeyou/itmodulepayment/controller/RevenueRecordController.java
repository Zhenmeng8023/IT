package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.dto.RevenueRecordDTO;
import com.alikeyou.itmodulepayment.entity.RevenueRecord;
import com.alikeyou.itmodulepayment.service.RevenueRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/revenue-records")
public class RevenueRecordController {

    private final RevenueRecordService revenueRecordService;

    public RevenueRecordController(RevenueRecordService revenueRecordService) {
        this.revenueRecordService = revenueRecordService;
    }

    // 创建收益记录
    @PostMapping
    public ResponseEntity<RevenueRecord> createRevenueRecord(@RequestBody RevenueRecordDTO dto) {
        RevenueRecord revenueRecord = revenueRecordService.createRevenueRecord(dto);
        return new ResponseEntity<>(revenueRecord, HttpStatus.CREATED);
    }

    // 更新收益记录
    @PutMapping("/{id}")
    public ResponseEntity<RevenueRecord> updateRevenueRecord(@PathVariable Long id, @RequestBody RevenueRecordDTO dto) {
        RevenueRecord revenueRecord = revenueRecordService.updateRevenueRecord(id, dto);
        return new ResponseEntity<>(revenueRecord, HttpStatus.OK);
    }

    // 删除收益记录
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRevenueRecord(@PathVariable Long id) {
        revenueRecordService.deleteRevenueRecord(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // 根据ID查询收益记录
    @GetMapping("/{id}")
    public ResponseEntity<RevenueRecord> getRevenueRecordById(@PathVariable Long id) {
        RevenueRecord revenueRecord = revenueRecordService.getRevenueRecordById(id);
        return new ResponseEntity<>(revenueRecord, HttpStatus.OK);
    }

    // 根据订单ID查询收益记录
    @GetMapping("/order/{orderId}")
    public ResponseEntity<RevenueRecord> getRevenueRecordByOrderId(@PathVariable Long orderId) {
        RevenueRecord revenueRecord = revenueRecordService.getRevenueRecordByOrderId(orderId);
        return new ResponseEntity<>(revenueRecord, HttpStatus.OK);
    }

    // 根据来源用户ID查询收益记录
    @GetMapping("/source-user/{sourceUserId}")
    public ResponseEntity<List<RevenueRecord>> getRevenueRecordsBySourceUserId(@PathVariable Long sourceUserId) {
        List<RevenueRecord> revenueRecords = revenueRecordService.getRevenueRecordsBySourceUserId(sourceUserId);
        return new ResponseEntity<>(revenueRecords, HttpStatus.OK);
    }

    // 根据结算状态查询收益记录
    @GetMapping("/settlement-status/{settlementStatus}")
    public ResponseEntity<List<RevenueRecord>> getRevenueRecordsBySettlementStatus(@PathVariable String settlementStatus) {
        List<RevenueRecord> revenueRecords = revenueRecordService.getRevenueRecordsBySettlementStatus(settlementStatus);
        return new ResponseEntity<>(revenueRecords, HttpStatus.OK);
    }
}