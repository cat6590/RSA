package com.ricedotwho.rsa.network;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponseDecoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

public class Socks5CommandRequestHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final InetSocketAddress destination;

    public Socks5CommandRequestHandler(InetSocketAddress destination) {
        this.destination = destination;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Don't fire yet, wait for SOCKS5 handshake to complete
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        LOGGER.info("[SOCKS5] CommandHandler received: {}", msg.getClass().getSimpleName());

        if (msg instanceof Socks5CommandResponse res) {
            LOGGER.info("[SOCKS5] Command response status: {}", res.status());

            if (res.status() == Socks5CommandStatus.SUCCESS) {
                LOGGER.info("[SOCKS5] Tunnel established to {}:{}", destination.getHostString(), destination.getPort());

                // Remove all SOCKS5 handlers
                ctx.pipeline().remove(Socks5ClientEncoder.class);
                ctx.pipeline().remove(Socks5CommandResponseDecoder.class);
                ctx.pipeline().remove(this);

                LOGGER.info("[SOCKS5] Pipeline after cleanup: {}", ctx.pipeline().names());

                // Now trigger MC handshake
                ctx.fireChannelActive();
            } else {
                LOGGER.error("[SOCKS5] Tunnel FAILED - status: {}", res.status());
                ctx.close();
            }
        } else {
            LOGGER.warn("[SOCKS5] Unexpected message: {}", msg.getClass().getName());
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("[SOCKS5] Exception in CommandRequestHandler", cause);
        ctx.close();
    }
}