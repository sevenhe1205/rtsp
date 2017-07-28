package com.tuya;

import com.tuya.handler.RtspHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.security.NoSuchAlgorithmException;


/**
 * Created by heshaoqiong on 2017/7/18.
 */
public class ServerInitializer extends ChannelInitializer<SocketChannel> {



    public ServerInitializer() throws NoSuchAlgorithmException {


    }



    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        SSLEngine engine = SecureSslContextFactory.getServerContext().createSSLEngine();
        engine.setUseClientMode(false);
        engine.setNeedClientAuth(false);
        //pipeline.addFirst(new SslHandler(engine));
	//pipeline.addLast(new HttpRequestDecoder());
        //pipeline.addLast(new HttpResponseEncoder());
        pipeline.addLast("decoder",new RtspDecoder());
        pipeline.addLast("encoder",new RtspEncoder());
        pipeline.addLast("handler", new RtspHandler());
        pipeline.addLast("logging",new LoggingHandler(LogLevel.WARN));

    }
}
