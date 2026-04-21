package com.alikeyou.itmoduleblog.entity.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Converter
public class BlogTagsJsonConverter implements AttributeConverter<Map<String, String>, String> {

    private static final Logger log = LoggerFactory.getLogger(BlogTagsJsonConverter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<LinkedHashMap<String, String>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (Exception ex) {
            throw new IllegalArgumentException("博客标签序列化失败", ex);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(dbData);
            if (root == null || root.isNull()) {
                return new LinkedHashMap<>();
            }
            if (root.isObject()) {
                return OBJECT_MAPPER.convertValue(root, MAP_TYPE);
            }
            if (root.isArray()) {
                LinkedHashMap<String, String> mapped = new LinkedHashMap<>();
                Iterator<JsonNode> iterator = root.elements();
                while (iterator.hasNext()) {
                    JsonNode node = iterator.next();
                    if (node == null || node.isNull()) {
                        continue;
                    }
                    String tagId = node.asText(null);
                    if (tagId == null || tagId.trim().isEmpty()) {
                        continue;
                    }
                    mapped.put(tagId.trim(), tagId.trim());
                }
                return mapped;
            }
            return new LinkedHashMap<>();
        } catch (Exception ex) {
            log.warn("解析博客 tags 失败，已降级为空标签。原始数据: {}", dbData, ex);
            return new LinkedHashMap<>();
        }
    }
}
