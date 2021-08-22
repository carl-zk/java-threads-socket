package com.socket.aio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StreamUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;

/**
 * @author carl
 */
public class ReadCH implements CompletionHandler<Integer, Integer> {
    private static final Logger logger = LoggerFactory.getLogger(ReadCH.class);

    private ByteBuffer buffer = ByteBuffer.allocate(10240);
    private AsynchronousSocketChannel asc;
    private FileOutputStream fos;
    private FileChannel fc;
    private long total;
    private long received;

    public ReadCH(AsynchronousSocketChannel asc) {
        this.asc = asc;
    }

    @Override
    public void completed(Integer r, Integer attachment) {
        if (r <= 0) {
            logger.info("No more incoming data now. Quit");
            return;
        }
        received += r;
        logger.info("Read {}/{}/{} bytes", r, received, total);
        try {
            buffer.flip();
            if (fc == null) {
                total = buffer.getLong();
                InetSocketAddress isa = (InetSocketAddress) asc.getRemoteAddress();
                fos = new FileOutputStream(String.format("target/%d_%d.txt", isa.getPort(), attachment));
                fc = fos.getChannel();
            }
            fc.write(buffer);
            buffer.clear();
            if (received < total) {
                asc.read(buffer, attachment, this);
            } else {
                buffer.putLong(total);
                buffer.putLong(received);
                buffer.flip();
                r = asc.write(buffer).get();
                logger.info("Written response {} bytes", r);
                this.reset();
                this.asc.read(buffer, attachment + 1, this);
            }
        } catch (Exception e) {
            logger.error("Error on receive file", e);
            this.close();
        }
    }

    @Override
    public void failed(Throwable exc, Integer attachment) {
        this.close();
        SocketAddress sa = null;
        try {
            sa = asc.getRemoteAddress();
        } catch (IOException e) {
            logger.error("Error on getRemoteAddress", e);
        }
        logger.error("Error on read from {}", sa, exc);
        this.close();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void reset() {
        StreamUtil.close(fos);
        fos = null;
        StreamUtil.close(fc);
        fc = null;
        buffer.clear();
        total = 0;
        received = 0;
    }

    public void close() {
        this.reset();
        StreamUtil.close(asc);
    }
}
