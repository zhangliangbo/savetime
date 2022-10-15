package io.github.zhangliangbo.savetime;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.hc.core5.http.ContentType;

import java.io.File;
import java.io.IOException;

/**
 * @author zhangliangbo
 * @since 2022/8/27
 */
public class App {
    public static void main(String[] args) throws Exception {
        System.out.println(ST.lucene.analyzer());
    }
}
