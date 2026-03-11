package com.alikeyou.itmodulecommon.controller;

import com.alikeyou.itmodulecommon.dto.RegionDTO;
import com.alikeyou.itmodulecommon.entity.Region;
import com.alikeyou.itmodulecommon.entity.Tag;
import com.alikeyou.itmodulecommon.service.RegionService;
import com.alikeyou.itmodulecommon.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/common")
public class CommonController {

    @Autowired
    private RegionService regionService;

    @Autowired
    private TagService tagService;

    // 获取所有地区
    @GetMapping("/regions")
    public ResponseEntity<List<RegionDTO>> getAllRegions() {
        List<Region> regions = regionService.getAllRegions();
        List<RegionDTO> regionDTOs = regions.stream()
                .map(region -> new RegionDTO(
                        region.getId(),
                        region.getName(),
                        region.getLevel(),
                        region.getCode(),
                        region.getParent() != null ? region.getParent().getId() : null
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(regionDTOs);
    }

    // 根据ID获取地区
    @GetMapping("/regions/{id}")
    public ResponseEntity<RegionDTO> getRegionById(@PathVariable Long id) {
        Optional<Region> region = regionService.getRegionById(id);
        return region.map(r -> {
            RegionDTO dto = new RegionDTO(
                    r.getId(),
                    r.getName(),
                    r.getLevel(),
                    r.getCode(),
                    r.getParent() != null ? r.getParent().getId() : null
            );
            return ResponseEntity.ok(dto);
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 获取所有标签
    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> getAllTags() {
        List<Tag> tags = tagService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    // 根据ID获取标签
    @GetMapping("/tags/{id}")
    public ResponseEntity<Tag> getTagById(@PathVariable Long id) {
        Optional<Tag> tag = tagService.getTagById(id);
        return tag.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}