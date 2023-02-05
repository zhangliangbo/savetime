package io.github.zhangliangbo.savetime.inner.netty.echo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangliangbo
 * @since 2023-02-04
 */
@Slf4j
public class EchoClientHandler extends ChannelInboundHandlerAdapter {

    private final EventLoopGroup handler = new DefaultEventLoopGroup();

    private final BlockingQueue<String> blockingQueue;

    public EchoClientHandler(BlockingQueue<String> blockingQueue) {
        this.blockingQueue = blockingQueue;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handler.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        String take = blockingQueue.take();
                        log.info("发送 {}", take);
                        ctx.writeAndFlush(Unpooled.copiedBuffer(take, CharsetUtil.UTF_8));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            log.info("{}={}", msg.getClass().getName(), ((ByteBuf) msg).toString(CharsetUtil.UTF_8));
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.info("报错 ", cause);
        ctx.close();
    }

}
