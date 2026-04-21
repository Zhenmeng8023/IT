package com.alikeyou.itmodulecommon.service;

import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.entity.Menu;
import com.alikeyou.itmodulecommon.entity.Role;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.MenuRepository;
import com.alikeyou.itmodulecommon.repository.RoleRepository;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulecommon.utils.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserInfoService {

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MenuRepository menuRepository;

    public Optional<UserInfo> getUserById(Long id) {
        return userInfoRepository.findByIdWithAssociations(id);
    }

    public Optional<UserInfo> getUserByUsername(String username) {
        return userInfoRepository.findByUsername(username);
    }

    public Optional<UserInfo> getUserByEmail(String email) {
        return userInfoRepository.findByEmail(email);
    }

    public Optional<UserInfo> getCurrentUser() {
        Long userId = LoginConstant.getUserId();
        if (userId != null) {
            return getUserById(userId);
        }

        String username = LoginConstant.getUsername();
        if (username != null) {
            return getUserByUsername(username);
        }

        String email = LoginConstant.getEmail();
        if (email != null) {
            return getUserByEmail(email);
        }

        return Optional.empty();
    }

    public UserInfo saveUser(UserInfo userInfo) {
        return userInfoRepository.save(userInfo);
    }

    public UserInfo updateUser(UserInfo userInfo) {
        if (userInfo == null || userInfo.getId() == null) {
            throw new IllegalArgumentException("User id is required for update");
        }

        UserInfo existingUser = userInfoRepository.findById(userInfo.getId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userInfo.getId()));

        mergeUpdatableFields(existingUser, userInfo);
        return userInfoRepository.save(existingUser);
    }

    private void mergeUpdatableFields(UserInfo target, UserInfo source) {
        if (source.getUsername() != null) target.setUsername(source.getUsername());
        if (source.getEmail() != null) target.setEmail(source.getEmail());
        if (source.getPhone() != null) target.setPhone(source.getPhone());
        if (source.getGender() != null) target.setGender(source.getGender());
        if (source.getBirthday() != null) target.setBirthday(source.getBirthday());
        if (source.getStatus() != null) target.setStatus(source.getStatus());
        if (source.getIdentityCard() != null) target.setIdentityCard(source.getIdentityCard());
        if (source.getLastActiveAt() != null) target.setLastActiveAt(source.getLastActiveAt());
        if (source.getLastLoginAt() != null) target.setLastLoginAt(source.getLastLoginAt());
        if (source.getLoginCount() != null) target.setLoginCount(source.getLoginCount());
        if (source.getAvatarUrl() != null) target.setAvatarUrl(source.getAvatarUrl());
        if (source.getBio() != null) target.setBio(source.getBio());
        if (source.getNickname() != null) target.setNickname(source.getNickname());
        if (source.getIsPremiumMember() != null) target.setIsPremiumMember(source.getIsPremiumMember());
        if (source.getPremiumExpiryDate() != null) target.setPremiumExpiryDate(source.getPremiumExpiryDate());
        if (source.getRoleId() != null) target.setRoleId(source.getRoleId());
        if (source.getBalance() != null) target.setBalance(source.getBalance());
        if (source.getRegion() != null) target.setRegion(source.getRegion());
        if (source.getAuthorTag() != null) target.setAuthorTag(source.getAuthorTag());
        if (source.getCreatedAt() != null) target.setCreatedAt(source.getCreatedAt());
        if (source.getPasswordHash() != null && !source.getPasswordHash().isBlank()) {
            target.setPasswordHash(PasswordEncoder.encode(source.getPasswordHash()));
        }
    }

    public void deleteUser(Long id) {
        userInfoRepository.deleteById(id);
    }

    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isEmpty()) {
            return false;
        }

        UserInfo user = userInfo.get();
        if (!PasswordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return false;
        }

        String encodedNewPassword = PasswordEncoder.encode(newPassword);
        user.setPasswordHash(encodedNewPassword);
        userInfoRepository.save(user);
        LoginConstant.setPassword(encodedNewPassword);
        return true;
    }

    public boolean changeEmail(Long userId, String newEmail) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isEmpty()) {
            return false;
        }
        UserInfo user = userInfo.get();
        user.setEmail(newEmail);
        userInfoRepository.save(user);
        LoginConstant.setEmail(newEmail);
        return true;
    }

    public boolean changeUsername(Long userId, String newUsername) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isEmpty()) {
            return false;
        }
        UserInfo user = userInfo.get();
        user.setUsername(newUsername);
        userInfoRepository.save(user);
        LoginConstant.setUsername(newUsername);
        return true;
    }

    public boolean verifyPassword(Long userId, String password) {
        Optional<UserInfo> userInfo = getUserById(userId);
        return userInfo.filter(user -> PasswordEncoder.matches(password, user.getPasswordHash())).isPresent();
    }

    public boolean bindThirdParty(Long userId, String thirdPartyType, String thirdPartyId) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isEmpty()) {
            return false;
        }
        // Placeholder: keep compatibility for current API until 3rd-party account fields are modeled.
        userInfoRepository.save(userInfo.get());
        return true;
    }

    public Optional<UserInfo> getPublicUserInfo(Long userId) {
        return userInfoRepository.findByIdWithAssociations(userId);
    }

    public List<UserInfo> getAllUsers() {
        return userInfoRepository.findAllWithAssociations();
    }

    public Page<UserInfo> getUsersPage(Pageable pageable) {
        return userInfoRepository.findAll(pageable);
    }

    public Page<UserInfo> getUsersPage(String username, String email, String phone, String status, Integer roleId, Pageable pageable) {
        return userInfoRepository.searchAdminUsers(username, email, phone, status, roleId, pageable);
    }

    public void batchDeleteUsers(List<Long> userIds) {
        userInfoRepository.deleteAllById(userIds);
    }

    @Transactional
    public int batchUpdateUserStatus(List<Long> userIds, String status) {
        if (userIds == null || userIds.isEmpty() || status == null || status.isBlank()) {
            return 0;
        }

        List<UserInfo> users = userInfoRepository.findAllById(userIds);
        users.forEach(user -> user.setStatus(status));
        userInfoRepository.saveAll(users);
        return users.size();
    }

    public boolean resetPassword(Long userId, String newPassword) {
        Optional<UserInfo> userInfo = getUserById(userId);
        if (userInfo.isEmpty()) {
            return false;
        }
        UserInfo user = userInfo.get();
        user.setPasswordHash(PasswordEncoder.encode(newPassword));
        userInfoRepository.save(user);
        return true;
    }

    public void assignRoles(Long userId, List<Integer> roleIds) {
        Optional<UserInfo> userOptional = userInfoRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        UserInfo user = userOptional.get();
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Integer roleId = roleIds.get(0);
        Optional<Role> roleOptional = roleRepository.findById(roleId);
        if (roleOptional.isEmpty()) {
            throw new RuntimeException("Role not found with id: " + roleId);
        }
        user.setRoleId(roleId);
        userInfoRepository.save(user);
    }

    public List<Role> getUserRoles(Long userId) {
        Optional<UserInfo> userOptional = userInfoRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        Integer roleId = userOptional.get().getRoleId();
        if (roleId == null) {
            return List.of();
        }
        return roleRepository.findById(roleId).map(List::of).orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public List<Menu> getUserMenus(Long userId) {
        Optional<UserInfo> userOptional = userInfoRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        Integer roleId = userOptional.get().getRoleId();
        if (roleId == null) {
            return List.of();
        }
        Optional<Role> roleOptional = roleRepository.findById(roleId);
        return roleOptional.map(role -> buildAccessibleAdminMenus(new ArrayList<>(role.getMenus())))
                .orElseGet(List::of);
    }

    private List<Menu> buildAccessibleAdminMenus(List<Menu> assignedMenus) {
        if (assignedMenus == null || assignedMenus.isEmpty()) {
            return List.of();
        }

        List<Menu> visibleAdminMenus = menuRepository.findVisibleAdminMenus();
        Map<Integer, Menu> visibleAdminMenuById = visibleAdminMenus.stream()
                .filter(menu -> menu.getId() != null)
                .collect(Collectors.toMap(Menu::getId, Function.identity(), (left, right) -> left));

        Set<Integer> accessibleMenuIds = assignedMenus.stream()
                .filter(this::isVisibleAdminMenu)
                .map(Menu::getId)
                .filter(id -> id != null && visibleAdminMenuById.containsKey(id))
                .collect(Collectors.toCollection(HashSet::new));

        Set<Integer> resultIds = new HashSet<>(accessibleMenuIds);
        for (Integer menuId : accessibleMenuIds) {
            Menu menu = visibleAdminMenuById.get(menuId);
            while (menu != null && menu.getParentId() != null) {
                Menu parent = visibleAdminMenuById.get(menu.getParentId());
                if (parent == null || parent.getId() == null) {
                    break;
                }
                resultIds.add(parent.getId());
                menu = parent;
            }
        }

        return visibleAdminMenus.stream()
                .filter(menu -> menu.getId() != null && resultIds.contains(menu.getId()))
                .sorted(menuComparator())
                .collect(Collectors.toList());
    }

    private boolean isVisibleAdminMenu(Menu menu) {
        return menu != null
                && menu.getId() != null
                && Boolean.FALSE.equals(menu.getIsHidden())
                && menu.getPath() != null
                && menu.getPath().startsWith("/admin")
                && "menu".equals(menu.getType());
    }

    private Comparator<Menu> menuComparator() {
        return Comparator
                .comparing((Menu menu) -> menu.getSortOrder() == null ? Integer.MAX_VALUE : menu.getSortOrder())
                .thenComparing(menu -> menu.getId() == null ? Integer.MAX_VALUE : menu.getId());
    }
}
