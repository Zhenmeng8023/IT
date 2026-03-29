package com.alikeyou.itmodulecircle.controller;

import com.alikeyou.itmodulecircle.dto.CircleRequest;
import com.alikeyou.itmodulecircle.dto.CircleResponse;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.exception.CircleException;
import com.alikeyou.itmodulecircle.service.CircleCommentService;
import com.alikeyou.itmodulecircle.service.CircleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/circle")
@Tag(name = "圈子管理", description = "圈子的创建、查询、更新、删除等相关接口")
public class CircleController {

    @Autowired
    private CircleService circleService;

    @Autowired
    private CircleCommentService circleCommentService;
    @PostMapping
    @Operation(summary = "创建新圈子")
    public ResponseEntity<CircleResponse> createCircle(@RequestBody CircleRequest request) {
        if (request.getCreatorId() == null) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "创建者 ID 不能为空");
            return ResponseEntity.badRequest().body(null);
        }

        try {
            Circle circle = new Circle();
            circle.setName(request.getName());
            circle.setDescription(request.getDescription());
            circle.setType(request.getType());
            circle.setVisibility(request.getVisibility());
            circle.setMaxMembers(request.getMaxMembers());
            circle.setCreatorId(request.getCreatorId());

            Circle result = circleService.createCircle(circle);
            CircleResponse response = circleService.convertToResponse(result);
            return ResponseEntity.ok(response);
        } catch (CircleException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "服务器内部错误");
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新圈子信息")
    public ResponseEntity<CircleResponse> updateCircle(@PathVariable Long id,
                                                       @RequestBody CircleRequest request) {
        try {
            Circle updatedCircle = new Circle();
            updatedCircle.setName(request.getName());
            updatedCircle.setDescription(request.getDescription());
            updatedCircle.setType(request.getType());
            updatedCircle.setVisibility(request.getVisibility());
            updatedCircle.setMaxMembers(request.getMaxMembers());

            Circle result = circleService.updateCircle(id, updatedCircle);
            CircleResponse response = circleService.convertToResponse(result);
            return ResponseEntity.ok(response);
        } catch (CircleException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "服务器内部错误");
            return ResponseEntity.internalServerError().body(null);
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
            List<Circle> circles = circleService.getAllCircles();
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
            List<Circle> circles = circleService.getPublicCircles();
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
}
