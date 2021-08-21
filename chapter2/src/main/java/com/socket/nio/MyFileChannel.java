package com.socket.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StreamUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * FileChannel 只支持阻塞模式
 *
 * @author carl
 */
public class MyFileChannel {
    static final Logger logger = LoggerFactory.getLogger(MyFileChannel.class);
    @SuppressWarnings("restriction")
    static final String LINE_SEPARATOR = java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));

    public static void main(String[] args) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel fcr = null;
        FileChannel fcw = null;
        Charset charset = StandardCharsets.UTF_8;
        try {
            File f = new File(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("f0.text")).getFile());
            fis = new FileInputStream(f);
            fos = new FileOutputStream(f, true);
            fcr = fis.getChannel();
            fcw = fos.getChannel();
            ByteBuffer buff = ByteBuffer.allocate(10240);
            while (fcr.read(buff) >= 0) {
                buff.flip();
                logger.info(String.valueOf(charset.decode(buff)));
                buff.clear();
            }
            // 换了个buff，上一个默认会回收
            buff = charset.encode(Util.getStdmfDateTime().concat(LINE_SEPARATOR));
            while (buff.hasRemaining()) {
                fcw.write(buff);
            }
        } catch (IOException e) {
            logger.error("Error on read file", e);
        } finally {
            StreamUtil.close(fcr);
            StreamUtil.close(fcw);
        }
    }
}
