package io.github.zhangliangbo.savetime.inner;

import com.cdancy.jenkins.rest.JenkinsClient;
import com.cdancy.jenkins.rest.domain.common.Error;
import com.cdancy.jenkins.rest.domain.common.ErrorsHolder;
import com.cdancy.jenkins.rest.domain.common.IntegerResponse;
import com.cdancy.jenkins.rest.domain.common.RequestStatus;
import com.cdancy.jenkins.rest.domain.job.BuildInfo;
import com.cdancy.jenkins.rest.domain.job.Job;
import com.cdancy.jenkins.rest.domain.job.JobInfo;
import com.cdancy.jenkins.rest.domain.job.JobList;
import com.cdancy.jenkins.rest.domain.system.SystemInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author zhangliangbo
 * @since 2022/11/14
 */
public class JenkinsRest extends AbstractConfigurable<JenkinsClient> {

    private volatile boolean valid = true;

    @Override
    protected boolean isValid(JenkinsClient jenkinsClient) {
        return valid;
    }

    @Override
    protected JenkinsClient create(String key) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode treeNode = objectMapper.readTree(getConfig());
        JsonNode jenkins = treeNode.get(key);

        String host = jenkins.get("host").asText();
        int port = jenkins.get("port").asInt();
        String username = jenkins.get("username").asText();
        String password = jenkins.get("password").asText();

        this.valid = true;

        return JenkinsClient.builder()
                .endPoint(String.join(":", host, String.valueOf(port)))
                .credentials(String.join(":", username, password))
                .build();
    }

    public JsonNode systemInfo(String key) throws Exception {
        SystemInfo systemInfo = mayRetry(key, () -> getOrCreate(key).api().systemApi().systemInfo());
        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
        objectNode.put("hudsonVersion", systemInfo.hudsonVersion());
        objectNode.put("jenkinsSession", systemInfo.jenkinsSession());
        objectNode.put("jenkinsVersion", systemInfo.jenkinsVersion());
        objectNode.put("instanceIdentity", systemInfo.instanceIdentity());
        objectNode.put("server", systemInfo.server());
        return objectNode;
    }

    public JsonNode jobs(String key) throws Exception {
        JobList jobList = getOrCreate(key).api().jobsApi().jobList(StringUtils.EMPTY);
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
        if (CollectionUtils.isNotEmpty(jobList.jobs())) {
            for (Job job : jobList.jobs()) {
                ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
                objectNode.put("clazz", job.clazz());
                objectNode.put("name", job.name());
                objectNode.put("url", job.url());
                arrayNode.add(objectNode);
            }
        }
        return arrayNode;
    }

    public JsonNode jobsByPatterns(String key, List<String> patterns) throws Exception {
        JobList jobList = getOrCreate(key).api().jobsApi().jobList(StringUtils.EMPTY);
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
        if (CollectionUtils.isNotEmpty(jobList.jobs())) {
            List<Pattern> patternList = patterns.stream().map(Pattern::compile).collect(Collectors.toList());
            for (Job job : jobList.jobs()) {
                if (patternList.stream().anyMatch(p -> p.matcher(job.name()).matches())) {
                    ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
                    objectNode.put("clazz", job.clazz());
                    objectNode.put("name", job.name());
                    objectNode.put("url", job.url());
                    arrayNode.add(objectNode);
                }
            }
        }
        return arrayNode;
    }

    public JsonNode jobsByPattern(String key, String pattern) throws Exception {
        return jobsByPatterns(key, Lists.newArrayList(pattern));
    }

    public JsonNode jobInfo(String key, String job) throws Exception {
        JobInfo jobInfo = getOrCreate(key).api().jobsApi().jobInfo(StringUtils.EMPTY, job);
        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
        objectNode.put("name", jobInfo.name());
        objectNode.put("url", jobInfo.url());
        objectNode.put("description", jobInfo.description());
        objectNode.put("buildable", jobInfo.buildable());
        objectNode.put("displayName", jobInfo.displayName());
        objectNode.put("nextBuildNumber", jobInfo.nextBuildNumber());
        objectNode.set("lastBuild", buildInfoNode(jobInfo.lastBuild()));
        return objectNode;
    }

    public JsonNode buildInfo(String key, String job, int number) throws Exception {
        BuildInfo buildInfo = getOrCreate(key).api().jobsApi().buildInfo(StringUtils.EMPTY, job, number);
        return buildInfoNode(buildInfo);
    }

    private JsonNode buildInfoNode(BuildInfo buildInfo) {
        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
        objectNode.put("url", buildInfo.url());
        objectNode.put("number", buildInfo.number());
        objectNode.put("building", buildInfo.building());
        objectNode.put("url", buildInfo.url());
        objectNode.put("id", buildInfo.id());
        objectNode.put("queueId", buildInfo.queueId());
        objectNode.put("displayName", buildInfo.displayName());
        objectNode.put("fullDisplayName", buildInfo.fullDisplayName());
        objectNode.put("timestamp", LocalDateTime.ofInstant(Instant.ofEpochMilli(buildInfo.timestamp()), ZoneId.systemDefault()).toString());
        objectNode.put("duration", buildInfo.duration());
        objectNode.put("estimatedDuration", Duration.ofMillis(buildInfo.estimatedDuration()).toString());
        objectNode.put("result", Objects.equals(buildInfo.result(), "SUCCESS"));
        return objectNode;
    }

    public JsonNode buildJobWithParameters(String key, String job, Map<String, List<String>> map) throws Exception {
        IntegerResponse integerResponse = mayRetry(key, () -> getOrCreate(key).api().jobsApi().buildWithParameters(StringUtils.EMPTY, job, map));
        return toNode(integerResponse);
    }

    private JsonNode toNode(IntegerResponse integerResponse) {
        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
        objectNode.put("value", integerResponse.value());
        if (CollectionUtils.isNotEmpty(integerResponse.errors())) {
            ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
            for (Error error : integerResponse.errors()) {
                ObjectNode errorNode = new ObjectNode(JsonNodeFactory.instance);
                objectNode.put("context", error.context());
                objectNode.put("message", error.message());
                objectNode.put("exceptionName", error.exceptionName());
                arrayNode.add(errorNode);
            }
            objectNode.set("errors", arrayNode);
        }
        return objectNode;
    }

    public JsonNode stopJob(String key, String job, int number) throws Exception {
        RequestStatus requestStatus = mayRetry(key, () -> getOrCreate(key).api().jobsApi().stop(StringUtils.EMPTY, job, number));
        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
        objectNode.put("value", requestStatus.value());
        if (CollectionUtils.isNotEmpty(requestStatus.errors())) {
            ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
            for (Error error : requestStatus.errors()) {
                ObjectNode errorNode = new ObjectNode(JsonNodeFactory.instance);
                objectNode.put("context", error.context());
                objectNode.put("message", error.message());
                objectNode.put("exceptionName", error.exceptionName());
                arrayNode.add(errorNode);
            }
            objectNode.set("errors", arrayNode);
        }
        return objectNode;
    }

    public int lastBuildNumber(String key, String job) throws Exception {
        return Optional.ofNullable(getOrCreate(key).api().jobsApi().lastBuildNumber(StringUtils.EMPTY, job)).orElse(0);
    }

    public String lastBuildTimestamp(String key, String job) throws Exception {
        return getOrCreate(key).api().jobsApi().lastBuildTimestamp(StringUtils.EMPTY, job);
    }

    public JsonNode buildJobWithParametersSync(String key, String job, Map<String, List<String>> map, int checkInterval, boolean closeBefore, int times) throws Exception {
        if (closeBefore) {
            int lastBuildNumber = lastBuildNumber(key, job);
            while (true) {
                JsonNode jsonNode = buildInfo(key, job, lastBuildNumber);
                JsonNode buildingNode = jsonNode.get("building");
                if (Objects.nonNull(buildingNode) && buildingNode.asBoolean()) {
                    JsonNode stopJob = stopJob(key, job, lastBuildNumber);
                    System.out.printf("停止任务 %s %s %s %s%n", key, job, lastBuildNumber, stopJob);
                    lastBuildNumber--;
                } else {
                    break;
                }
            }
        }
        JsonNode buildJob = buildJobWithParameters(key, job, map);
        int queueId = buildJob.get("value").asInt();
        System.out.printf("%s %s%n", job, buildJob);
        //等待任务开始
        int lastBuildNumber;
        while (true) {
            lastBuildNumber = lastBuildNumber(key, job);
            JsonNode buildInfo = buildInfo(key, job, lastBuildNumber);
            JsonNode queueIdNode = buildInfo.get("queueId");
            System.out.printf("%s %s%n", job, queueIdNode);
            if (Objects.nonNull(queueIdNode) && queueIdNode.asInt() < queueId) {
                TimeUnit.SECONDS.sleep(checkInterval);
            } else {
                break;
            }
        }
        //等待任务完成
        while (true) {
            JsonNode buildInfo = buildInfo(key, job, lastBuildNumber);
            JsonNode buildingNode = buildInfo.get("building");
            if (Objects.nonNull(buildingNode) && buildingNode.asBoolean()) {
                TimeUnit.SECONDS.sleep(checkInterval);
            } else {
                break;
            }
        }

        JsonNode buildInfo = buildInfo(key, job, lastBuildNumber);
        JsonNode resultNode = buildInfo.get("result");
        boolean result = resultNode.asBoolean(true);
        if (result || times >= 3) {
            return buildJob;
        }
        ++times;
        System.out.printf("%s %s 报错开始重试 %s%n", job, buildJob, times);
        TimeUnit.SECONDS.sleep(checkInterval);
        return buildJobWithParametersSync(key, job, map, checkInterval, closeBefore, times);
    }

    public JsonNode buildJobWithParametersSync(String key, String job, Map<String, List<String>> map) throws Exception {
        return buildJobWithParametersSync(key, job, map, 10, true, 0);
    }

    private <T extends ErrorsHolder> T mayRetry(String key, Callable<T> callable) throws Exception {
        while (true) {
            T t = callable.call();
            if (CollectionUtils.isEmpty(t.errors())) {
                return t;
            } else {
                System.out.println(t.errors());
                //关闭现有客户端
                getOrCreate(key).close();
                //置为失效，这样后续才生成新的客户端
                this.valid = false;
            }
        }
    }

}
