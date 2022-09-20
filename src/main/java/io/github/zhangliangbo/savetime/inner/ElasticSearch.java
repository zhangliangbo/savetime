package io.github.zhangliangbo.savetime.inner;

import org.elasticsearch.client.RestHighLevelClient;

/**
 * @author zhangliangbo
 * @since 2022/9/20
 */
public class ElasticSearch extends AbstractConfigurable<RestHighLevelClient> {

    @Override
    protected boolean isValid(RestHighLevelClient restHighLevelClient) {
        return true;
    }

    @Override
    protected RestHighLevelClient create(String key) throws Exception {
        return null;
    }
}
