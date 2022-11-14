package io.github.zhangliangbo.savetime.inner;

import com.cdancy.jenkins.rest.JenkinsClient;
import com.cdancy.jenkins.rest.domain.common.IntegerResponse;
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
import org.apache.commons.dbutils.QueryRunner;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhangliangbo
 * @since 2022/11/14
 */
public class JenkinsRest extends AbstractConfigurable<JenkinsClient> {

    @Override
    protected boolean isValid(JenkinsClient jenkinsClient) {
        return true;
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

        return JenkinsClient.builder()
                .endPoint(String.join(":", host, String.valueOf(port)))
                .credentials(String.join(":", username, password))
                .build();
    }

    public JsonNode systemInfo(String key) throws Exception {
        SystemInfo systemInfo = getOrCreate(key).api().systemApi().systemInfo();
        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
        objectNode.put("hudsonVersion", systemInfo.hudsonVersion());
        objectNode.put("jenkinsSession", systemInfo.jenkinsSession());
        objectNode.put("jenkinsVersion", systemInfo.jenkinsVersion());
        objectNode.put("instanceIdentity", systemInfo.instanceIdentity());
        objectNode.put("server", systemInfo.server());
        return objectNode;
    }

    public JsonNode jobs(String key) throws Exception {
        JobList jobList = getOrCreate(key).api().jobsApi().jobList(null);
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

    public JsonNode jobInfo(String key, String job) throws Exception {
        JobInfo jobList = getOrCreate(key).api().jobsApi().jobInfo(null, job);
        System.out.println(jobList);
        return null;
    }

    public JsonNode buildJob(String key, String job) throws Exception {
        Map<String, List<String>> map = new LinkedHashMap<>();
        IntegerResponse jobList = getOrCreate(key).api().jobsApi().buildWithParameters(null, job, map);
        System.out.println(jobList);
        return null;
    }

}
