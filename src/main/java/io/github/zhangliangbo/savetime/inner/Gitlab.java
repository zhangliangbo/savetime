package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.vavr.collection.LinkedHashMap;

import java.net.URLEncoder;
import java.util.Objects;

/**
 * @author zhangliangbo
 * @since 2022-11-19
 */
public class Gitlab extends Http {
    /**
     * 版本
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode version(String key) throws Exception {
        return get(key, "/version");
    }

    interface PageProcessor {
        JsonNode apply(int page) throws Exception;
    }

    /**
     * 组
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode groups(String key, String search) throws Exception {
        return extractPart(page -> get(key, "/groups", LinkedHashMap.<String, Object>of("search", search, "page", page, "per_page", 10, "simple", true).filterValues(Objects::nonNull).toJavaMap()));
    }

    public JsonNode extractPart(PageProcessor processor) throws Exception {
        int page = 1;

        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);

        while (true) {
            JsonNode jsonNode = processor.apply(page);
            if (!jsonNode.isArray() || jsonNode.isEmpty()) {
                break;
            }
            ArrayNode an = (ArrayNode) jsonNode;
            for (JsonNode node : an) {
                arrayNode.add(node);
            }
            ++page;
        }

        return arrayNode;
    }

    /**
     * 根据名称获取组id
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public String groupIdByName(String key, String name) throws Exception {
        JsonNode jsonNode = groups(key, name);
        return extractId(jsonNode, name);
    }

    private String extractId(JsonNode jsonNode, String name) {
        if (!jsonNode.isArray() || jsonNode.isEmpty()) {
            return null;
        }
        ArrayNode arrayNode = (ArrayNode) jsonNode;
        for (JsonNode node : arrayNode) {
            JsonNode nameNode = node.get("name");
            if (Objects.isNull(nameNode) || !Objects.equals(name, nameNode.asText())) {
                continue;
            }
            return node.get("id").asText();
        }
        return null;
    }

    /**
     * 根据组id获取组详情
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode groupDetail(String key, String groupName) throws Exception {
        String id = groupIdByName(key, groupName);
        return get(key, String.format("/groups/%s", id));
    }

    /**
     * 项目
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode projects(String key, String search) throws Exception {
        return extractPart(page -> get(key, "/projects", io.vavr.collection.LinkedHashMap.<String, Object>of("search", search, "page", page, "per_page", 10, "simple", true).filterValues(Objects::nonNull).toJavaMap()));
    }

    /**
     * 项目
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode projectsByGroup(String key, String groupName, String search) throws Exception {
        String groupId = groupIdByName(key, groupName);
        return extractPart(page -> get(key, String.format("/groups/%s/projects", groupId), io.vavr.collection.LinkedHashMap.<String, Object>of("search", search, "page", page, "per_page", 10, "simple", true).filterValues(Objects::nonNull).toJavaMap()));
    }

    /**
     * 根据项目名称获取id
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public String projectIdByName(String key, String groupName, String projectName) throws Exception {
        JsonNode jsonNode = projectsByGroup(key, groupName, projectName);
        return extractId(jsonNode, projectName);
    }

    /**
     * 根据组id获取组详情
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode projectDetail(String key, String groupName, String projectName) throws Exception {
        String id = projectIdByName(key, groupName, projectName);
        return get(key, String.format("/projects/%s", id));
    }


    /**
     * 分支列表
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode branches(String key, String groupName, String projectName) throws Exception {
        String projectId = projectIdByName(key, groupName, projectName);
        return extractPart(page -> get(key, String.format("/projects/%s/repository/branches", projectId), io.vavr.collection.LinkedHashMap.<String, Object>of("page", page, "per_page", 10).filterValues(Objects::nonNull).toJavaMap()));
    }

    /**
     * 分支详情
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode branchDetail(String key, String groupName, String projectName, String branch) throws Exception {
        String projectId = projectIdByName(key, groupName, projectName);
        return get(key, String.format("/projects/%s/repository/branches/%s", projectId, branch));
    }


    /**
     * 目录树
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode repositoryTree(String key, String groupName, String projectName, String branch, String path) throws Exception {
        String projectId = projectIdByName(key, groupName, projectName);
        return repositoryTreeByProjectId(key, projectId, branch, path);
    }

    /**
     * 目录树
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode repositoryTreeByProjectId(String key, String projectId, String branch, String path) throws Exception {
        return extractPart(page -> get(key, String.format("/projects/%s/repository/tree", projectId), io.vavr.collection.LinkedHashMap.<String, Object>of("ref", branch, "path", path, "page", page, "per_page", 10).filterValues(Objects::nonNull).toJavaMap()));
    }

    /**
     * blob信息
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode repositoryBlob(String key, String groupName, String projectName, String branch, String path, String name) throws Exception {
        String projectId = projectIdByName(key, groupName, projectName);
        if (Objects.isNull(projectId)) {
            return null;
        }
        String sha = repositoryBlobShaById(key, projectId, branch, path, name);
        if (Objects.isNull(sha)) {
            return null;
        }
        return get(key, String.format("/projects/%s/repository/blobs/%s", projectId, sha));
    }

    /**
     * blob内容
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public String repositoryBlobRaw(String key, String groupName, String projectName, String branch, String path, String name) throws Exception {
        String projectId = projectIdByName(key, groupName, projectName);
        if (Objects.isNull(projectId)) {
            return null;
        }
        String sha = repositoryBlobShaById(key, projectId, branch, path, name);
        if (Objects.isNull(sha)) {
            return null;
        }
        JsonNode jsonNode = get(key, String.format("/projects/%s/repository/blobs/%s/raw", projectId, sha));
        if (Objects.isNull(jsonNode)) {
            return null;
        }
        return jsonNode.get("response").asText();
    }

    /**
     * blob sha值
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    private String repositoryBlobShaById(String key, String projectId, String branch, String path, String name) throws Exception {
        JsonNode jsonNode = repositoryTreeByProjectId(key, projectId, branch, path);
        return extractId(jsonNode, name);
    }

    /**
     * blob信息
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public JsonNode repositoryFile(String key, String groupName, String projectName, String branch, String filePath) throws Exception {
        String projectId = projectIdByName(key, groupName, projectName);
        return get(key, String.format("/projects/%s/repository/files/%s", projectId, URLEncoder.encode(filePath, "UTF-8")), io.vavr.collection.LinkedHashMap.<String, Object>of("ref", branch).filterValues(Objects::nonNull).toJavaMap());
    }

    /**
     * blob内容
     *
     * @param key 环境
     * @return 数据
     * @throws Exception 异常
     */
    public String repositoryFileRaw(String key, String groupName, String projectName, String branch, String filePath) throws Exception {
        String projectId = projectIdByName(key, groupName, projectName);
        JsonNode jsonNode = get(key, String.format("/projects/%s/repository/files/%s/raw", projectId, URLEncoder.encode(filePath, "UTF-8")), LinkedHashMap.<String, Object>of("ref", branch).filterValues(Objects::nonNull).toJavaMap());
        if (Objects.isNull(jsonNode)) {
            return null;
        }
        return jsonNode.get("response").asText();
    }

}
