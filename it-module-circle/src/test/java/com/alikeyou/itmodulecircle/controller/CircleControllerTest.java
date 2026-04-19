package com.alikeyou.itmodulecircle.controller;

import com.alikeyou.itmodulecircle.dto.CircleCreateRequest;
import com.alikeyou.itmodulecircle.dto.CircleResponse;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.exception.CircleException;
import com.alikeyou.itmodulecircle.service.CircleService;
import com.alikeyou.itmodulecommon.constant.LoginConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CircleControllerTest {

    @Mock
    private CircleService circleService;

    @InjectMocks
    private CircleController circleController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(circleController).build();
        LoginConstant.clearUserInfo();
    }

    @AfterEach
    void tearDown() {
        LoginConstant.clearUserInfo();
    }

    @Test
    void createCircleShouldReturnUnauthorizedWhenUserNotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/circle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "测试圈子",
                                  "description": "desc"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录后再操作"));

        verifyNoInteractions(circleService);
    }

    @Test
    void createCircleShouldInjectCurrentUserIdIntoRequest() throws Exception {
        LoginConstant.setUserId(88L);
        LoginConstant.setRoleId(3);
        LoginConstant.setUsername("tester");

        Circle savedCircle = new Circle();
        savedCircle.setId(12L);
        savedCircle.setName("测试圈子");

        CircleResponse response = new CircleResponse();
        response.setId(12L);
        response.setName("测试圈子");

        when(circleService.createCircleWithOperator(any(CircleCreateRequest.class))).thenReturn(savedCircle);
        when(circleService.convertToResponse(savedCircle)).thenReturn(response);

        mockMvc.perform(post("/api/circle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "测试圈子",
                                  "description": "desc",
                                  "visibility": "public"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.name").value("测试圈子"));

        ArgumentCaptor<CircleCreateRequest> captor = ArgumentCaptor.forClass(CircleCreateRequest.class);
        verify(circleService).createCircleWithOperator(captor.capture());
        assertEquals(88L, captor.getValue().getCreatorId());
    }

    @Test
    void createCircleShouldNormalizeNotFoundMessage() throws Exception {
        LoginConstant.setUserId(88L);
        LoginConstant.setRoleId(3);
        doThrow(new CircleException("圈子不存在，ID: 99"))
                .when(circleService)
                .createCircleWithOperator(any(CircleCreateRequest.class));

        mockMvc.perform(post("/api/circle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "测试圈子",
                                  "description": "desc"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("资源不存在或不可访问"));
    }
}

