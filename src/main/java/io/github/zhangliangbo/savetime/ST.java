package io.github.zhangliangbo.savetime;

import io.github.zhangliangbo.savetime.inner.*;
import io.github.zhangliangbo.savetime.inner.method.*;
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
    public static final Vonage vonage = new Vonage();
    public static final Thymeleaf thymeleaf = new Thymeleaf();
    public static final Netty netty = new Netty();
    public static final Flink flink = new Flink();
    public static final Time time = new Time();
    public static final NepxionDiscovery nepxionDiscovery = new NepxionDiscovery();
    public static final OpenSearch openSearch = new OpenSearch();
    public static final StMath math = new StMath();
    public static final DynamicProgramming dp = new DynamicProgramming();
    public static final StString string = new StString();
    public static final StStack stack = new StStack();
    public static final StQueue queue = new StQueue();
    public static final Data data = new Data();
    public static final StJsoup jsoup = new StJsoup();
    public static final BackTracking backTracking = new BackTracking();
    public static final DivideConquer divideConquer = new DivideConquer();
    public static final Greedy greedy = new Greedy();
    public static final Recursion recursion = new Recursion();
    public static final Iteration iteration = new Iteration();

    public static <T> List<T> listOf(Iterable<T> iterable) {
        return List.ofAll(iterable);
    }

    @SafeVarargs
    public static <T> List<T> listOf(T... t) {
        return List.of(t);
    }

}
