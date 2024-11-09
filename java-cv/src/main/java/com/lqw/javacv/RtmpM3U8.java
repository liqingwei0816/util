package com.lqw.javacv;

import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.io.File;

/**
 * m3u8推流
 */
public class RtmpM3U8 {
    private static final String outputUrl = "D:\\nginx_rtmp\\html\\test.m3u8";
    private static final String inputUrl = "D:\\神武天尊动态漫画第242集.mp4";

    public static void main(String[] args) throws FrameGrabber.Exception, FrameRecorder.Exception {
        // 设置 FFmpeg 日志级别
        avutil.av_log_set_level(avutil.AV_LOG_WARNING);
        FFmpegLogCallback.set();

        // 检查输入文件是否存在
        File inputFile = new File(inputUrl);
        if (!inputFile.exists()) {
            throw new RuntimeException("Input file does not exist: " + inputUrl);
        }

        // 以文件路径的方式传入视频
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputUrl);
        // 开始捕获视频流
        grabber.start();

        AVFormatContext avFormatContext = grabber.getFormatContext();
        // 获取视频时长
        // long duration = avFormatContext.duration();
        // 检查文件是否媒体流(视频流、音频流)
        if (avFormatContext.nb_streams() == 0) {
            // 表明没有媒体流
            return;
        }

        // 打印输入像素格式
        int pixelFormat = grabber.getPixelFormat();
        System.out.println("Input pixel format: " + pixelFormat);
        System.out.println("AV_PIX_FMT_BGR24: " + avutil.AV_PIX_FMT_BGR24);

        // 确保输入像素格式是 BGR24
        if (pixelFormat != avutil.AV_PIX_FMT_BGR24) {
            throw new RuntimeException("Input pixel format is not BGR24");
        }

        // 用于将捕获到的视频流转换为输出 URL 的 mp4 格式。
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputUrl, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
        recorder.setFormat("hls");
        recorder.setVideoBitrate(10000000); // 设置视频比特率为 1 Mbps
        recorder.setFrameRate(grabber.getVideoFrameRate()); // 设置帧率
        recorder.setGopSize(30); // 设置关键帧间隔

        // 设置 HLS 切片参数
        recorder.setOption("hls_time", "15");
        recorder.setOption("hls_list_size", "20");
        recorder.setOption("hls_wrap", "20");
        recorder.setOption("hls_flags", "delete_segments");
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); // 输出格式设置为 YUV420P

        // 设置音频编码为 AAC
        if (grabber.getAudioChannels() > 0) {
            recorder.setAudioChannels(grabber.getAudioChannels());
            recorder.setAudioBitrate(grabber.getAudioBitrate());
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        }

        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);

        // 将解码后的帧记录到输出文件中
        recorder.start();
        Frame frame;
        while ((frame = grabber.grab()) != null) {
            if (frame.image != null) {
                //System.out.println("Recording video frame");
                recorder.record(frame);
            } else if (frame.samples != null) {
                //System.out.println("Recording audio frame");
                recorder.record(frame);
            } else {
                System.out.println("Skipping empty frame");
            }
        }

        recorder.close();
        grabber.close();
    }
}