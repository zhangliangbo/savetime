package io.github.zhangliangbo.savetime.inner;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.zhangliangbo.savetime.ST;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.core.MainResponse;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.cluster.health.ClusterIndexHealth;
import org.elasticsearch.cluster.health.ClusterShardHealth;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhangliangbo
 * @since 2022/9/20
 */
public class ElasticSearch extends AbstractConfigurable<RestHighLevelClient> {

    @Override
    protected boolean isValid(RestHighLevelClient restHighLevelClient) {
        try {
            return restHighLevelClient.ping(RequestOptions.DEFAULT);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    protected RestHighLevelClient create(String key) throws Exception {
        JsonNode conf = ST.io.readTree(getConfig()).get(key);

        JsonNode ssh = conf.get("ssh");
        Iterator<JsonNode> iterator = conf.get("uris").elements();
        List<String> urls = new LinkedList<>();
        while (iterator.hasNext()) {
            JsonNode next = iterator.next();
            String url = next.asText();
            URI uri = URI.create(url);
            url = ST.ssh.forward(ssh, uri, url);
            urls.add(url);
        }
        HttpHost[] hosts = urls.stream().map(HttpHost::create).toArray(HttpHost[]::new);
        RestClientBuilder builder = RestClient.builder(hosts);

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(conf.get("username").asText(), conf.get("password").asText());
        credentialsProvider.setCredentials(AuthScope.ANY, credentials);
        builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        RestHighLevelClient restHighLevelClient = new RestHighLevelClient(builder);
        System.out.println("客户端创建完毕 " + key);
        return restHighLevelClient;
    }

    /**
     * ping服务
     *
     * @param key 环境
     * @return 是否通畅
     * @throws Exception 异常
     */
    public boolean ping(String key) throws Exception {
        return getOrCreate(key).ping(RequestOptions.DEFAULT);
    }

    /**
     * 索引是否存在
     *
     * @param key   环境
     * @param index 索引
     * @return 是否存在
     * @throws Exception 异常
     */
    public boolean indexExist(String key, String index) throws Exception {
        GetIndexRequest request = new GetIndexRequest(index);
        return getOrCreate(key).indices().exists(request, RequestOptions.DEFAULT);
    }

    /**
     * 索引信息
     *
     * @param key     环境
     * @param indices 索引
     * @return 信息
     * @throws Exception 异常
     */
    public JsonNode indexInfo(String key, String... indices) throws Exception {
        GetIndexRequest request = new GetIndexRequest(indices);
        GetIndexResponse response = getOrCreate(key).indices().get(request, RequestOptions.DEFAULT);
        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);

        ObjectNode settingNode = new ObjectNode(JsonNodeFactory.instance);
        for (Map.Entry<String, Settings> entry : response.getSettings().entrySet()) {
            settingNode.set(entry.getKey(), ST.io.readTree(entry.getValue().toString()));
        }
        objectNode.set("settings", settingNode);


        ObjectNode mappingNode = new ObjectNode(JsonNodeFactory.instance);
        for (Map.Entry<String, MappingMetadata> entry : response.getMappings().entrySet()) {
            mappingNode.set(entry.getKey(), ST.io.readTree(entry.getValue().source().toString()));
        }
        objectNode.set("mappings", mappingNode);

        ObjectNode aliasNode = new ObjectNode(JsonNodeFactory.instance);
        for (Map.Entry<String, List<AliasMetadata>> entry : response.getAliases().entrySet()) {
            List<String> aliases = entry.getValue().stream().map(AliasMetadata::alias).collect(Collectors.toList());
            aliasNode.set(entry.getKey(), ST.io.toJsonNode(aliases));
        }
        objectNode.set("aliases", aliasNode);

        objectNode.set("indices", ST.io.toJsonNode(response.getIndices()));

        return objectNode;
    }

    /**
     * 创建索引
     *
     * @param key   环境
     * @param index 索引
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode indexCreate(String key, String index) throws Exception {
        return indexCreate(key, index, null);
    }

    /**
     * 创建索引
     *
     * @param key   环境
     * @param index 索引
     * @param alias 别名
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode indexCreate(String key, String index, Map<String, String> alias) throws Exception {
        return indexCreate(key, index, alias, null);
    }

    /**
     * 创建索引
     *
     * @param key      环境
     * @param index    索引
     * @param alias    别名
     * @param mappings 映射
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode indexCreate(String key, String index, Map<String, String> alias, Map<String, Object> mappings) throws Exception {
        Map<String, Object> settings = new LinkedHashMap<>();
        JsonNode jsonNode = clusterHealth(key);
        int numberOfDataNodes = jsonNode.get("numberOfDataNodes").asInt();
        settings.put("index.number_of_shards", numberOfDataNodes);
        settings.put("index.number_of_replicas", 1);
        return indexCreate(key, index, alias, mappings, settings);
    }

    /**
     * 创建索引
     *
     * @param key      环境
     * @param index    索引
     * @param alias    别名
     * @param mappings 映射
     * @param settings 设置
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode indexCreate(String key, String index, Map<String, String> alias, Map<String, Object> mappings, Map<String, Object> settings) throws Exception {
        CreateIndexRequest request = new CreateIndexRequest(index);
        if (Objects.nonNull(alias)) {
            request.aliases(alias);
        }
        if (Objects.nonNull(mappings)) {
            request.mapping(mappings);
        }
        if (Objects.nonNull(settings)) {
            request.settings(settings);
        }
        CreateIndexResponse response = getOrCreate(key).indices().create(request, RequestOptions.DEFAULT);
        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
        objectNode.put("acknowledged", response.isAcknowledged());
        objectNode.put("shardsAcknowledged", response.isShardsAcknowledged());
        objectNode.put("index", response.index());
        return objectNode;
    }

    /**
     * 删除索引
     *
     * @param key     环境
     * @param indices 索引
     * @return 是否成功
     * @throws Exception 异常
     */
    public boolean indexDelete(String key, String... indices) throws Exception {
        DeleteIndexRequest request = new DeleteIndexRequest(indices);
        AcknowledgedResponse response = getOrCreate(key).indices().delete(request, RequestOptions.DEFAULT);
        return response.isAcknowledged();
    }

    /**
     * ES信息
     *
     * @param key 环境
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode info(String key) throws Exception {
        MainResponse response = getOrCreate(key).info(RequestOptions.DEFAULT);
        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
        objectNode.put("clusterName", response.getClusterName());
        objectNode.put("clusterUuid", response.getClusterUuid());
        objectNode.put("nodeName", response.getNodeName());
        objectNode.put("tagline", response.getTagline());

        ObjectNode versionNode = new ObjectNode(JsonNodeFactory.instance);
        MainResponse.Version version = response.getVersion();
        versionNode.put("buildDate", version.getBuildDate());
        versionNode.put("buildFlavor", version.getBuildFlavor());
        versionNode.put("buildHash", version.getBuildHash());
        versionNode.put("buildType", version.getBuildType());
        versionNode.put("luceneVersion", version.getLuceneVersion());
        versionNode.put("minimumIndexCompatibilityVersion", version.getMinimumIndexCompatibilityVersion());
        versionNode.put("minimumWireCompatibilityVersion", version.getMinimumWireCompatibilityVersion());
        versionNode.put("number", version.getNumber());
        versionNode.put("isSnapshot", version.isSnapshot());
        objectNode.set("version", versionNode);

        return objectNode;
    }

    /**
     * 集群信息
     *
     * @param key     环境
     * @param indices 索引
     * @return 应答
     * @throws Exception 异常
     */
    public JsonNode clusterHealth(String key, String... indices) throws Exception {
        ClusterHealthRequest request = new ClusterHealthRequest(indices);
        ClusterHealthResponse response = getOrCreate(key).cluster().health(request, RequestOptions.DEFAULT);

        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
        objectNode.put("status", response.getStatus().name());
        objectNode.put("numberOfNodes", response.getNumberOfNodes());
        objectNode.put("numberOfDataNodes", response.getNumberOfDataNodes());
        objectNode.put("activeShards", response.getActiveShards());
        objectNode.put("activePrimaryShards", response.getActivePrimaryShards());
        objectNode.put("relocatingShards", response.getRelocatingShards());
        objectNode.put("initializingShards", response.getInitializingShards());
        objectNode.put("unassignedShards", response.getUnassignedShards());
        objectNode.put("delayedUnassignedShards", response.getDelayedUnassignedShards());
        objectNode.put("activeShardsPercent", response.getActiveShardsPercent());
        objectNode.put("taskMaxWaitingTime", response.getTaskMaxWaitingTime().toString());
        objectNode.put("numberOfPendingTasks", response.getNumberOfPendingTasks());
        objectNode.put("numberOfInFlightFetch", response.getNumberOfInFlightFetch());

        ObjectNode indicesNode = new ObjectNode(JsonNodeFactory.instance);
        for (Map.Entry<String, ClusterIndexHealth> entry : response.getIndices().entrySet()) {
            ObjectNode indexNode = new ObjectNode(JsonNodeFactory.instance);
            indexNode.put("index", entry.getValue().getIndex());
            indexNode.put("status", entry.getValue().getStatus().name());
            indexNode.put("numberOfShards", entry.getValue().getNumberOfShards());
            indexNode.put("numberOfReplicas", entry.getValue().getNumberOfReplicas());
            indexNode.put("activeShards", entry.getValue().getActiveShards());
            indexNode.put("activePrimaryShards", entry.getValue().getActivePrimaryShards());
            indexNode.put("initializingShards", entry.getValue().getInitializingShards());
            indexNode.put("relocatingShards", entry.getValue().getRelocatingShards());
            indexNode.put("unassignedShards", entry.getValue().getUnassignedShards());

            ObjectNode shardsNode = new ObjectNode(JsonNodeFactory.instance);
            for (Map.Entry<Integer, ClusterShardHealth> shardHealthEntry : entry.getValue().getShards().entrySet()) {
                ObjectNode shardNode = new ObjectNode(JsonNodeFactory.instance);
                shardNode.put("shardId", shardHealthEntry.getValue().getShardId());
                shardNode.put("status", shardHealthEntry.getValue().getStatus().name());
                shardNode.put("activeShards", shardHealthEntry.getValue().getActiveShards());
                shardNode.put("initializingShards", shardHealthEntry.getValue().getInitializingShards());
                shardNode.put("unassignedShards", shardHealthEntry.getValue().getUnassignedShards());
                shardNode.put("relocatingShards", shardHealthEntry.getValue().getRelocatingShards());
                shardNode.put("primaryActive", shardHealthEntry.getValue().isPrimaryActive());

                shardsNode.set(entry.getKey(), shardNode);
            }
            indicesNode.set("shards", shardsNode);

            indicesNode.set(entry.getKey(), indexNode);
        }
        objectNode.set("indices", indicesNode);

        return objectNode;
    }

    /**
     * 字段映射信息
     *
     * @param key     环境
     * @param indices 索引
     * @return 字段映射
     * @throws Exception 异常
     */
    public JsonNode mapping(String key, String... indices) throws Exception {
        GetMappingsRequest request = new GetMappingsRequest().indices(indices);
        GetMappingsResponse response = getOrCreate(key).indices().getMapping(request, RequestOptions.DEFAULT);
        ObjectNode jsonNode = new ObjectNode(JsonNodeFactory.instance);
        for (Map.Entry<String, MappingMetadata> entry : response.mappings().entrySet()) {
            jsonNode.set(entry.getKey(), ST.io.toJsonNode(entry.getValue().getSourceAsMap()));
        }
        return jsonNode;
    }

    /**
     * 设置信息
     *
     * @param key     环境
     * @param indices 索引
     * @return 设置
     * @throws Exception 异常
     */
    public JsonNode setting(String key, String... indices) throws Exception {
        GetSettingsRequest request = new GetSettingsRequest().indices(indices);
        GetSettingsResponse response = getOrCreate(key).indices().getSettings(request, RequestOptions.DEFAULT);

        ObjectNode jsonNode = new ObjectNode(JsonNodeFactory.instance);
        for (ObjectObjectCursor<String, Settings> entry : response.getIndexToSettings()) {
            jsonNode.set(entry.key, ST.io.readTree(entry.value.toString()));
        }
        return jsonNode;
    }

    /**
     * 别名
     *
     * @param key   环境
     * @param alias 别名
     * @return 别名
     * @throws Exception 异常
     */
    public JsonNode alias(String key, String... alias) throws Exception {
        GetAliasesRequest request = new GetAliasesRequest(alias);
        GetAliasesResponse response = getOrCreate(key).indices().getAlias(request, RequestOptions.DEFAULT);

        ObjectNode jsonNode = new ObjectNode(JsonNodeFactory.instance);
        for (Map.Entry<String, Set<AliasMetadata>> entry : response.getAliases().entrySet()) {
            Set<String> set = entry.getValue().stream().map(AliasMetadata::getAlias).collect(Collectors.toSet());
            jsonNode.set(entry.getKey(), ST.io.toJsonNode(set));
        }

        return jsonNode;
    }

    /**
     * 别名
     *
     * @param key   环境
     * @param index 索引
     * @param alia  别名
     * @return 别名
     * @throws Exception 异常
     */
    public boolean aliasAdd(String key, String index, String alia) throws Exception {
        IndicesAliasesRequest request = new IndicesAliasesRequest();
        IndicesAliasesRequest.AliasActions aliasAction = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                .index(index).alias(alia);
        request.addAliasAction(aliasAction);
        AcknowledgedResponse response = getOrCreate(key).indices().updateAliases(request, RequestOptions.DEFAULT);
        return response.isAcknowledged();
    }

    /**
     * 别名是否存在
     *
     * @param key   环境
     * @param alias 别名
     * @return 是否存在
     * @throws Exception 异常
     */
    public boolean aliasExist(String key, String... alias) throws Exception {
        GetAliasesRequest request = new GetAliasesRequest(alias);
        return getOrCreate(key).indices().existsAlias(request, RequestOptions.DEFAULT);
    }

    /**
     * 删除别名
     *
     * @param key   环境
     * @param index 索引
     * @param alia  别名
     * @return 是否成功
     * @throws Exception 异常
     */
    public boolean aliasDelete(String key, String index, String alia) throws Exception {
        DeleteAliasRequest request = new DeleteAliasRequest(index, alia);
        org.elasticsearch.client.core.AcknowledgedResponse response = getOrCreate(key).indices().deleteAlias(request, RequestOptions.DEFAULT);
        return response.isAcknowledged();
    }

    /**
     * 移动别名
     *
     * @param key       环境
     * @param fromIndex 开始索引
     * @param toIndex   结束索引
     * @param alia      别名
     * @return 是否成功
     * @throws Exception 异常
     */
    public boolean aliasMove(String key, String fromIndex, String toIndex, String alia) throws Exception {
        IndicesAliasesRequest request = new IndicesAliasesRequest();
        IndicesAliasesRequest.AliasActions aliasAction1 = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
                .index(fromIndex).alias(alia);
        request.addAliasAction(aliasAction1);
        IndicesAliasesRequest.AliasActions aliasAction2 = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                .index(toIndex).alias(alia);
        request.addAliasAction(aliasAction2);
        AcknowledgedResponse response = getOrCreate(key).indices().updateAliases(request, RequestOptions.DEFAULT);
        return response.isAcknowledged();
    }

}
