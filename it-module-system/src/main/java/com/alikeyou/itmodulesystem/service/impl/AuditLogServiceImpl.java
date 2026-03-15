package com.alikeyou.itmodulesystem.service.impl;

import com.alikeyou.itmodulesystem.entity.AuditLog;
import com.alikeyou.itmodulesystem.repository.AuditLogRepository;
import com.alikeyou.itmodulesystem.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public AuditLog logAction(String action, String targetType, Long targetId,
                              String ipAddress, String userAgent, Map<String, Object> details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setDetails(details);
        return auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional
    public AuditLog saveAuditLog(AuditLog auditLog) {
        return auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuditLog> getAuditLogById(Long id) {
        return auditLogRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByAction(String action) {
        return auditLogRepository.findByAction(action);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByTarget(String targetType, Long targetId) {
        return auditLogRepository.findByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByTimeRange(Instant startTime, Instant endTime) {
        return auditLogRepository.findByCreatedAtBetween(startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> searchAuditLogs(String action, String targetType,
                                          Instant startTime, Instant endTime) {
        return auditLogRepository.searchAuditLogs(action, targetType, startTime, endTime);
    }

    @Override
    @Transactional
    public void deleteAuditLog(Long id) {
        auditLogRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAction(String action) {
        return auditLogRepository.countByAction(action);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByTargetType(String targetType) {
        return auditLogRepository.countByTargetType(targetType);
    }
}
