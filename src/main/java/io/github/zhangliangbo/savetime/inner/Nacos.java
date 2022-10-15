package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zhangliangbo
 * @since 2022/10/13
 */
public class Nacos extends Http {

    public JsonNode namespaces(String key) throws Exception {
        JsonNode jsonNode = get(key, "/nacos/v1/console/namespaces");
        if (jsonNode.has("data")) {
            return jsonNode.get("data");
        }
        return null;
    }

    public JsonNode state(String key) throws Exception {
        return get(key, "/nacos/v1/console/server/state");
    }

    public JsonNode configs(String key, String namespace, String dataId, String group, int page, int size) throws Exception {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("tenant", namespace);
        query.put("dataId", Optional.ofNullable(dataId).orElse(StringUtils.EMPTY));
        query.put("group", Optional.ofNullable(group).orElse(StringUtils.EMPTY));
        query.put("pageNo", page);
        query.put("pageSize", size);
        query.put("search", Objects.nonNull(dataId) && dataId.contains("*") ? "blur" : "accurate");
        return get(key, "/nacos/v1/cs/configs", query);
    }

    public JsonNode config(String key, String namespace, String dataId, String group) throws Exception {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("tenant", namespace);
        query.put("namespaceId", namespace);
        query.put("dataId", Optional.ofNullable(dataId).orElse(StringUtils.EMPTY));
        query.put("group", Optional.ofNullable(group).orElse(StringUtils.EMPTY));
        query.put("show", "all");
        return get(key, "/nacos/v1/cs/configs", query);
    }

    public JsonNode config(String key, String namespace, String dataId) throws Exception {
        return config(key, namespace, dataId, "DEFAULT_GROUP");
    }

    public JsonNode updateConfig(String key, String namespace, String dataId, String group, String content) throws Exception {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("username", getOrCreate(key).getLeft().get("username").asText());
        query.put("tenant", namespace);
        query.put("dataId", Optional.ofNullable(dataId).orElse(StringUtils.EMPTY));
        query.put("group", Optional.ofNullable(group).orElse(StringUtils.EMPTY));
        query.put("content", content);
        return post(key, "/nacos/v1/cs/configs", query);
    }

    public JsonNode updateConfig(String key, String namespace, String dataId, String content) throws Exception {
        return updateConfig(key, namespace, dataId, "DEFAULT_GROUP", content);
    }

    public JsonNode updateConfig(String key, String namespace, File file) throws Exception {
        String dataId = FilenameUtils.getName(file.getAbsolutePath());
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        return updateConfig(key, namespace, dataId, content);
    }

}
