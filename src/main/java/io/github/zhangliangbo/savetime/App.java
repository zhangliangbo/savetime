package io.github.zhangliangbo.savetime;

import java.util.concurrent.CompletableFuture;

/**
 * @author zhangliangbo
 * @since 2022/8/27
 */
public class App {
    public static void main(String[] args) throws Exception {
        CompletableFuture<Void> completableFuture = ST.netty.snoopServer(9999, false);
        completableFuture.join();
    }
}
