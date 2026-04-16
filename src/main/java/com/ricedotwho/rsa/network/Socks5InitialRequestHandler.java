package com.ricedotwho.rsa.network;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.socksx.v5.*;
import org.slf4j.Logger;

public class Socks5InitialRequestHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info("[SOCKS5] Channel active, sending initial request to {}", ctx.channel().remoteAddress());
        // Advertise both auth methods, proxy will pick one
        ctx.writeAndFlush(new DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH, Socks5AuthMethod.PASSWORD))
                .addListener(f -> {
                    if (f.isSuccess()) LOGGER.info("[SOCKS5] Initial request sent successfully");
                    else LOGGER.error("[SOCKS5] Failed to send initial request", f.cause());
                });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        LOGGER.info("[SOCKS5] Received message: {}", msg.getClass().getSimpleName());

        if (msg instanceof Socks5InitialResponse res) {
            LOGGER.info("[SOCKS5] Initial response - auth method: {}", res.authMethod());

            if (res.authMethod() == Socks5AuthMethod.NO_AUTH) {
                LOGGER.info("[SOCKS5] No auth required, handing off to command handler");
                ctx.pipeline().replace(Socks5InitialResponseDecoder.class,
                        "cmdDecoder", new Socks5CommandResponseDecoder());
                ctx.pipeline().remove(this);

            } else if (res.authMethod() == Socks5AuthMethod.PASSWORD) {
                LOGGER.info("[SOCKS5] Password auth required, sending credentials...");
                ctx.pipeline().replace(Socks5InitialResponseDecoder.class,
                        "passwordDecoder", new Socks5PasswordAuthResponseDecoder());
                ctx.writeAndFlush(new DefaultSocks5PasswordAuthRequest("username", "password"))
                        .addListener(f -> {
                            if (f.isSuccess()) LOGGER.info("[SOCKS5] Password auth request sent");
                            else LOGGER.error("[SOCKS5] Failed to send password auth", f.cause());
                        });

            } else {
                LOGGER.error("[SOCKS5] Unaccepted/unknown auth method: {}", res.authMethod());
                ctx.close();
            }

        } else if (msg instanceof Socks5PasswordAuthResponse res) {
            LOGGER.info("[SOCKS5] Password auth response - status: {}", res.status());

            if (res.status() == Socks5PasswordAuthStatus.SUCCESS) {
                LOGGER.info("[SOCKS5] Auth successful, handing off to command handler");
                ctx.pipeline().replace(Socks5PasswordAuthResponseDecoder.class,
                        "cmdDecoder", new Socks5CommandResponseDecoder());
                ctx.pipeline().remove(this);
            } else {
                LOGGER.error("[SOCKS5] Auth FAILED - wrong username/password");
                ctx.close();
            }

        } else {
            LOGGER.warn("[SOCKS5] Unexpected message type: {}", msg.getClass().getName());
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("[SOCKS5] Exception in InitialRequestHandler", cause);
        ctx.close();
    }
}