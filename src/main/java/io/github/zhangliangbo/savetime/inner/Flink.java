package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * /jobs/overview
 * /jobs/e62cefa45693b8337b3b8504dde8a002
 *
 * @author zhangliangbo
 * @since 2023/2/11
 */
public class Flink extends Http {
    /**
     * 所有任务
     *
     * @param key 环境
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode jobs(String key) throws Exception {
        return get(key, "/jobs/overview");
    }

    /**
     * 根据名称搜索所有任务
     *
     * @param key 环境
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode jobsByName(String key, String name) throws Exception {
        JsonNode jsonNode = get(key, "/jobs/overview");

        ArrayNode result = new ArrayNode(JsonNodeFactory.instance);
        ArrayNode arrayNode = (ArrayNode) jsonNode.get("jobs");
        for (JsonNode node : arrayNode) {
            if (node.has("name") && Objects.equals(node.get("name").asText(), name)) {
                result.add(node);
            }
        }

        return result;
    }

    /**
     * 所有JAR
     *
     * @param key 环境
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode jars(String key) throws Exception {
        return get(key, "/jars");
    }

    /**
     * 上传JAR
     *
     * @param key 环境
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode jarUpload(String key, File file) throws Exception {
        Map<String, Object> parts = new LinkedHashMap<>();
        parts.put("jarfile", file);
        return postMultipart(key, "/jars/upload", null, parts);
    }

    /**
     * 运行JAR
     *
     * @param key 环境
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode jarRun(String key, String fileName, String entryClass, String programArgs) throws Exception {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("entry-class", entryClass);
        query.put("program-args", URLEncoder.encode(programArgs, "UTF-8"));
        Map<String, Object> body = new LinkedHashMap<>();
        query.put("entryClass", entryClass);
        query.put("programArgs", programArgs);
        String path = String.format("/jars/%s/run", FilenameUtils.getName(fileName));
        return post(key, path, query, body);
    }

    /**
     * 取消JOB
     *
     * @param key 环境
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode jobCancel(String key, String jobId) throws Exception {
        String path = String.format("/jobs/%s/yarn-cancel", jobId);
        return get(key, path);
    }

}
