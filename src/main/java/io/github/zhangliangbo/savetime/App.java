package io.github.zhangliangbo.savetime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;

/**
 * @author zhangliangbo
 * @since 2022/8/27
 */
public class App {
    public static void main(String[] args) throws Exception {
        SynchronousQueue<String> queue = new SynchronousQueue<>();
        CompletableFuture<Void> completableFuture = ST.netty.echoClient("127.0.0.1", 9999, queue);
        System.out.println(completableFuture);
        Thread.sleep(5000);
        queue.offer("1");
        queue.offer("2");
        completableFuture.join();
    }
}
