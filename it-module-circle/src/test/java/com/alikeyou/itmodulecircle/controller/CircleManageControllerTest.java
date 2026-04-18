package com.alikeyou.itmodulecircle.controller;

import com.alikeyou.itmodulecircle.dto.CircleCreatorInfo;
import com.alikeyou.itmodulecircle.dto.CircleMemberResponse;
import com.alikeyou.itmodulecircle.dto.CircleResponse;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.entity.CircleComment;
import com.alikeyou.itmodulecircle.entity.CircleMember;
import com.alikeyou.itmodulecircle.service.CircleCommentService;
import com.alikeyou.itmodulecircle.service.CircleMemberService;
import com.alikeyou.itmodulecircle.service.CircleService;
import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CircleManageControllerTest {

    @Mock
    private CircleService circleService;

    @Mock
    private CircleMemberService circleMemberService;

    @Mock
    private CircleCommentService circleCommentService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CircleManageController circleManageController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(circleManageController).build();

        LoginConstant.setUserId(1L);
        LoginConstant.setRoleId(1);
        LoginConstant.setUsername("admin");
    }

    @AfterEach
    void tearDown() {
        LoginConstant.clearUserInfo();
    }

    @Test
    void toggleRecommendShouldReturnNotImplemented() throws Exception {
        mockMvc.perform(put("/api/circle/manage/toggle-recommend/1"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.code").value(501))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void listShouldReturnWrappedPageContract() throws Exception {
        Circle circle = buildCircle(100L, "approved");
        when(circleService.getAllCircles()).thenReturn(List.of(circle));
        when(circleService.convertToResponse(any(Circle.class))).thenReturn(buildCircleResponse(circle));

        mockMvc.perform(get("/api/circle/manage/list")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.currentPage").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.list[0].type").value("approved"))
                .andExpect(jsonPath("$.data.list[0].status").value("normal"));
    }

    @Test
    void listShouldSupportPageSizeAndPrivacyAlias() throws Exception {
        Circle publicCircle = buildCircle(101L, "approved");
        Circle privateCircle = buildCircle(102L, "approved");
        privateCircle.setVisibility("private");

        when(circleService.getAllCircles()).thenReturn(List.of(publicCircle, privateCircle));
        when(circleService.convertToResponse(any(Circle.class)))
                .thenAnswer(invocation -> buildCircleResponse(invocation.getArgument(0)));

        mockMvc.perform(get("/api/circle/manage/list")
                        .param("page", "1")
                        .param("pageSize", "1")
                        .param("privacy", "public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(101))
                .andExpect(jsonPath("$.data.list[0].privacy").value("public"));
    }

    @Test
    void setAdminShouldSupportPutAndReturnUnifiedContract() throws Exception {
        CircleMember member = buildMember(11L, "member");
        CircleMember updatedMember = buildMember(11L, "admin");
        CircleMemberResponse updatedResponse = buildMemberResponse(11L, "admin");

        when(circleMemberService.getMemberById(11L)).thenReturn(Optional.of(member));
        when(circleMemberService.setMemberRoleByMemberId(11L, "admin")).thenReturn(updatedMember);
        when(circleMemberService.convertToResponse(updatedMember)).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/circle/manage/set-admin/11")
                        .param("role", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.role").value("admin"));
    }

    @Test
    void removeMemberShouldSupportPostFallback() throws Exception {
        CircleMember member = buildMember(12L, "member");
        when(circleMemberService.getMemberById(12L)).thenReturn(Optional.of(member));
        doNothing().when(circleMemberService).removeMemberByMemberId(12L);

        mockMvc.perform(post("/api/circle/manage/remove-member/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.removed").value(true))
                .andExpect(jsonPath("$.data.id").value(12));
    }

    @Test
    void postsShouldReturnManagePostSchema() throws Exception {
        CircleComment post = buildPost(21L, 3L, null);
        when(circleCommentService.getPostsByCircleId(3L)).thenReturn(List.of(post));
        when(circleCommentService.countRepliesByPostId(21L)).thenReturn(2L);

        mockMvc.perform(get("/api/circle/manage/posts/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(21))
                .andExpect(jsonPath("$.data[0].status").value("pending"))
                .andExpect(jsonPath("$.data[0].commentCount").value(2));
    }

    @Test
    void approvePostShouldSupportPostMethod() throws Exception {
        CircleComment post = buildPost(22L, 3L, "published");
        when(circleCommentService.approvePost(22L)).thenReturn(post);
        when(circleCommentService.countRepliesByPostId(22L)).thenReturn(0L);

        mockMvc.perform(post("/api/circle/manage/approve-post/22")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(22))
                .andExpect(jsonPath("$.data.status").value("published"));
    }

    private Circle buildCircle(Long id, String type) {
        Circle circle = new Circle();
        circle.setId(id);
        circle.setName("测试圈子");
        circle.setDescription("desc");
        circle.setType(type);
        circle.setVisibility("public");
        circle.setMaxMembers(500);
        circle.setCreatorId(9L);
        circle.setCreatedAt(Instant.parse("2026-04-18T00:00:00Z"));
        circle.setUpdatedAt(Instant.parse("2026-04-18T01:00:00Z"));
        return circle;
    }

    private CircleResponse buildCircleResponse(Circle circle) {
        CircleResponse response = new CircleResponse();
        response.setId(circle.getId());
        response.setName(circle.getName());
        response.setDescription(circle.getDescription());
        response.setType(circle.getType());
        response.setVisibility(circle.getVisibility());
        response.setMaxMembers(circle.getMaxMembers());
        response.setCreatedAt(circle.getCreatedAt());
        response.setUpdatedAt(circle.getUpdatedAt());
        response.setMemberCount(10L);
        response.setActiveMemberCount(8L);
        response.setPostCount(6L);

        CircleCreatorInfo creatorInfo = new CircleCreatorInfo();
        creatorInfo.setId(9L);
        creatorInfo.setUsername("creator");
        creatorInfo.setAvatar("avatar.png");
        response.setCreator(creatorInfo);
        return response;
    }

    private CircleMember buildMember(Long id, String role) {
        Circle circle = new Circle();
        circle.setId(3L);

        UserInfo user = new UserInfo();
        user.setId(20L);
        user.setUsername("member-user");
        user.setNickname("成员");
        user.setAvatarUrl("avatar.png");

        CircleMember member = new CircleMember();
        member.setId(id);
        member.setCircle(circle);
        member.setUser(user);
        member.setRole(role);
        member.setStatus("active");
        member.setJoinTime(Instant.parse("2026-04-18T02:00:00Z"));
        return member;
    }

    private CircleMemberResponse buildMemberResponse(Long id, String role) {
        CircleMemberResponse response = new CircleMemberResponse();
        response.setId(id);
        response.setCircleId(3L);
        response.setUserId(20L);
        response.setUsername("member-user");
        response.setNickname("成员");
        response.setAvatarUrl("avatar.png");
        response.setJoinTime(Instant.parse("2026-04-18T02:00:00Z"));
        response.setLastActive(Instant.parse("2026-04-18T02:00:00Z"));
        response.setStatus("active");
        response.setRole(role);
        return response;
    }

    private CircleComment buildPost(Long id, Long circleId, String status) {
        UserInfo author = new UserInfo();
        author.setId(30L);
        author.setUsername("author");
        author.setNickname("作者");
        author.setAvatarUrl("author.png");

        CircleComment post = new CircleComment();
        post.setId(id);
        post.setCircleId(circleId);
        post.setPostId(id);
        post.setParentCommentId(null);
        post.setAuthor(author);
        post.setContent("这是一个帖子内容");
        post.setStatus(status);
        post.setLikes(3);
        post.setCreatedAt(Instant.parse("2026-04-18T03:00:00Z"));
        return post;
    }
}
