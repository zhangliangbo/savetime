package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.github.zhangliangbo.savetime.ST;
import io.vavr.collection.Stream;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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

}
