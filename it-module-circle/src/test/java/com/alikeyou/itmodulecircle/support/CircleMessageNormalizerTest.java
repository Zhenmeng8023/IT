package com.alikeyou.itmodulecircle.support;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CircleMessageNormalizerTest {

    @Test
    void shouldMapUnauthorizedMessage() {
        String message = CircleMessageNormalizer.normalize(HttpStatus.UNAUTHORIZED, "用户未登录");
        assertEquals("请先登录后再操作", message);
    }

    @Test
    void shouldMapBadRequestNotFoundMessageToNotFoundStatus() {
        HttpStatus status = CircleMessageNormalizer.resolveStatus(HttpStatus.BAD_REQUEST, "成员关系不存在，ID: 12");
        String message = CircleMessageNormalizer.normalize(status, "成员关系不存在，ID: 12");

        assertEquals(HttpStatus.NOT_FOUND, status);
        assertEquals("资源不存在或不可访问", message);
    }

    @Test
    void shouldNormalizeInvalidParamMessage() {
        String message = CircleMessageNormalizer.normalize(HttpStatus.BAD_REQUEST, "圈子 ID 不能为空");
        assertEquals("请求参数不合法", message);
    }
}
