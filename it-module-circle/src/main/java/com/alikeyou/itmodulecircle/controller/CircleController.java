package com.alikeyou.itmodulecircle.controller;

import com.alikeyou.itmodulecircle.dto.CircleCloseRequest;
import com.alikeyou.itmodulecircle.dto.CircleCreateRequest;
import com.alikeyou.itmodulecircle.dto.CircleUpdateRequest;
import com.alikeyou.itmodulecircle.dto.CircleResponse;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.exception.CircleException;
import com.alikeyou.itmodulecircle.service.CircleService;
import com.alikeyou.itmodulecircle.support.CircleMessageNormalizer;
import com.alikeyou.itmodulecommon.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/circle")
@Tag(name = "圈子管理", description = "圈子的创建、查询、更新、删除等相关接口")
public class CircleController {

    @Autowired
    private CircleService circleService;

    @PostMapping
    @Operation(summary = "创建新圈子", description = "创建一个新的交流圈子，默认状态为待审核")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功", content = @Content(schema = @Schema(implementation = CircleResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误，如名称重复、用户不存在等"),
            @ApiResponse(responseCode = "401", description = "未登录或登录信息失效"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> createCircle(@Valid @RequestBody CircleCreateRequest request) {
        try {
            // 自动获取当前登录用户 ID 作为创建者
            Long currentUserId = requireCurrentUserId();
            request.setCreatorId(currentUserId);

            Circle result = circleService.createCircleWithOperator(request);
            CircleResponse response = circleService.convertToResponse(result);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return buildError(resolveHttpStatus(e), e.getReason());
        } catch (CircleException e) {
            return buildError(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新圈子信息", description = "更新指定圈子的基本信息，仅圈主或管理员可操作")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功", content = @Content(schema = @Schema(implementation = CircleResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误，如名称重复等"),
            @ApiResponse(responseCode = "401", description = "未登录或登录信息失效"),
            @ApiResponse(responseCode = "404", description = "圈子不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> updateCircle(@PathVariable Long id,
                                          @Valid @RequestBody CircleUpdateRequest request) {
        try {
            // 自动获取当前登录用户 ID 作为操作人
            Long currentUserId = requireCurrentUserId();
            request.setOperatorId(currentUserId);

            Circle result = circleService.updateCircleWithOperator(id, request);
            CircleResponse response = circleService.convertToResponse(result);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return buildError(resolveHttpStatus(e), e.getReason());
        } catch (CircleException e) {
            return buildError(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
        }
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "关闭圈子", description = "将已审核通过的圈子状态设置为关闭，需要操作人权限")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "关闭成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误或圈子状态不符合要求"),
            @ApiResponse(responseCode = "404", description = "圈子不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<Map<String, String>> closeCircle(@PathVariable Long id,
                                                           @Valid @RequestBody CircleCloseRequest request) {
        try {
            // 自动获取当前登录用户 ID 作为操作人
            Long currentUserId = UserUtil.getCurrentUserId();
            request.setOperatorId(currentUserId);

            circleService.closeCircleWithDetail(id, request);

            Map<String, String> result = new HashMap<>();
            result.put("message", CircleMessageNormalizer.OPERATION_SUCCESS);
            return ResponseEntity.ok(result);
        } catch (CircleException e) {
            return buildError(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildError(HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除圈子")
    public ResponseEntity<Void> deleteCircle(@PathVariable Long id) {
        try {
            circleService.deleteCircle(id);
            return ResponseEntity.noContent().build();
        } catch (CircleException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取单个圈子详情")
    public ResponseEntity<CircleResponse> getCircleById(@PathVariable Long id) {
        try {
            return circleService.getCircleById(id)
                    .map(circle -> ResponseEntity.ok(circleService.convertToResponse(circle)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("")
    @Operation(summary = "获取所有圈子列表")
    public ResponseEntity<List<CircleResponse>> getAllCircles() {
        try {
            List<Circle> circles = circleService.getApprovedPublicCircles();
            List<CircleResponse> responses = circleService.convertToResponseList(circles);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/creator/{creatorId}")
    @Operation(summary = "根据创建者 ID 获取圈子列表")
    public ResponseEntity<List<CircleResponse>> getCirclesByCreatorId(@PathVariable Long creatorId) {
        try {
            List<Circle> circles = circleService.getCirclesByCreatorId(creatorId);
            List<CircleResponse> responses = circleService.convertToResponseList(circles);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/public")
    @Operation(summary = "获取公开圈子列表")
    public ResponseEntity<List<CircleResponse>> getPublicCircles() {
        try {
            List<Circle> circles = circleService.getApprovedPublicCircles();
            List<CircleResponse> responses = circleService.convertToResponseList(circles);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "根据类型获取圈子列表")
    public ResponseEntity<List<CircleResponse>> getCirclesByType(@PathVariable String type) {
        try {
            List<Circle> circles = circleService.getCirclesByType(type);
            List<CircleResponse> responses = circleService.convertToResponseList(circles);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待审核圈子列表", description = "返回所有状态为 pending 的待审核圈子")
    public ResponseEntity<List<CircleResponse>> getPendingCircles() {
        try {
            List<Circle> circles = circleService.getPendingCircles();
            List<CircleResponse> responses = circleService.convertToResponseList(circles);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/approved-public")
    @Operation(summary = "获取公开且已审核通过的圈子列表", description = "返回 type 为 public 且 visibility 为 public 的已审核圈子")
    public ResponseEntity<List<CircleResponse>> getApprovedPublicCircles() {
        try {
            List<Circle> circles = circleService.getApprovedPublicCircles();
            List<CircleResponse> responses = circleService.convertToResponseList(circles);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "根据名称获取圈子")
    public ResponseEntity<CircleResponse> getCircleByName(@PathVariable String name) {
        try {
            return circleService.getCircleByName(name)
                    .map(circle -> ResponseEntity.ok(circleService.convertToResponse(circle)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }


    @GetMapping("/exists/{name}")
    @Operation(summary = "检查圈子名称是否存在")
    public ResponseEntity<?> existsCircleByName(@PathVariable String name) {
        try {
            boolean exists = circleService.existsCircleByName(name);
            Map<String, Boolean> result = new HashMap<>();
            result.put("exists", exists);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private Long requireCurrentUserId() {
        try {
            Long currentUserId = UserUtil.getCurrentUserId();
            if (currentUserId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CircleMessageNormalizer.LOGIN_REQUIRED);
            }
            return currentUserId;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CircleMessageNormalizer.LOGIN_REQUIRED);
        }
    }

    private ResponseEntity<Map<String, String>> buildError(HttpStatus status, String message) {
        HttpStatus normalizedStatus = CircleMessageNormalizer.resolveStatus(status, message);
        Map<String, String> body = new HashMap<>();
        body.put("message", CircleMessageNormalizer.normalize(normalizedStatus, message));
        return ResponseEntity.status(normalizedStatus).body(body);
    }

    private HttpStatus resolveHttpStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        return status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    }
}
