package io.github.zhangliangbo.savetime.inner;

import io.github.zhangliangbo.savetime.inner.netty.ServerUtil;
import io.github.zhangliangbo.savetime.inner.netty.echo.EchoClientHandler;
import io.github.zhangliangbo.savetime.inner.netty.echo.EchoServerHandler;
import io.github.zhangliangbo.savetime.inner.netty.http.file.HttpStaticFileServerInitializer;
import io.github.zhangliangbo.savetime.inner.netty.http.helloworld.HttpHelloWorldServerInitializer;
import io.github.zhangliangbo.savetime.inner.netty.http.snoop.HttpSnoopClientInitializer;
import io.github.zhangliangbo.savetime.inner.netty.http.snoop.HttpSnoopServerInitializer;
import io.github.zhangliangbo.savetime.inner.netty.http.upload.HttpUploadServerInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

/**
 * https://netty.io/wiki/index.html
 * https://github.com/netty/netty
 *
 * @author zhangliangbo
 * @since 2022-10-15
 */
@Slf4j
public class Netty {
    /**
     * 回声服务器
     *
     * @param port 端口
     * @return 插座关闭的结果
     */
    public CompletableFuture<Void> echoServer(int port) {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
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
                        log.info("插座已绑定 {}", port);
                        //等待直到服务器插座关闭
                        f.channel().closeFuture().sync();
                    } finally {
                        //关闭所有事件循环以终止所有线程
                        bossGroup.shutdownGracefully();
                        workerGroup.shutdownGracefully();
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }
        });
    }

    public CompletableFuture<Void> echoClient(String host, int port, BlockingQueue<String> blockingQueue) throws CertificateException, SSLException, InterruptedException {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    //配置SSL
                    final SslContext sslCtx = ServerUtil.buildSslContext();

                    //配置客户端
                    EventLoopGroup group = new NioEventLoopGroup();
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
                                        p.addLast(new EchoClientHandler(blockingQueue));
                                    }
                                });

                        //启动客户端
                        ChannelFuture f = b.connect(host, port).sync();
                        log.info("插座已连接 {} {}", host, port);
                        //等待直到连接关闭
                        f.channel().closeFuture().sync();
                    } finally {
                        //关闭消息循环以终止所有线程
                        group.shutdownGracefully();
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }
        });
    }

    public CompletableFuture<Void> httpSnoopServer(int port, boolean ssl) {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    //配置SSL
                    final SslContext sslCtx = ServerUtil.buildSslContext();

                    //配置服务器
                    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
                    EventLoopGroup workerGroup = new NioEventLoopGroup();
                    try {
                        ServerBootstrap b = new ServerBootstrap();
                        b.group(bossGroup, workerGroup)
                                .channel(NioServerSocketChannel.class)
                                .handler(new LoggingHandler(LogLevel.INFO))
                                .childHandler(new HttpSnoopServerInitializer(sslCtx));

                        ChannelFuture f = b.bind(port).sync();

                        log.info("打开浏览器并导航到{}://127.0.0.1:{}/", (ssl ? "https" : "http"), port);

                        f.channel().closeFuture().sync();

                    } finally {
                        bossGroup.shutdownGracefully();
                        workerGroup.shutdownGracefully();
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }
        });
    }

    public void httpSnoopClient(String url) throws URISyntaxException, SSLException, InterruptedException {
        URI uri = new URI(url);
        //必要的话配置SSL上下文
        final boolean ssl = "https".equalsIgnoreCase(uri.getScheme());
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        //配置客户端
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new HttpSnoopClientInitializer(sslCtx));

            //尝试建立连接
            ChannelFuture f = b.connect(uri.getHost(), uri.getPort()).sync();

            //准备HTTP请求
            HttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath(), Unpooled.EMPTY_BUFFER);
            request.headers().set(HttpHeaderNames.HOST, uri.getHost());
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

            //设置一些文本
            request.headers().set(
                    HttpHeaderNames.COOKIE,
                    ClientCookieEncoder.STRICT.encode(
                            new DefaultCookie("my-cookie", "foo"),
                            new DefaultCookie("another-cookie", "bar")));

            //发送HTTP请求
            f.channel().writeAndFlush(request);

            //等待服务器关闭连接
            f.channel().closeFuture().sync();
        } finally {
            //关闭所有的执行线程以退出
            group.shutdownGracefully();
        }
    }

    public CompletableFuture<Void> httpStaticFileServer(int port, boolean ssl) {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                EventLoopGroup bossGroup = new NioEventLoopGroup(1);
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    final SslContext sslCtx = ServerUtil.buildSslContext();
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .handler(new LoggingHandler(LogLevel.INFO))
                            .childHandler(new HttpStaticFileServerInitializer(sslCtx));

                    Channel ch = b.bind(port).sync().channel();

                    log.info("打开浏览器并导航到{}://127.0.0.1:{}/", (ssl ? "https" : "http"), port);

                    ch.closeFuture().sync();
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage());
                } finally {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            }
        });
    }

    public CompletableFuture<Void> httpHelloWorldServer(int port, boolean ssl) {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                // Configure the server.
                EventLoopGroup bossGroup = new NioEventLoopGroup(1);
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    // Configure SSL.
                    final SslContext sslCtx = ServerUtil.buildSslContext();
                    ServerBootstrap b = new ServerBootstrap();
                    b.option(ChannelOption.SO_BACKLOG, 1024);
                    b.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .handler(new LoggingHandler(LogLevel.INFO))
                            .childHandler(new HttpHelloWorldServerInitializer(sslCtx));

                    Channel ch = b.bind(port).sync().channel();

                    System.err.println("Open your web browser and navigate to " +
                            (ssl ? "https" : "http") + "://127.0.0.1:" + port + '/');

                    ch.closeFuture().sync();
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage());
                } finally {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            }
        });
    }

    public CompletableFuture<Void> httpUploadServer(int port, boolean ssl) {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                EventLoopGroup bossGroup = new NioEventLoopGroup(1);
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    // Configure SSL.
                    final SslContext sslCtx = ServerUtil.buildSslContext();
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(bossGroup, workerGroup);
                    b.channel(NioServerSocketChannel.class);
                    b.handler(new LoggingHandler(LogLevel.INFO));
                    b.childHandler(new HttpUploadServerInitializer(sslCtx));

                    Channel ch = b.bind(port).sync().channel();

                    System.err.println("Open your web browser and navigate to " +
                            (ssl ? "https" : "http") + "://127.0.0.1:" + port + '/');

                    ch.closeFuture().sync();
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage());
                } finally {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            }
        });
    }

}
