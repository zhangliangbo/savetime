package io.github.zhangliangbo.savetime.inner;

import io.github.zhangliangbo.savetime.inner.netty.ServerUtil;
import io.github.zhangliangbo.savetime.inner.netty.echo.EchoClientHandler;
import io.github.zhangliangbo.savetime.inner.netty.echo.EchoServerHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultThreadFactory;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.concurrent.BlockingQueue;

/**
 * https://netty.io/wiki/index.html
 *
 * @author zhangliangbo
 * @since 2022-10-15
 */
public class Netty {
    /**
     * 回声服务器
     *
     * @param port 端口
     * @return 插座关闭的结果
     * @throws CertificateException 证书异常
     * @throws SSLException         SSL异常
     * @throws InterruptedException 中断异常
     */
    public ChannelFuture echoServer(int port) throws CertificateException, SSLException, InterruptedException {
        //配置SSL
        final SslContext sslCtx = ServerUtil.buildSslContext();

        //配置服务器
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        final EchoServerHandler serverHandler = new EchoServerHandler();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            //p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(serverHandler);
                        }
                    });

            //启动服务器
            ChannelFuture f = b.bind(port).sync();
            System.out.printf("插座已绑定 %s\n", port);
            //等待直到服务器插座关闭
            return f.channel().closeFuture().sync();
        } finally {
            //关闭所有事件循环以终止所有线程
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public ChannelFuture echoClient(String host, int port, BlockingQueue<String> blockingQueue) throws CertificateException, SSLException, InterruptedException {

        //配置SSL
        final SslContext sslCtx = ServerUtil.buildSslContext();

        //配置客户端
        EventLoopGroup group = new NioEventLoopGroup();
        EventLoopGroup handler = new DefaultEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                            }
                            //p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(handler, new EchoClientHandler());
                        }
                    });

            //启动客户端
            ChannelFuture f = b.connect(host, port).sync();

            //等待直到连接关闭
            return f.channel().closeFuture().sync();
        } finally {
            //关闭消息循环以终止所有线程
            group.shutdownGracefully();
        }
    }

}
