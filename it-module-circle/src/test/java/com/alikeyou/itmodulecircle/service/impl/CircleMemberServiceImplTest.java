package com.alikeyou.itmodulecircle.service.impl;

import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.entity.CircleMember;
import com.alikeyou.itmodulecircle.exception.CircleException;
import com.alikeyou.itmodulecircle.repository.CircleMemberRepository;
import com.alikeyou.itmodulecircle.repository.CircleRepository;
import com.alikeyou.itmodulecircle.service.CircleService;
import com.alikeyou.itmodulecircle.support.CircleMessageNormalizer;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CircleMemberServiceImplTest {

    @Mock
    private CircleMemberRepository circleMemberRepository;

    @Mock
    private CircleRepository circleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CircleService circleService;

    @InjectMocks
    private CircleMemberServiceImpl circleMemberService;

    @Test
    void setAdminRoleShouldRequireManagePermission() {
        Circle circle = buildCircle(21L);

        when(circleService.requireCircleManagePermission(21L, 7L))
                .thenThrow(new CircleException(CircleMessageNormalizer.PERMISSION_DENIED));

        CircleException exception = assertThrows(CircleException.class,
                () -> circleMemberService.setAdminRole(21L, 8L, "admin", 7L));

        assertEquals(CircleMessageNormalizer.PERMISSION_DENIED, exception.getMessage());
    }

    @Test
    void removeMemberShouldDeleteAfterPermissionCheck() {
        Circle circle = buildCircle(22L);
        UserInfo targetUser = buildUser(9L);

        CircleMember member = new CircleMember();
        member.setCircle(circle);
        member.setUser(targetUser);
        member.setRole("member");

        when(circleService.requireCircleManagePermission(22L, 1L)).thenReturn(circle);
        when(userRepository.findById(9L)).thenReturn(Optional.of(targetUser));
        when(circleMemberRepository.findByCircleAndUser(circle, targetUser)).thenReturn(Optional.of(member));
        doNothing().when(circleMemberRepository).delete(member);

        circleMemberService.removeMember(22L, 9L, 1L);

        verify(circleMemberRepository).delete(member);
    }

    @Test
    void setMemberRoleByMemberIdShouldPreventOwnerDemotion() {
        Circle circle = buildCircle(23L);
        UserInfo user = buildUser(10L);

        CircleMember member = new CircleMember();
        member.setId(31L);
        member.setCircle(circle);
        member.setUser(user);
        member.setRole("owner");

        when(circleMemberRepository.findById(31L)).thenReturn(Optional.of(member));
        when(circleService.requireCircleManagePermission(23L, 1L)).thenReturn(circle);

        CircleException exception = assertThrows(CircleException.class,
                () -> circleMemberService.setMemberRoleByMemberId(31L, "admin", 1L));

        assertEquals("圈主角色不能直接变更，请先完成圈主转让", exception.getMessage());
    }

    private Circle buildCircle(Long id) {
        Circle circle = new Circle();
        circle.setId(id);
        circle.setCreatorId(99L);
        return circle;
    }

    private UserInfo buildUser(Long id) {
        UserInfo user = new UserInfo();
        user.setId(id);
        user.setUsername("user-" + id);
        return user;
    }
}
