package com.tuya.rtsp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import io.netty.handler.codec.rtsp.RtspEncoder;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.mp4.impl.MP4Reader;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.VideoData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by heshaoqiong on 2017/7/20.
 */
public class RtpPacketization implements Runnable {
    private static Logger log = LoggerFactory.getLogger(RtpPacketization.class);

    ChannelHandlerContext ctx;
    String filePath;

    private Integer ssrc1 = 100231;
    private Integer ssrc2 = 2100231;
    public RtpPacketization(ChannelHandlerContext ctx, String filePath) {
        // TODO Auto-generated constructor stub
        this.ctx = ctx;
        this.filePath = filePath;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        System.out.println("SendRtpPacket");
        try {
            MP4Reader mp4Reader = new MP4Reader(new File(filePath));
            ITag tag = null;
            short sequenceNumber =0;
            Short sequenceNumber1 = 10000;
            Short sequenceNumber2 = 20000;

            while (mp4Reader.hasMoreTags()) {
                tag = mp4Reader.readTag();
             //   System.out.println("Tag\n{}",tag);
                IoBuffer body = tag.getBody();
                byte dataType = tag.getDataType();
                if(dataType== MP4Reader.TYPE_AUDIO) {                // 8

                    continue;
                }

                short rtpLength = (short)(body.limit()+12+1-7); //12为rtp头的固定长度
                ByteBuf buffer = Unpooled.buffer();

                byte magicNumber = (byte)0x24;			// 1byte magic number 0x24
                byte channelNumber = (byte)0x00;		// 1byte channel number 0x00(RTP)
                buffer.writeByte(magicNumber);
                //System.out.println(ByteBufUtil.hexDump(buffer));

                buffer.writeByte(channelNumber);
                //System.out.println(ByteBufUtil.hexDump(buffer));

                buffer.writeShort(rtpLength);
                //System.out.println(ByteBufUtil.hexDump(buffer));


                /***
                 * Version/Padding/Extension/CSRC Count
                 */
                byte VnPnXnCC	= (byte)0x80;	// V=2 P=0 X=0 CC=0
                buffer.writeByte(VnPnXnCC);
                //System.out.println(ByteBufUtil.hexDump(buffer));


                /***
                 * Marker/PayloadType
                 */
                int ssrc = 0;
                byte MnPT= (byte)0x80;
                dataType = tag.getDataType();
                if(dataType== MP4Reader.TYPE_AUDIO) {                // 8
                    MnPT = (byte) (MnPT | 0x61);                    // 97
                    ssrc=ssrc1;
                    /***
                     * sequenceNumber/timestamp/SSRC
                     */
                    sequenceNumber=sequenceNumber1;
                    sequenceNumber1++;
                    AudioData audioData = new AudioData(tag.getBody());
                    audioData.getData();

                    continue;
                }
                else if(dataType==MP4Reader.TYPE_VIDEO)			// 9
                {
                    MnPT = (byte) (MnPT | 0x60);                    // 96
                    ssrc=ssrc2;
                    /***
                     * sequenceNumber/timestamp/SSRC
                     */
                    VideoData videoData = new VideoData(body);
                   // if(videoData.getFrameType() != VideoData.FrameType.INTERFRAME &&videoData.getFrameType()!=VideoData.FrameType.KEYFRAME) {
                        System.out.println("dataType:   " + videoData.getFrameType());
                    byte b = videoData.getData().buf().get(6);
                    if(b!=0){
                        continue;
                    }
                    sequenceNumber=sequenceNumber2;
                    sequenceNumber2++;
                }
                else if(dataType==MP4Reader.TYPE_METADATA)		// 18
                    //MnPT = (byte)0x12;						// 18
                    continue;
                else{
                    System.out.println("Exception in buffer");
                }
                buffer.writeByte(MnPT);
               // System.out.println(ByteBufUtil.hexDump(buffer));


                buffer.writeShort(sequenceNumber);
                //System.out.println(ByteBufUtil.hexDump(buffer));

                int timestamp = tag.getTimestamp()*100;
                buffer.writeInt(timestamp);			// timestamp int
                //System.out.println(ByteBufUtil.hexDump(buffer));


                buffer.writeInt(ssrc);
                //System.out.println(ByteBufUtil.hexDump(buffer));
                VideoData videoData = new VideoData(body);
                if(videoData.getFrameType() == VideoData.FrameType.KEYFRAME){
                    byte fu = 0x38;
                    buffer.writeByte(fu);
                }else {
                    byte fu = 0x18;
                    buffer.writeByte(fu);
                }

                ByteBuffer byteBuffer = body.buf();

                buffer.writeBytes(byteBuffer.array(),7,byteBuffer.limit()-7);
                System.out.println(ByteBufUtil.hexDump(buffer));


                ctx.writeAndFlush(buffer);
                Thread.sleep(33);


            }
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
    public static void main(String[] args) throws IOException {
        String path = "/Users/tuya/Downloads/sample_h264_300kbit.mp4";
        MP4Reader mp4Reader = new MP4Reader(new File(path));
        System.out.println("mp4Reader.getBytesRead():  "+mp4Reader.getBytesRead());
        System.out.println("mp4Reader.getDuration():  "+mp4Reader.getDuration());
        System.out.println("mp4Reader.getOffset():  "+mp4Reader.getOffset());
        System.out.println("mp4Reader.getTotalBytes():  "+mp4Reader.getTotalBytes());

        while (mp4Reader.hasMoreTags()){
            System.out.println("mp4Reader.hasVideo():  "+mp4Reader.readTag());
        }


        System.exit(0);
    }
    */
}
