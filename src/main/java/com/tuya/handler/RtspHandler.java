package com.tuya.handler;


import com.tuya.Main;
import com.tuya.rtsp.RtpPacketization;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.*;
import io.netty.util.CharsetUtil;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.rtsp.RtspHeaderNames.CONNECTION;

/**
 * Created by heshaoqiong on 2017/7/20.
 */
public class RtspHandler extends SimpleChannelInboundHandler<DefaultHttpRequest> {
    private	static final String ROOT_DIRECTORY 			= "/home/heshaoqiong/packet/DarwinStreamingSrvrlinux-Linux/";
    Thread rtpThread = null;

    protected void channelRead0(ChannelHandlerContext ctx, DefaultHttpRequest request) throws Exception {
        if(request.method().equals(RtspMethods.TEARDOWN))
            handleRtspTEARDOWNMethod(ctx,request);
        else if(request.method().equals(RtspMethods.OPTIONS))
            handleRtspOPTIONSMethod(ctx,request);
        else if(request.method().equals(RtspMethods.DESCRIBE))
            handleRtspDESCRIBEMethod(ctx,request);
        else if(request.method().equals(RtspMethods.SETUP))
            handleRtspSETUPMethod(ctx,request);
        else if(request.method().equals(RtspMethods.PLAY))
            handleRtspPLAYMethod(ctx,request);
        else if(request.method().equals(RtspMethods.PAUSE))
            handleRtspPAUSEMethod(ctx,request);
        else{
	    handleException(ctx,request);
            System.err.println("Exception in ServerHandler");
	}

    }

    private void handleRtspTEARDOWNMethod(ChannelHandlerContext ctx, DefaultHttpRequest request) {
        System.out.println(request);
        if(rtpThread !=null) {
            rtpThread.interrupt();
        }
        System.err.println("RTP 传输中断!");
    }

    private void handleRtspPAUSEMethod(ChannelHandlerContext ctx, DefaultHttpRequest request) {
        showRequest(request);
        // TODO Auto-generated method stub
        System.out.println("PAUSE NOT YET");
    }

    private void handleRtspPLAYMethod(ChannelHandlerContext ctx, DefaultHttpRequest request) {
	    showRequest(request);
        FullHttpResponse response = null;
        String sessionID = request.headers().get(RtspHeaderNames.SESSION);
        String uri = request.uri();
        String path = uri.substring(uri.indexOf("8554")+4);
        String filePath = filePath();


        File file = new File(filePath);

        if (file.isDirectory() || !file.exists()) {
            return;
        }

        long rtpTime = 0;
        int trackID = 1;
        String rtpInfo = "url="+uri+"/trackID="+trackID+";seq=10000;rtptime="+rtpTime;
       // trackID=2;
       // rtpInfo = rtpInfo+",url="+uri+"/trackID="+trackID+";seq=10000;rtptime="+rtpTime;
        response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);
        response.headers().set(RtspHeaderNames.CSEQ,request.headers().get(RtspHeaderNames.CSEQ));
        response.headers().set(RtspHeaderNames.SESSION,sessionID);
        response.headers().set(RtspHeaderNames.RTP_INFO,rtpInfo);
        response.headers().set(RtspHeaderNames.RANGE,"npt=0.000-");



        showResponse(response);
	
        RtpPacketization rtpPacket = new RtpPacketization(ctx, filePath);
        rtpThread = new Thread(rtpPacket);
        rtpThread.start();
        writeResponseWithFuture(ctx, request, response);
    }
    private void handleRtspSETUPMethod(ChannelHandlerContext ctx, DefaultHttpRequest request) {
        // TODO Auto-generated method stub
        showRequest(request);

        FullHttpResponse response = null;

        URI uri = null;
        try {
            uri = new URI(request.uri());
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String path = uri.getPath();
        String filePath = filePath();


        File file = new File(filePath);
        if (file.isDirectory() || !file.exists()) {
            return;
        }

        String interleaved	= request.headers().get(RtspHeaderNames.TRANSPORT).substring(request.headers().get(RtspHeaderNames.TRANSPORT).indexOf("interleaved"));
        String channel = interleaved.substring(interleaved.indexOf("=")+1);
        String ssrc=null;
        if("0-1".equals(channel)){
            ssrc="00018787";
        }else {
            ssrc="00200c07";
        }
        String serverTransport = "RTP/AVP/TCP;unicast"
                + ";interleaved="+interleaved.substring(interleaved.indexOf("=")+1)
                +";ssrc="+ssrc;

        response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);
        response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
        response.headers().set(RtspHeaderNames.TRANSPORT, serverTransport);
        response.headers().set(RtspHeaderNames.SESSION,"1");
        showResponse(response);
        writeResponseWithFuture(ctx,request,response);

    }

    private void handleRtspDESCRIBEMethod(ChannelHandlerContext ctx, DefaultHttpRequest request) {
        // TODO Auto-generated method stub
        showRequest(request);

        String uri = request.uri();
        String ipAddress = ctx.channel().localAddress().toString();
        ipAddress = ipAddress.substring(1,ipAddress.indexOf(":"));
        String path = uri.substring(uri.indexOf("8554")+4);
        String filePath = filePath();


        String sdp = generateSDP(filePath,ipAddress);
        ByteBuf buffer = Unpooled.copiedBuffer(sdp, CharsetUtil.UTF_8);

        FullHttpResponse response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);
        response.headers().set(RtspHeaderNames.CSEQ, request.headers().get(RtspHeaderNames.CSEQ));
        response.headers().set(RtspHeaderNames.CONTENT_BASE,uri);
        response.headers().set(RtspHeaderNames.CONTENT_TYPE,"application/sdp");
        response.headers().set(RtspHeaderNames.CONTENT_LENGTH,String.valueOf(sdp.length()));
        response.content().writeBytes(buffer);
        buffer.release();
        showResponse(response,sdp);
        writeResponseWithFuture(ctx,request,response);
    }

    private String generateSDP(String filePath, String ipAddress) {
        // TODO Auto-generated method stub
        File file = new File(filePath);

        if(!file.exists()){
            System.err.println("File Not Exists");
            return null;
        }

        StringBuilder sdpStr = new StringBuilder()
                .append("v=0\n")									/* 会话版本 */
                .append("o=- "+System.currentTimeMillis()+" "+(System.currentTimeMillis()+1)+" IN IP4 "+ipAddress+"\n")
                .append("s=/sample_h264_300kbit.mp4\n")		/* 会话名称 */
                .append("c=IN IP4 0.0.0.0\n")
                .append("t=0 0\n")									/* 会话有效时间 */
                .append("a=control:*\n")							/* 会话控制 */
                .append("a=isma-compliance:2,2.0,2\n")
                .append("a=range:npt=0-  \n")
                .append("m=video 0 RTP/AVP 96\n")					/* 媒体信息 */
                .append("a=rtpmap:96 H264/90000\n")					/* 媒体的详细信息 */
                .append("a=fmtp:96 packetization-mode=1;profile-level-id=4D401E;sprop-parameter-sets=J01AHqkYMB73oA==,KM4NiA==\n")
                .append("a=control:trackID=1\n")					/* 媒体的详细信息 */
                .append("a=cliprect:0,0,480,380\n")
                .append("a=framesize:96 380-480\n")
                .append("a=mpeg4-esid:201\n");

                //.append("m=audio 0 RTP/AVP 97\n")					/* 媒体信息 */
                //.append("a=rtpmap:97 mpeg4-generic/22050/2\n")			/* 媒体的详细信息*/
                //.append("a=fmtp:97 profile-level-id=15;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3;config=1390\n")
                //.append("a=control:trackID=2\n")					/* 媒体访问控制变量指定 */
                //.append("a=mpeg4-esid:101\n");

        return sdpStr.toString();
    }

    private void handleRtspOPTIONSMethod(ChannelHandlerContext ctx, DefaultHttpRequest request) {
        // TODO Auto-generated method stub
        showRequest(request);
        String options = RtspMethods.OPTIONS.name()+", "+RtspMethods.DESCRIBE.name()+", "+RtspMethods.SETUP.name()
                +", "+RtspMethods.TEARDOWN.name()+", "+RtspMethods.PLAY.name()+", "+ RtspMethods.PAUSE.name();

        FullHttpResponse response = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);
        HttpHeaders headers = request.headers();
        if(headers != null) {
            HttpHeaders httpHeaders = response.headers();
            if(headers.get(RtspHeaderNames.CSEQ) != null){
                httpHeaders.set(RtspHeaderNames.CSEQ, headers.get(RtspHeaderNames.CSEQ));
            }

        }
        response.headers().set(RtspHeaderNames.PUBLIC,options);
        showResponse(response);
        writeResponseWithFuture(ctx,request,response);

    }

    private void writeResponseWithFuture(ChannelHandlerContext ctx,
                                         DefaultHttpRequest request, HttpResponse response) {
        // TODO Auto-generated method stub
        ChannelFuture responseFuture;
        ChannelFuture lastresponseFuture;

        responseFuture = ctx.write(response,ctx.newProgressivePromise());
        lastresponseFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        responseFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                System.err.println(future.channel() + " "+future.cause()+" "+future.isCancelled()+" "+future.isDone()+" "+future.isSuccess()+" "/*+future.sync()*/);
            }

            @Override
            public void operationProgressed(ChannelProgressiveFuture paramF,
                                            long paramLong1, long paramLong2) throws Exception {
                // TODO Auto-generated method stub

            }
        });
        if (!HttpUtil.isKeepAlive(request)) {
            lastresponseFuture.addListener(ChannelFutureListener.CLOSE);
        }

    }
	
    private void handleException(ChannelHandlerContext ctx, DefaultHttpRequest request) {

        System.out.println("Exception");
	FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer("I am ok"
                .getBytes()));
        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        ctx.write(response);
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        // TODO Auto-generated method stub
        super.exceptionCaught(ctx, cause);
    }

    private static String changeUriToAbsolutePath(String uri) {
	System.out.println("uri: "+uri);
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }

        uri = uri.replace('/', File.separatorChar);

        return ROOT_DIRECTORY+uri;
    }

   private static String filePath(){
	return "/home/heshaoqiong/packet/DarwinStreamingSrvrlinux-Linux/sample_h264_300kbit.mp4";
	}


	private void showRequest(DefaultHttpRequest request){
        System.out.println();
        System.out.println("====================requeset==========================");
        System.out.println();
        System.out.println(request);
        System.out.println();
        System.out.println("======================================================");
        System.out.println();
    }

    private void showResponse(FullHttpResponse response){
        System.out.println();
        System.out.println("====================response==========================");
        System.out.println();
        System.out.println(response);
        System.out.println();
        System.out.println("======================================================");
        System.out.println();
    }

    private void showResponse(FullHttpResponse response,String content){
        System.out.println();
        System.out.println("====================response==========================");
        System.out.println();
        System.out.println(response);
        System.out.println(content);
        System.out.println();
        System.out.println("======================================================");
        System.out.println();
    }

}
