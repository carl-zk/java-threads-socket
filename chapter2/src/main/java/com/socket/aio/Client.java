package com.socket.aio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.FileUtil;
import util.StreamUtil;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author carl
 */
public class Client implements CompletionHandler<Integer, Integer> {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private AsynchronousSocketChannel asc;
    private ByteBuffer buff = ByteBuffer.allocate(10240);
    private FileInputStream fis;
    private FileChannel fc;
    private int maxFileNo = 2;

    public void start() throws IOException, ExecutionException, InterruptedException {
        asc = AsynchronousSocketChannel.open();
        Future<Void> ft = asc.connect(new InetSocketAddress("127.0.0.1", 9000));
        ft.get();
        this.send(0);
    }

    public void send(Integer i) throws IOException {
        StreamUtil.close(fc);
        StreamUtil.close(fis);
        String fn = FileUtil.getFile(String.format("f%d.text", i));
        fis = new FileInputStream(fn);
        fc = fis.getChannel();
        buff.clear();
        buff.putLong(fc.size() + 8);
        fc.read(buff);
        buff.flip();
        logger.info("Write first buffer of file {}", fn);
        asc.write(buff, i, this);
    }

    @Override
    public void completed(Integer r, Integer attachment) {
        if (r <= 0) {
            logger.info("No written data now. Quit");
            return;
        }
        logger.info("Written {} bytes", r);
        try {
            if (buff.hasRemaining()) {
                asc.write(buff, attachment, this);
                return;
            }
            buff.clear();
            if (fc.read(buff) > 0) {
                buff.flip();
                asc.write(buff, attachment, this);
            } else {
                r = asc.read(buff).get();
                logger.info("Read response {} bytes", r);
                buff.flip();
                long total = buff.getLong();
                long received = buff.getLong();
                logger.info("{}/{}", total, received);
                if (attachment < maxFileNo) {
                    this.send(attachment + 1);
                } else {
                    asc.shutdownOutput();
                    synchronized (this) {
                        this.notify();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error on send file", e);
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
    }

    public void close() {
        StreamUtil.close(asc);
        StreamUtil.close(fis);
        StreamUtil.close(fc);
    }

    public static void main(String[] args) {
        BufferedReader br = null;
        Client client = new Client();
        try {
            client.start();
            synchronized (client) {
                client.wait();
            }
        } catch (Exception e) {
            logger.error("Error on run client", e);
        } finally {
            StreamUtil.close(br);
            client.close();
        }
        logger.info("bye");
    }
}