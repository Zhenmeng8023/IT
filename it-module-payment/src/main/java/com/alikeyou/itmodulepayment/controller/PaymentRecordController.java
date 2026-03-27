package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.dto.PaymentRecordDTO;
import com.alikeyou.itmodulepayment.entity.PaymentRecord;
import com.alikeyou.itmodulepayment.service.PaymentRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-records")
public class PaymentRecordController {

    private final PaymentRecordService paymentRecordService;

    public PaymentRecordController(PaymentRecordService paymentRecordService) {
        this.paymentRecordService = paymentRecordService;
    }

    // 创建支付记录
    @PostMapping
    public ResponseEntity<PaymentRecord> createPaymentRecord(@RequestBody PaymentRecordDTO dto) {
        PaymentRecord paymentRecord = paymentRecordService.createPaymentRecord(dto);
        return new ResponseEntity<>(paymentRecord, HttpStatus.CREATED);
    }

    // 更新支付记录
    @PutMapping("/{id}")
    public ResponseEntity<PaymentRecord> updatePaymentRecord(@PathVariable Long id, @RequestBody PaymentRecordDTO dto) {
        PaymentRecord paymentRecord = paymentRecordService.updatePaymentRecord(id, dto);
        return new ResponseEntity<>(paymentRecord, HttpStatus.OK);
    }

    // 删除支付记录
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaymentRecord(@PathVariable Long id) {
        paymentRecordService.deletePaymentRecord(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // 根据ID查询支付记录
    @GetMapping("/{id}")
    public ResponseEntity<PaymentRecord> getPaymentRecordById(@PathVariable Long id) {
        PaymentRecord paymentRecord = paymentRecordService.getPaymentRecordById(id);
        return new ResponseEntity<>(paymentRecord, HttpStatus.OK);
    }

    // 根据订单ID查询支付记录
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentRecord>> getPaymentRecordsByOrderId(@PathVariable Long orderId) {
        List<PaymentRecord> paymentRecords = paymentRecordService.getPaymentRecordsByOrderId(orderId);
        return new ResponseEntity<>(paymentRecords, HttpStatus.OK);
    }

    // 根据支付平台查询支付记录
    @GetMapping("/platform/{paymentPlatform}")
    public ResponseEntity<List<PaymentRecord>> getPaymentRecordsByPaymentPlatform(@PathVariable String paymentPlatform) {
        List<PaymentRecord> paymentRecords = paymentRecordService.getPaymentRecordsByPaymentPlatform(paymentPlatform);
        return new ResponseEntity<>(paymentRecords, HttpStatus.OK);
    }

    // 根据支付状态查询支付记录
    @GetMapping("/status/{paymentStatus}")
    public ResponseEntity<List<PaymentRecord>> getPaymentRecordsByPaymentStatus(@PathVariable String paymentStatus) {
        List<PaymentRecord> paymentRecords = paymentRecordService.getPaymentRecordsByPaymentStatus(paymentStatus);
        return new ResponseEntity<>(paymentRecords, HttpStatus.OK);
    }
}