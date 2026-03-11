package com.alikeyou.itmodulecommon.service;

import com.alikeyou.itmodulecommon.entity.Region;
import java.util.List;
import java.util.Optional;

public interface RegionService {
    List<Region> getAllRegions();
    Optional<Region> getRegionById(Long id);
}