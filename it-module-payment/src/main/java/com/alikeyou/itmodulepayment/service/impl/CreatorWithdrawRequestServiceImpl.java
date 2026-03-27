package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.dto.CreatorWithdrawRequestDTO;
import com.alikeyou.itmodulepayment.entity.CreatorWithdrawRequest;
import com.alikeyou.itmodulepayment.repository.CreatorWithdrawRequestRepository;
import com.alikeyou.itmodulepayment.service.CreatorWithdrawRequestService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CreatorWithdrawRequestServiceImpl implements CreatorWithdrawRequestService {

    private final CreatorWithdrawRequestRepository creatorWithdrawRequestRepository;

    public CreatorWithdrawRequestServiceImpl(CreatorWithdrawRequestRepository creatorWithdrawRequestRepository) {
        this.creatorWithdrawRequestRepository = creatorWithdrawRequestRepository;
    }

    @Override
    public CreatorWithdrawRequest createCreatorWithdrawRequest(CreatorWithdrawRequestDTO dto) {
        CreatorWithdrawRequest request = new CreatorWithdrawRequest();
        BeanUtils.copyProperties(dto, request);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        return creatorWithdrawRequestRepository.save(request);
    }

    @Override
    public CreatorWithdrawRequest updateCreatorWithdrawRequest(Long id, CreatorWithdrawRequestDTO dto) {
        CreatorWithdrawRequest request = creatorWithdrawRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("提现请求不存在"));
        BeanUtils.copyProperties(dto, request);
        request.setUpdatedAt(LocalDateTime.now());
        return creatorWithdrawRequestRepository.save(request);
    }

    @Override
    public void deleteCreatorWithdrawRequest(Long id) {
        creatorWithdrawRequestRepository.deleteById(id);
    }

    @Override
    public CreatorWithdrawRequest getCreatorWithdrawRequestById(Long id) {
        return creatorWithdrawRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("提现请求不存在"));
    }

    @Override
    public List<CreatorWithdrawRequest> getCreatorWithdrawRequestsByUserId(Long userId) {
        return creatorWithdrawRequestRepository.findByUserId(userId);
    }

    @Override
    public List<CreatorWithdrawRequest> getCreatorWithdrawRequestsByStatus(String status) {
        return creatorWithdrawRequestRepository.findByStatus(status);
    }

    @Override
    public List<CreatorWithdrawRequest> getCreatorWithdrawRequestsByUserIdAndStatus(Long userId, String status) {
        return creatorWithdrawRequestRepository.findByUserIdAndStatus(userId, status);
    }
}