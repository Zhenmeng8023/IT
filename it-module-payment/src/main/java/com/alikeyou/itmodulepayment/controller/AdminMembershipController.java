package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulepayment.dto.MembershipDTO;
import com.alikeyou.itmodulepayment.entity.Membership;
import com.alikeyou.itmodulepayment.entity.MembershipLevel;
import com.alikeyou.itmodulepayment.pojo.Result;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.repository.MembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 后台会员管理 Controller
 * 提供会员的查询、状态管理、手动开通/续费等功能
 */
@RestController
@RequestMapping("/api/admin/memberships")
@PreAuthorize("@authorizationGuard.canManageUsers()")
public class AdminMembershipController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminMembershipController.class);
    
    private final MembershipRepository membershipRepository;
    private final MembershipLevelRepository membershipLevelRepository;
    private final UserInfoRepository userInfoRepository;
    
    public AdminMembershipController(MembershipRepository membershipRepository,
                                    MembershipLevelRepository membershipLevelRepository,
                                    UserInfoRepository userInfoRepository) {
        this.membershipRepository = membershipRepository;
        this.membershipLevelRepository = membershipLevelRepository;
        this.userInfoRepository = userInfoRepository;
    }
    
    /**
     * 分页查询所有会员记录
     */
    @GetMapping("/page")
    public ResponseEntity<Map<String, Object>> getMembershipsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId) {
        
        try {
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Membership> membershipPage;
            
            if (userId != null) {
                // 按用户ID筛选
                membershipPage = membershipRepository.findByUserId(userId, pageable);
            } else if (status != null && !status.isEmpty()) {
                // 按状态筛选
                membershipPage = membershipRepository.findByStatus(status, pageable);
            } else {
                // 查询所有
                membershipPage = membershipRepository.findAll(pageable);
            }
            
            // 转换为 DTO（包含等级名称）
            List<MembershipDTO> dtoList = membershipPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dtoList);
            response.put("total", membershipPage.getTotalElements());
            response.put("totalPages", membershipPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询会员列表失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 根据ID查询会员详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMembershipDetail(@PathVariable Long id) {
        try {
            Optional<Membership> membershipOpt = membershipRepository.findById(id);
            
            if (membershipOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "会员记录不存在");
                return ResponseEntity.status(404).body(error);
            }
            
            Membership membership = membershipOpt.get();
            MembershipDTO dto = convertToDTO(membership);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dto);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询会员详情失败，会员ID: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 根据用户ID查询会员信息
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getMembershipsByUserId(@PathVariable Long userId) {
        try {
            List<Membership> memberships = membershipRepository.findByUserIdOrderByCreatedAtDesc(userId);
            List<MembershipDTO> dtoList = memberships.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dtoList);
            response.put("count", dtoList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("查询用户会员记录失败，用户ID: {}", userId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 更新会员状态（后台管理）
     */
    @PutMapping("/{id}/status")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Map<String, Object>> updateMembershipStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        
        try {
            String newStatus = request.get("status");
            if (newStatus == null || newStatus.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "状态不能为空");
                return ResponseEntity.badRequest().body(error);
            }
            
            Membership membership = membershipRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("会员记录不存在"));
            
            String oldStatus = membership.getStatus();
            membership.setStatus(newStatus.toLowerCase());
            membership.setUpdatedAt(LocalDateTime.now());
            
            membershipRepository.save(membership);
            
            logger.info("会员状态更新成功，会员ID: {}, 原状态: {}, 新状态: {}", id, oldStatus, newStatus);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "状态更新成功");
            response.put("membershipId", id);
            response.put("oldStatus", oldStatus);
            response.put("newStatus", newStatus);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新会员状态失败，会员ID: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 手动为用户开通/续费会员（后台管理）
     */
    @PostMapping("/grant")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Map<String, Object>> grantMembership(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long levelId = Long.valueOf(request.get("levelId").toString());
            Integer durationDays = request.get("durationDays") != null 
                    ? Integer.valueOf(request.get("durationDays").toString()) 
                    : null;
            
            // 验证用户是否存在
            UserInfo user = userInfoRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 验证会员等级是否存在
            MembershipLevel level = membershipLevelRepository.findById(levelId)
                    .orElseThrow(() -> new RuntimeException("会员等级不存在"));
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now;
            LocalDateTime endTime;
            
            // 如果指定了天数，使用指定的；否则使用等级的默认天数
            int days = durationDays != null ? durationDays : level.getDurationDays();
            
            // 检查是否有有效会员，有则续费
            Optional<Membership> activeOpt = membershipRepository
                    .findTopByUserIdAndStatusAndEndTimeAfterOrderByEndTimeDesc(userId, "active", now);
            
            Membership membership;
            if (activeOpt.isPresent()) {
                // 续费
                membership = activeOpt.get();
                LocalDateTime currentEnd = membership.getEndTime();
                endTime = currentEnd.isAfter(now) ? currentEnd.plusDays(days) : now.plusDays(days);
                membership.setLevelId(levelId);
                logger.info("会员续费，用户ID: {}, 原到期时间: {}, 新到期时间: {}", userId, currentEnd, endTime);
            } else {
                // 新开通
                membership = new Membership();
                membership.setUserId(userId);
                membership.setLevelId(levelId);
                endTime = now.plusDays(days);
                membership.setCreatedAt(now);
                logger.info("新开通会员，用户ID: {}, 到期时间: {}", userId, endTime);
            }
            
            membership.setStartTime(startTime);
            membership.setEndTime(endTime);
            membership.setStatus("active");
            membership.setUpdatedAt(now);
            
            membershipRepository.save(membership);
            
            // 同步更新 user_info 表
            user.setIsPremiumMember(true);
            user.setPremiumExpiryDate(endTime.atZone(ZoneId.systemDefault()).toInstant());
            userInfoRepository.save(user);
            
            logger.info("✅ 手动开通/续费会员成功，用户ID: {}, 会员等级: {}, 到期时间: {}", 
                    userId, level.getName(), endTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "会员开通/续费成功");
            response.put("membershipId", membership.getId());
            response.put("userId", userId);
            response.put("levelName", level.getName());
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("durationDays", days);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("手动开通会员失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "操作失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 删除会员记录（后台管理）
     * 注意：已激活的会员记录不建议删除
     */
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<Map<String, Object>> deleteMembership(@PathVariable Long id) {
        try {
            Membership membership = membershipRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("会员记录不存在"));
            
            // 检查会员状态
            if ("active".equalsIgnoreCase(membership.getStatus())) {
                Map<String, Object> warning = new HashMap<>();
                warning.put("success", false);
                warning.put("message", "该会员记录处于激活状态，不建议删除。如需取消，请将状态改为 cancelled。");
                return ResponseEntity.badRequest().body(warning);
            }
            
            membershipRepository.deleteById(id);
            
            logger.info("会员记录删除成功，会员ID: {}", id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "会员记录删除成功");
            response.put("membershipId", id);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除会员记录失败，会员ID: {}", id, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 统计会员数据（后台仪表盘）
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getMembershipStatistics() {
        try {
            long totalMemberships = membershipRepository.count();
            long activeMemberships = membershipRepository.countByStatus("active");
            long expiredMemberships = membershipRepository.countByStatus("expired");
            long cancelledMemberships = membershipRepository.countByStatus("cancelled");
            
            // 统计不同等级的会员数量
            List<Object[]> levelStats = membershipRepository.countByLevelId();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", Map.of(
                "totalMemberships", totalMemberships,
                "activeMemberships", activeMemberships,
                "expiredMemberships", expiredMemberships,
                "cancelledMemberships", cancelledMemberships,
                "levelStatistics", levelStats
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取会员统计失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "统计失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 将 Membership 实体转换为 DTO
     */
    private MembershipDTO convertToDTO(Membership membership) {
        MembershipDTO dto = new MembershipDTO();
        dto.setId(membership.getId());
        dto.setUserId(membership.getUserId());
        dto.setLevelId(membership.getLevelId());
        dto.setStartTime(membership.getStartTime());
        dto.setEndTime(membership.getEndTime());
        dto.setStatus(membership.getStatus());
        
        // 获取等级名称
        if (membership.getLevelId() != null) {
            membershipLevelRepository.findById(membership.getLevelId())
                    .ifPresent(level -> dto.setLevelName(level.getName()));
        }
        
        // 判断是否为 VIP
        boolean isVip = "active".equalsIgnoreCase(membership.getStatus()) 
                && membership.getEndTime() != null 
                && membership.getEndTime().isAfter(LocalDateTime.now());
        dto.setIsVip(isVip);
        
        return dto;
    }
}
