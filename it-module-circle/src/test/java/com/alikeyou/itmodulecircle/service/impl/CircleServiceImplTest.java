package com.alikeyou.itmodulecircle.service.impl;

import com.alikeyou.itmodulecircle.dto.CircleCreateRequest;
import com.alikeyou.itmodulecircle.dto.CircleCloseRequest;
import com.alikeyou.itmodulecircle.dto.CircleResponse;
import com.alikeyou.itmodulecircle.dto.CircleUpdateRequest;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.entity.CircleComment;
import com.alikeyou.itmodulecircle.entity.CircleMember;
import com.alikeyou.itmodulecircle.exception.CircleException;
import com.alikeyou.itmodulecircle.repository.CircleCommentRepository;
import com.alikeyou.itmodulecircle.repository.CircleMemberRepository;
import com.alikeyou.itmodulecircle.repository.CircleRepository;
import com.alikeyou.itmodulecircle.support.CircleCommentVisibilitySupport;
import com.alikeyou.itmodulecircle.support.CircleLifecycleCompat;
import com.alikeyou.itmodulecircle.support.CircleMessageNormalizer;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CircleServiceImplTest {

    @Mock
    private CircleRepository circleRepository;

    @Mock
    private CircleMemberRepository circleMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CircleCommentRepository circleCommentRepository;

    @InjectMocks
    private CircleServiceImpl circleService;

    @Test
    void createCircleWithOperatorShouldApplyDefaultVisibilityAndMaxMembers() {
        CircleCreateRequest request = new CircleCreateRequest();
        request.setName("默认值圈子");
        request.setDescription("desc");
        request.setCreatorId(1L);

        UserInfo creator = new UserInfo();
        creator.setId(1L);
        creator.setUsername("creator");

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(circleRepository.save(any(Circle.class))).thenAnswer(invocation -> {
            Circle circle = invocation.getArgument(0);
            circle.setId(101L);
            return circle;
        });
        when(circleMemberRepository.save(any(CircleMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Circle saved = circleService.createCircleWithOperator(request);

        assertNotNull(saved);
        assertEquals("public", saved.getVisibility());
        assertEquals(500, saved.getMaxMembers());

        ArgumentCaptor<CircleMember> memberCaptor = ArgumentCaptor.forClass(CircleMember.class);
        verify(circleMemberRepository).save(memberCaptor.capture());
        assertEquals("owner", memberCaptor.getValue().getRole());
        assertEquals("active", memberCaptor.getValue().getStatus());
    }

    @Test
    void createCircleWithOperatorShouldRespectProvidedVisibilityAndMaxMembers() {
        CircleCreateRequest request = new CircleCreateRequest();
        request.setName("自定义圈子");
        request.setDescription("desc");
        request.setCreatorId(2L);
        request.setVisibility("private");
        request.setMaxMembers(128);

        UserInfo creator = new UserInfo();
        creator.setId(2L);
        creator.setUsername("creator2");

        when(userRepository.findById(2L)).thenReturn(Optional.of(creator));
        when(circleRepository.save(any(Circle.class))).thenAnswer(invocation -> {
            Circle circle = invocation.getArgument(0);
            circle.setId(102L);
            return circle;
        });
        when(circleMemberRepository.save(any(CircleMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Circle saved = circleService.createCircleWithOperator(request);

        assertNotNull(saved);
        assertEquals("private", saved.getVisibility());
        assertEquals(128, saved.getMaxMembers());
    }

    @Test
    void requireCircleManagePermissionShouldAllowCircleAdmin() {
        Circle circle = new Circle();
        circle.setId(11L);
        circle.setCreatorId(99L);

        UserInfo operator = new UserInfo();
        operator.setId(2L);
        operator.setRoleId(4);

        CircleMember adminMember = new CircleMember();
        adminMember.setCircle(circle);
        adminMember.setUser(operator);
        adminMember.setRole("admin");

        when(circleRepository.findById(11L)).thenReturn(Optional.of(circle));
        when(userRepository.findById(2L)).thenReturn(Optional.of(operator));
        when(circleMemberRepository.findByCircleAndUser(circle, operator)).thenReturn(Optional.of(adminMember));

        Circle result = circleService.requireCircleManagePermission(11L, 2L);

        assertSame(circle, result);
    }

    @Test
    void requireCircleManagePermissionShouldRejectOrdinaryMember() {
        Circle circle = new Circle();
        circle.setId(12L);
        circle.setCreatorId(99L);

        UserInfo operator = new UserInfo();
        operator.setId(3L);
        operator.setRoleId(4);

        CircleMember member = new CircleMember();
        member.setCircle(circle);
        member.setUser(operator);
        member.setRole("member");

        when(circleRepository.findById(12L)).thenReturn(Optional.of(circle));
        when(userRepository.findById(3L)).thenReturn(Optional.of(operator));
        when(circleMemberRepository.findByCircleAndUser(circle, operator)).thenReturn(Optional.of(member));

        CircleException exception = assertThrows(CircleException.class,
                () -> circleService.requireCircleManagePermission(12L, 3L));

        assertEquals(CircleMessageNormalizer.PERMISSION_DENIED, exception.getMessage());
    }

    @Test
    void requirePublicVisibleCircleShouldTreatLegacyPublicTypeAsApproved() {
        Circle circle = new Circle();
        circle.setId(13L);
        circle.setType("public");
        circle.setVisibility("public");
        circle.setDescription("legacy");

        when(circleRepository.findById(13L)).thenReturn(Optional.of(circle));

        Circle result = circleService.requirePublicVisibleCircle(13L);

        assertSame(circle, result);
        assertEquals(CircleLifecycleCompat.APPROVED, circleService.getLifecycleStatus(circle));
    }

    @Test
    void convertToResponseShouldUsePublicVisiblePostCount() {
        Circle circle = new Circle();
        circle.setId(20L);
        circle.setName("测试圈子");
        circle.setVisibility("public");
        circle.setType("approved");

        when(circleMemberRepository.countByCircle(circle)).thenReturn(5L);
        when(circleMemberRepository.countByCircleAndStatus(circle, "active")).thenReturn(4L);
        when(circleCommentRepository.findByCircleIdAndParentCommentIdIsNullOrderByCreatedAtDesc(20L)).thenReturn(List.of(
                buildRootPost(1L, "published"),
                buildRootPost(2L, "normal"),
                buildRootPost(3L, "pending"),
                buildRootPost(4L, "hidden"),
                buildRootPost(5L, "deleted")
        ));

        CircleResponse response = circleService.convertToResponse(circle);

        assertEquals(2L, response.getPostCount());
    }

    @Test
    void getCircleStatisticsByIdShouldSeparatePublicAndManagePostCalibers() {
        Circle circle = new Circle();
        circle.setId(21L);

        when(circleRepository.findById(21L)).thenReturn(Optional.of(circle));
        when(circleMemberRepository.countByCircle(circle)).thenReturn(5L);
        when(circleMemberRepository.countByCircleAndStatus(circle, "active")).thenReturn(4L);
        when(circleCommentRepository.findByCircleIdAndParentCommentIdIsNullOrderByCreatedAtDesc(21L)).thenReturn(List.of(
                buildRootPost(1L, "published"),
                buildRootPost(2L, "normal"),
                buildRootPost(3L, "hidden"),
                buildRootPost(4L, "deleted")
        ));

        Map<String, Long> stats = circleService.getCircleStatisticsById(21L);

        assertEquals(2L, stats.get("postCount"));
        assertEquals(4L, stats.get("managePostCount"));
        assertEquals(1L, stats.get("pendingPostCount"));
        assertEquals(2L, stats.get("publishedPostCount"));
        assertEquals(1L, stats.get("deletedPostCount"));
    }

    @Test
    void countPostsByCircleIdShouldOnlyCountPublicVisiblePosts() {
        when(circleCommentRepository.findByCircleIdAndParentCommentIdIsNullOrderByCreatedAtDesc(22L)).thenReturn(List.of(
                buildRootPost(1L, "published"),
                buildRootPost(2L, "hidden"),
                buildRootPost(3L, "normal")
        ));

        long postCount = circleService.countPostsByCircleId(22L);

        assertEquals(2L, postCount);
    }

    @Test
    void updateCircleWithOperatorShouldRejectOrdinaryMember() {
        Circle existing = buildCircle(30L, 99L, "approved");
        UserInfo memberUser = buildUser(7L, 4);
        CircleMember member = buildMembership(existing, memberUser, "member");
        CircleUpdateRequest request = new CircleUpdateRequest();
        request.setOperatorId(7L);
        request.setName("新名称");

        when(circleRepository.findById(30L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(7L)).thenReturn(Optional.of(memberUser));
        when(circleMemberRepository.findByCircleAndUser(existing, memberUser)).thenReturn(Optional.of(member));

        CircleException exception = assertThrows(CircleException.class,
                () -> circleService.updateCircleWithOperator(30L, request));

        assertEquals(CircleMessageNormalizer.PERMISSION_DENIED, exception.getMessage());
    }

    @Test
    void closeCircleWithDetailShouldRejectOrdinaryMember() {
        Circle existing = buildCircle(31L, 99L, "approved");
        UserInfo memberUser = buildUser(7L, 4);
        CircleMember member = buildMembership(existing, memberUser, "member");
        CircleCloseRequest request = new CircleCloseRequest();
        request.setOperatorId(7L);
        request.setReason("无权限关闭");

        when(circleRepository.findById(31L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(7L)).thenReturn(Optional.of(memberUser));
        when(circleMemberRepository.findByCircleAndUser(existing, memberUser)).thenReturn(Optional.of(member));

        CircleException exception = assertThrows(CircleException.class,
                () -> circleService.closeCircleWithDetail(31L, request));

        assertEquals(CircleMessageNormalizer.PERMISSION_DENIED, exception.getMessage());
    }

    @Test
    void deleteCircleShouldRejectOrdinaryMember() {
        Circle existing = buildCircle(32L, 99L, "approved");
        UserInfo memberUser = buildUser(7L, 4);
        CircleMember member = buildMembership(existing, memberUser, "member");

        when(circleRepository.findById(32L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(7L)).thenReturn(Optional.of(memberUser));
        when(circleMemberRepository.findByCircleAndUser(existing, memberUser)).thenReturn(Optional.of(member));

        CircleException exception = assertThrows(CircleException.class,
                () -> circleService.deleteCircle(32L, 7L));

        assertEquals(CircleMessageNormalizer.PERMISSION_DENIED, exception.getMessage());
    }

    @Test
    void updateCircleWithOperatorShouldAllowCircleAdmin() {
        Circle existing = buildCircle(33L, 99L, "approved");
        UserInfo adminUser = buildUser(8L, 4);
        CircleMember adminMember = buildMembership(existing, adminUser, "admin");
        CircleUpdateRequest request = new CircleUpdateRequest();
        request.setOperatorId(8L);
        request.setName("管理员更新后");
        request.setDescription("更新描述");

        when(circleRepository.findById(33L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(8L)).thenReturn(Optional.of(adminUser));
        when(circleMemberRepository.findByCircleAndUser(existing, adminUser)).thenReturn(Optional.of(adminMember));
        when(circleRepository.save(any(Circle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Circle saved = circleService.updateCircleWithOperator(33L, request);

        assertEquals("管理员更新后", saved.getName());
        assertEquals("更新描述", CircleLifecycleCompat.stripLifecycleMarker(saved.getDescription()));
    }

    @Test
    void deleteCircleShouldAllowOwnerCreator() {
        Circle existing = buildCircle(34L, 11L, "approved");
        when(circleRepository.findById(34L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(11L)).thenReturn(Optional.of(buildUser(11L, 4)));
        doNothing().when(circleRepository).delete(existing);

        circleService.deleteCircle(34L, 11L);

        verify(circleRepository).delete(existing);
    }

    private CircleComment buildRootPost(Long id, String status) {
        CircleComment post = new CircleComment();
        post.setId(id);
        post.setStatus(status);
        post.setParentCommentId(null);
        post.setPostId(id);
        return post;
    }

    private Circle buildCircle(Long id, Long creatorId, String lifecycle) {
        Circle circle = new Circle();
        circle.setId(id);
        circle.setName("圈子-" + id);
        circle.setCreatorId(creatorId);
        circle.setType(lifecycle);
        circle.setVisibility("public");
        circle.setDescription("圈子描述");
        return circle;
    }

    private UserInfo buildUser(Long id, Integer roleId) {
        UserInfo user = new UserInfo();
        user.setId(id);
        user.setRoleId(roleId);
        user.setUsername("user-" + id);
        return user;
    }

    private CircleMember buildMembership(Circle circle, UserInfo user, String role) {
        CircleMember member = new CircleMember();
        member.setCircle(circle);
        member.setUser(user);
        member.setRole(role);
        return member;
    }
}

