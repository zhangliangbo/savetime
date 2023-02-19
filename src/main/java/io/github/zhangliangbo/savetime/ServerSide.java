package io.github.zhangliangbo.savetime;

/**
 * @author zhangliangbo
 * @since 2023/2/18
 */
public class ServerSide {
    public static void main(String[] args) {
        ST.netty.httpHelloWorldServer(9999, false).join();
    }
}
