package io.github.zhangliangbo.savetime;

import io.github.zhangliangbo.savetime.inner.*;
import io.vavr.collection.List;

/**
 * @author zhangliangbo
 * @since 2022/8/27
 */
public final class ST {
    public static final Ssh ssh = new Ssh();
    public static final Http http = new Http();
    public static final Jdbc jdbc = new Jdbc();
    public static final Lettuce lettuce = new Lettuce();
    public static final IO io = new IO();
    public static final RabbitWeb rabbitWeb = new RabbitWeb();
    public static final Mail mail = new Mail();
    public static final ElasticSearch elasticSearch = new ElasticSearch();
    public static final StCollection collection = new StCollection();
    public static final Nacos nacos = new Nacos();
    public static final Lucene lucene = new Lucene();
    public static final JenkinsRest jenkinsRest = new JenkinsRest();
    public static final Cf cf = new Cf();
    public static final Gitlab gitlab = new Gitlab();
    public static final Rabbit rabbit = new Rabbit();
    public static final Rand rand = new Rand();
    public static final MybatisPlusGenerator mybatisPlusGenerator = new MybatisPlusGenerator();

    public static <T> List<T> listOf(Iterable<T> iterable) {
        return List.ofAll(iterable);
    }

    @SafeVarargs
    public static <T> List<T> listOf(T... t) {
        return List.of(t);
    }

}
