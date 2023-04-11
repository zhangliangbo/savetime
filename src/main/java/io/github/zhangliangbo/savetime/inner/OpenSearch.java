package io.github.zhangliangbo.savetime.inner;

import com.aliyun.opensearch.OpenSearchClient;
import com.aliyun.opensearch.SearcherClient;
import com.aliyun.opensearch.sdk.generated.search.Config;
import com.aliyun.opensearch.sdk.generated.search.SearchFormat;
import com.aliyun.opensearch.sdk.generated.search.SearchParams;
import com.aliyun.opensearch.sdk.generated.search.general.SearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.github.zhangliangbo.savetime.ST;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author zhangliangbo
 * @since 2023/3/15
 */
public class OpenSearch extends AbstractConfigurable<OpenSearchClient> {

    @Override
    protected boolean isValid(OpenSearchClient openSearchClient) {
        return !openSearchClient.isExpired();
    }

    @Override
    protected OpenSearchClient create(String key) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode treeNode = objectMapper.readTree(getConfig());
        JsonNode os = treeNode.get(key);
        String accessKey = os.get("accessKey").asText();
        String secret = os.get("secret").asText();
        String host = os.get("host").asText();
        com.aliyun.opensearch.sdk.generated.OpenSearch openSearch = new com.aliyun.opensearch.sdk.generated.OpenSearch(accessKey, secret, host);
        return new OpenSearchClient(openSearch);
    }

    /**
     * 搜索
     *
     * @param key   环境
     * @param app   程序
     * @param query 查询语句
     * @return 结果
     * @throws Exception 异常
     */
    public JsonNode search(String key, String app, String query, String filter, int start, int size) throws Exception {
        List<String> appList = Lists.newArrayList(app);
        Config config = new Config(appList);
        config.setStart(start);
        config.setHits(size);
        config.setSearchFormat(SearchFormat.JSON);
        SearchParams searchParams = new SearchParams(config);
        if (StringUtils.isNotBlank(query)) {
            searchParams.setQuery(query);
        }
        if (StringUtils.isNotBlank(filter)) {
            searchParams.setFilter(filter);
        }
        SearcherClient searcherClient = new SearcherClient(getOrCreate(key));
        SearchResult searchResult = searcherClient.execute(searchParams);
        String result = searchResult.getResult();
        return ST.io.readTree(result);
    }


    /**
     * 搜索
     *
     * @param key   环境
     * @param app   程序
     * @param query 查询语句
     * @return 结果
     * @throws Exception 异常
     */
    public JsonNode search(String key, String app, String query, String filter) throws Exception {
        return search(key, app, query, filter, 0, 1);
    }

}
