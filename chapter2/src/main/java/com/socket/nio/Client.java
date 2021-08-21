package com.socket.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StreamUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author carl
 */
public class Client {
    static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        SocketChannel sc = null;
        FileChannel fc = null;
        try {
            sc = SocketChannel.open();
            sc.configureBlocking(true);
            sc.connect(new InetSocketAddress("127.0.0.1", 9000));
            if (!sc.finishConnect()) {
                logger.error("Can't connet to server");
                return;
            }
            ByteBuffer buff = ByteBuffer.allocate(10240);
            int r = 0;
            // 完美传输大文件 4g
            String f = "/Users/carl/Downloads/Spider-Man.Into the Spider-Verse.2019.1080p.WEB-DL.H264.AC3-EVO[EtHD]/Spider-Man.Into the Spider-Verse.2019.1080p.WEB-DL.H264.AC3-EVO[EtHD].mkv";
            fc = new FileInputStream(f).getChannel();
//            fc = new FileInputStream(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(f)).getFile()).getChannel();
            try {
                buff.putLong(fc.size());
                while ((r = fc.read(buff)) > 0) {
                    logger.info("Read {} bytes from file", r);
                    buff.flip();
                    while (buff.hasRemaining() && (r = sc.write(buff)) > 0) {
                        logger.info("Write {} bytes to server", r);
                    }
                    buff.clear();
                }
            } finally {
                StreamUtil.close(fc);
            }
            while ((r = sc.read(buff)) > 0) {
                logger.info("Read {} bytes from socket", r);
            }
            buff.flip();
            logger.info(StandardCharsets.UTF_8.decode(buff).toString());
        } catch (IOException e) {
            logger.error("Error on send file", e);
        } finally {
            StreamUtil.close(sc);
        }
        logger.info("done");
    }
}
