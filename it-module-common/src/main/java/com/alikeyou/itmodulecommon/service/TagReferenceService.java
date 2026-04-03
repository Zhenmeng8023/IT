package com.alikeyou.itmodulecommon.service;

import java.util.Map;

public interface TagReferenceService {

    Map<Long, Long> countPublishedTagUsage();

    void renameTagReferences(Long tagId, String newTagName);

    void removeTagReferences(Long tagId);
}
