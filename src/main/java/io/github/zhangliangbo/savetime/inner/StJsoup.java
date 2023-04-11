package io.github.zhangliangbo.savetime.inner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;

/**
 * @author zhangliangbo
 * @since 2023/4/11
 */
public class StJsoup {

    /**
     * 解析html
     *
     * @param html html
     * @return 节点文档
     */
    public Document parse(String html) {
        return Jsoup.parse(html);
    }

    /**
     * 解析html
     *
     * @param file file
     * @return 节点文档
     */
    public Document parse(File file) throws IOException {
        return Jsoup.parse(file);
    }

}
