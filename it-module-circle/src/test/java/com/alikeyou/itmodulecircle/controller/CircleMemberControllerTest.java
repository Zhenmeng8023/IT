package com.alikeyou.itmodulecircle.controller;

import com.alikeyou.itmodulecircle.dto.CircleMemberResponse;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.entity.CircleMember;
import com.alikeyou.itmodulecircle.service.CircleMemberService;
import com.alikeyou.itmodulecircle.service.CircleService;
import com.alikeyou.itmodulecommon.constant.LoginConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CircleMemberControllerTest {

    @Mock
    private CircleMemberService circleMemberService;
    @Mock
    private CircleService circleService;

    @InjectMocks
    private CircleMemberController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        LoginConstant.clearUserInfo();
    }

    @AfterEach
    void tearDown() {
        LoginConstant.clearUserInfo();
    }

    @Test
    void joinCircleShouldUseCurrentLoggedInUser() throws Exception {
        LoginConstant.setUserId(18L);
        Circle circle = new Circle();
        circle.setId(6L);
        CircleMember member = buildMember(6L, 18L, "member");
        CircleMemberResponse response = buildResponse(6L, 18L, "member");

        when(circleService.requirePublicVisibleCircle(6L)).thenReturn(circle);
        when(circleMemberService.joinCircle(6L, 18L)).thenReturn(member);
        when(circleMemberService.convertToResponse(member)).thenReturn(response);

        mockMvc.perform(post("/api/circle/6/join")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.circleId").value(6))
                .andExpect(jsonPath("$.userId").value(18))
                .andExpect(jsonPath("$.role").value("member"));

        verify(circleMemberService).joinCircle(6L, 18L);
    }

    @Test
    void leaveCircleShouldUseCurrentLoggedInUser() throws Exception {
        LoginConstant.setUserId(18L);
        doNothing().when(circleMemberService).leaveCircle(6L, 18L);

        mockMvc.perform(post("/api/circle/6/leave")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(circleMemberService).leaveCircle(6L, 18L);
    }

    private CircleMember buildMember(Long circleId, Long userId, String role) {
        Circle circle = new Circle();
        circle.setId(circleId);

        com.alikeyou.itmodulecommon.entity.UserInfo user = new com.alikeyou.itmodulecommon.entity.UserInfo();
        user.setId(userId);
        user.setUsername("user-" + userId);
        user.setNickname("昵称-" + userId);
        user.setAvatarUrl("avatar.png");

        CircleMember member = new CircleMember();
        member.setCircle(circle);
        member.setUser(user);
        member.setRole(role);
        member.setStatus("active");
        member.setJoinTime(Instant.parse("2026-04-18T00:00:00Z"));
        return member;
    }

    private CircleMemberResponse buildResponse(Long circleId, Long userId, String role) {
        CircleMemberResponse response = new CircleMemberResponse();
        response.setCircleId(circleId);
        response.setUserId(userId);
        response.setUsername("user-" + userId);
        response.setRole(role);
        response.setStatus("active");
        return response;
    }
}
