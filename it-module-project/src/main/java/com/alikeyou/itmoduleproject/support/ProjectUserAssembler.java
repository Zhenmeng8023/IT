package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.UserInfoLiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProjectUserAssembler {

    private final UserInfoLiteRepository userInfoLiteRepository;

    public Map<Long, UserInfoLite> mapByIds(Collection<Long> ids) {
        if (ids == null) {
            return Collections.emptyMap();
        }
        List<Long> userIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userInfoLiteRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(UserInfoLite::getId, Function.identity(), (a, b) -> a));
    }

    public String resolveDisplayName(UserInfoLite user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return null;
    }
}
