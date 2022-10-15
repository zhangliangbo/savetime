package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;


/**
 * @author zhangliangbo
 * @since 2022-10-15
 */
public class Lucene {

    /**
     * 列出所有的分词器
     *
     * @return 分词器
     */
    public ArrayNode analyzer() {
        ConfigurationBuilder builder = new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage("org.apache.lucene.analysis"))
                .addUrls(ClasspathHelper.forPackage("org.wltea.analyzer.lucene"));
        Reflections reflections = new Reflections(builder);
        Set<Class<? extends Analyzer>> subTypes = reflections.getSubTypesOf(Analyzer.class);
        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
        for (Class<?> subType : subTypes) {
            if (subType.isAnonymousClass()) {
                continue;
            }
            arrayNode.add(subType.getName());
        }
        return arrayNode;
    }

    /**
     * 分词
     *
     * @param content 内容
     * @param cls     分词器全名类
     * @return 结果
     * @throws IOException 异常
     */
    public ArrayNode token(String content, String cls) throws Exception {
        Class<Analyzer> analyzerClass = (Class<Analyzer>) Class.forName(cls);
        Analyzer analyzer = analyzerClass.newInstance();
        return token(content, analyzer);
    }

    /**
     * 分词
     *
     * @param content  内容
     * @param analyzer 分词器
     * @return 结果
     * @throws IOException 异常
     */
    public ArrayNode token(String content, Analyzer analyzer) throws IOException {
        TokenStream ts = analyzer.tokenStream("fieldName", new StringReader(content));

        ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);

        try {
            ts.reset();
            while (ts.incrementToken()) {
                arrayNode.add(new TextNode(ts.reflectAsString(false)));
            }
            ts.end();
        } finally {
            ts.close();
        }
        analyzer.close();

        return arrayNode;
    }

}
