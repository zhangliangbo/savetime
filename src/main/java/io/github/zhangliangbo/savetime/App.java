package io.github.zhangliangbo.savetime;

import java.util.concurrent.SynchronousQueue;

/**
 * @author zhangliangbo
 * @since 2022/8/27
 */
public class App {
    public static void main(String[] args) throws Exception {

        System.out.println(ST.netty.echoClient("127.0.0.1", 9999,new SynchronousQueue<>()));
    }
}
