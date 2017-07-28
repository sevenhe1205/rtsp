package com.tuya;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4JLoggerFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;

import java.security.NoSuchAlgorithmException;

/**
 * Created by heshaoqiong on 2017/7/17.
 */
public class Main {
    public static final int PORT=443;


    public static void main(String args[]) throws NoSuchAlgorithmException {
        LogManager.resetConfiguration();
        InternalLoggerFactory.setDefaultFactory( Log4JLoggerFactory.INSTANCE);
        PropertyConfigurator.configure ("classpath:log4j.properties");


        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap =new ServerBootstrap();
        bootstrap.group(bossGroup,workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.TRACE))
                .childHandler(new ServerInitializer());
        try {
            ChannelFuture channelFuture = bootstrap.bind(PORT).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
