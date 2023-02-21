package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.base.Stopwatch;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
    public ArrayNode jobsByName(String key, String name) throws Exception {
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
        body.put("entryClass", entryClass);
        body.put("programArgs", programArgs);
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

    public Pair<String, Duration> uploadRunCancel(String key, File file, String name, String entryClass, String programArgs) throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();

        System.out.println("开始上传jar");
        JsonNode jsonNode = jarUpload(key, file);
        String status = jsonNode.get("status").asText();
        if (!Objects.equals(status, "success")) {
            throw new Exception("上传Jar报错");
        }
        String fileName = jsonNode.get("filename").asText();
        System.out.printf("jar上传完毕 %s\n", fileName);
        System.out.printf("运行程序开始 %s %s\n", entryClass, programArgs);
        jsonNode = jarRun(key, fileName, entryClass, programArgs);
        String jobId = jsonNode.get("jobid").asText();
        System.out.printf("运行程序结果 %s\n", jobId);

        System.out.printf("等待程序启动 %s\n", jobId);
        while (true) {
            jsonNode = jobInfo(key, jobId);
            if (Objects.isNull(jsonNode)) {
                break;
            }
            String state = jsonNode.get("state").asText();
            int total = jsonNode.path("tasks").path("total").asInt(0);
            int running = jsonNode.path("tasks").path("running").asInt(0);
            if (Objects.equals(state, "RUNNING") && total == running) {
                System.out.printf("等待程序启动 结束 %s\n", jobId);
                break;
            }
            System.out.printf("等待程序启动 未完成 开始睡眠 %s\n", jobId);
            TimeUnit.SECONDS.sleep(5);
        }

        System.out.println("取消旧程序开始");
        ArrayNode oldJobs = jobsByName(key, name);
        for (JsonNode oldJob : oldJobs) {
            String jid = oldJob.get("jid").asText();
            String state = oldJob.get("state").asText();
            if (Objects.equals(jid, jobId) || !Objects.equals(state, "RUNNING")) {
                continue;
            }
            jsonNode = jobCancel(key, jid);
            System.out.printf("取消旧程序结果 %s %s\n", jid, jsonNode);
        }

        return Pair.of(jobId, stopwatch.elapsed());
    }

    /**
     * 任务详情
     *
     * @param key 环境
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode jobInfo(String key, String jobId) throws Exception {
        JsonNode jsonNode = get(key, "/jobs/overview");

        ArrayNode arrayNode = (ArrayNode) jsonNode.get("jobs");
        for (JsonNode node : arrayNode) {
            if (node.has("jid") && Objects.equals(node.get("jid").asText(), jobId)) {
                return node;
            }
        }

        return null;

    }

}
