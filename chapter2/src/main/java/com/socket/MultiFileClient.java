package com.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StreamUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Objects;

/**
 * 传输多笔数据
 *
 * @author carl
 */
public class MultiFileClient {
    static Logger logger = LoggerFactory.getLogger(MultiFileClient.class);

    public static byte[] intToByteArray(int a) {
        return new byte[]{(byte) ((a >> 24) & 0xFF), (byte) ((a >> 16) & 0xFF), (byte) ((a >> 8) & 0xFF), (byte) (a & 0xFF)};
    }

    public static void main(String[] args) {
        BufferedReader sr = null;
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        Socket socket = null;
        byte[] buff = new byte[10240];
        try {
            socket = new Socket("127.0.0.1", 9000);
            sr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bos = new BufferedOutputStream(socket.getOutputStream());
            for (int i = 0; i < 1; i++) {
                String fn = String.format("f%d.text", i);
                bis = new BufferedInputStream(new FileInputStream("/Users/carl/Downloads/Spider-Man.Into the Spider-Verse.2019.1080p.WEB-DL.H264.AC3-EVO[EtHD]/Spider-Man.Into the Spider-Verse.2019.1080p.WEB-DL.H264.AC3-EVO[EtHD].mkv"));
//                bis = new BufferedInputStream(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(fn)));
                try {
                    // 文件大小超过 Integer.MAX_VALUE 就会报错
                    int t = bis.available(), r = 0;
                    logger.info("Send file {}, size = {}", fn, t);
                    bos.write(intToByteArray(t));
                    while ((r = bis.read(buff)) > 0) {
                        t -= r;
                        logger.info("Read {} bytes from file, {} bytes remain", r, t);
                        bos.write(buff, 0, r);
                        bos.flush();
                    }
                    String s = sr.readLine();
                    if (Client.OK.equalsIgnoreCase(s)) {
                        logger.info("Transport file {} successfully", fn);
                    } else {
                        logger.error("Transport file {} failed", fn);
                    }
                } finally {
                    StreamUtil.close(bis);
                }
            }
            socket.shutdownOutput();
        } catch (IOException e) {
            logger.error("Error on connection", e);
        } finally {
            StreamUtil.close(sr);
            StreamUtil.close(bos);
            StreamUtil.close(socket);
        }
        logger.info("done");
    }
}
