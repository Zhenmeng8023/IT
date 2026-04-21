package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.UserInfoLiteRepository;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/project/member/user")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "项目成员用户搜索")
public class ProjectMemberUserController {

    private final UserInfoLiteRepository userInfoLiteRepository;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/search")
    @Operation(summary = "按昵称或用户名搜索可添加的用户")
    public ResponseEntity<ApiResponse<List<UserInfoLite>>> searchUsers(@RequestParam String keyword,
                                                                       @RequestParam(defaultValue = "10") Integer size,
                                                                       HttpServletRequest request) {
        currentUserProvider.getCurrentUserIdRequired(request);
        String searchKeyword = keyword == null ? "" : keyword.trim();
        int pageSize = Math.max(1, Math.min(size == null ? 10 : size, 20));
        return ResponseEntity.ok(ApiResponse.ok(userInfoLiteRepository.searchByKeyword(searchKeyword, PageRequest.of(0, pageSize))));
    }
}
