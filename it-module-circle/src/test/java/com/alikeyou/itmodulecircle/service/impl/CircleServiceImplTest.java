package com.alikeyou.itmodulecircle.service.impl;

import com.alikeyou.itmodulecircle.dto.CircleCreateRequest;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.entity.CircleMember;
import com.alikeyou.itmodulecircle.repository.CircleCommentRepository;
import com.alikeyou.itmodulecircle.repository.CircleMemberRepository;
import com.alikeyou.itmodulecircle.repository.CircleRepository;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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
}

