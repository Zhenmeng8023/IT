//package com.alikeyou.itmodulepayment.service.impl;
//
//import com.alikeyou.itmodulecommon.entity.UserInfo;
//import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
//import com.alikeyou.itmodulepayment.dto.MembershipDTO;
//import com.alikeyou.itmodulepayment.entity.MembershipLevel;
//import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
//import com.alikeyou.itmodulepayment.service.MembershipService;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.Optional;
//
//@Service
//public class MembershipServiceImpl implements MembershipService {
//
//    private final UserInfoRepository userInfoRepository;
//    private final MembershipLevelRepository membershipLevelRepository;
//
//    public MembershipServiceImpl(UserInfoRepository userInfoRepository,
//                                  MembershipLevelRepository membershipLevelRepository) {
//        this.userInfoRepository = userInfoRepository;
//        this.membershipLevelRepository = membershipLevelRepository;
//    }
//
//    @Override
//    public Boolean checkUserIsVip(Long userId) {
//        if (userId == null) {
//            return false;
//        }
//
//        // 直接从 user_info 表查询 is_premium_member 字段
//        Optional<UserInfo> userOpt = userInfoRepository.findById(userId);
//        if (userOpt.isPresent()) {
//            UserInfo user = userOpt.get();
//            System.out.println("=== checkUserIsVip ===");
//            System.out.println("userId: " + userId);
//            System.out.println("isPremiumMember: " + user.getIsPremiumMember());
//            System.out.println("premiumExpiryDate: " + user.getPremiumExpiryDate());
//
//            // 判断是否为会员
//            if (Boolean.TRUE.equals(user.getIsPremiumMember())) {
//                // 检查是否过期
//                if (user.getPremiumExpiryDate() != null) {
//                    LocalDateTime expiryDateTime = LocalDateTime.ofInstant(
//                        user.getPremiumExpiryDate(), ZoneId.systemDefault());
//                    System.out.println("expiryDateTime: " + expiryDateTime);
//                    System.out.println("now: " + LocalDateTime.now());
//                    System.out.println("is after: " + expiryDateTime.isAfter(LocalDateTime.now()));
//
//                    if (expiryDateTime.isAfter(LocalDateTime.now())) {
//                        return true;
//                    } else {
//                        System.out.println("会员已过期");
//                        return false;
//                    }
//                } else {
//                    // 如果没有设置过期时间，直接返回 true
//                    System.out.println("永久会员");
//                    return true;
//                }
//            } else {
//                System.out.println("不是会员");
//            }
//        }
//
//        return false;
//    }
//
//    @Override
//    public Optional<MembershipDTO> getUserMembershipInfo(Long userId) {
//        if (userId == null) {
//            return Optional.empty();
//        }
//
//        // 从 user_info 表查询会员信息
//        Optional<UserInfo> userOpt = userInfoRepository.findById(userId);
//        if (userOpt.isEmpty()) {
//            return Optional.empty();
//        }
//
//        UserInfo user = userOpt.get();
//        boolean isVip = Boolean.TRUE.equals(user.getIsPremiumMember());
//
//        // 检查是否过期
//        boolean isExpired = false;
//        if (user.getPremiumExpiryDate() != null) {
//            LocalDateTime expiryDateTime = LocalDateTime.ofInstant(
//                user.getPremiumExpiryDate(), ZoneId.systemDefault());
//            isExpired = expiryDateTime.isBefore(LocalDateTime.now());
//        }
//
//        MembershipDTO dto = new MembershipDTO();
//        dto.setUserId(userId);
//        dto.setIsVip(isVip && !isExpired);
//        dto.setStatus(isVip ? (isExpired ? "expired" : "active") : "none");
//
//        // 如果有过期时间，设置到 DTO 中
//        if (user.getPremiumExpiryDate() != null) {
//            dto.setEndTime(LocalDateTime.ofInstant(
//                user.getPremiumExpiryDate(), ZoneId.systemDefault()));
//        }
//
//        return Optional.of(dto);
//    }
//
//    @Override
//    public Optional<MembershipDTO> getUserActiveMembership(Long userId) {
//        if (userId == null) {
//            return Optional.empty();
//        }
//
//        // 从 user_info 表查询会员信息
//        Optional<UserInfo> userOpt = userInfoRepository.findById(userId);
//        if (userOpt.isEmpty()) {
//            return Optional.empty();
//        }
//
//        UserInfo user = userOpt.get();
//
//        // 检查是否为会员
//        if (!Boolean.TRUE.equals(user.getIsPremiumMember())) {
//            return Optional.empty();
//        }
//
//        // 检查是否过期
//        if (user.getPremiumExpiryDate() != null) {
//            LocalDateTime expiryDateTime = LocalDateTime.ofInstant(
//                user.getPremiumExpiryDate(), ZoneId.systemDefault());
//            if (expiryDateTime.isBefore(LocalDateTime.now())) {
//                return Optional.empty();
//            }
//        }
//
//        MembershipDTO dto = new MembershipDTO();
//        dto.setUserId(userId);
//        dto.setIsVip(true);
//        dto.setStatus("active");
//
//        // 设置过期时间
//        if (user.getPremiumExpiryDate() != null) {
//            dto.setEndTime(LocalDateTime.ofInstant(
//                user.getPremiumExpiryDate(), ZoneId.systemDefault()));
//        }
//
//        return Optional.of(dto);
//    }
//}
