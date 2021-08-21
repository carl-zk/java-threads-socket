package com.socket.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StreamUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * @author carl
 */
public class Server implements Runnable {
    static Logger logger = LoggerFactory.getLogger(Server.class);

    private Selector selector = null;
    private ServerSocketChannel ssc = null;
    private Thread thread = new Thread(this);
    private volatile boolean live = true;

    class TempBuff {
        private long required = -1L;
        private long received = 0L;
        private ByteBuffer buffer = ByteBuffer.allocate(10240);
        private FileChannel channel;

        @SuppressWarnings("resource")
        public TempBuff(int port) throws FileNotFoundException {
            this.channel = new FileOutputStream(String.format("target/%d.mkv", port)).getChannel();
        }
    }

    public void start() throws IOException {
        selector = Selector.open();
        ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress(9000));
        ssc.configureBlocking(false);
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        thread.start();
    }

    @Override
    public void run() {
        try {
            while (live && !Thread.interrupted()) {
                if (selector.select(1000) == 0) {
                    continue;
                }
                Set<SelectionKey> set = selector.selectedKeys();
                Iterator<SelectionKey> it = set.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isValid() && key.isAcceptable()) {
                        this.onAcceptable(key);
                    }

                    if (key.isValid() && key.isReadable()) {
                        this.onReadable(key);
                    }

                    if (key.isValid() && key.isWritable()) {
                        this.onWritable(key);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error on socket I/O", e);
        }
    }

    public void onAcceptable(SelectionKey key) {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel sc = null;
        try {
            sc = ssc.accept();
            if (sc != null) {
                InetSocketAddress isa = (InetSocketAddress) sc.getRemoteAddress();
                logger.info("Client {} connected", isa);
                sc.configureBlocking(false);
                sc.register(key.selector(), SelectionKey.OP_READ, new TempBuff(isa.getPort()));
            }
        } catch (IOException e) {
            logger.error("Error on accept connection", e);
            StreamUtil.close(sc);
        }
    }

    public void onReadable(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        TempBuff tb = (TempBuff) key.attachment();
        if (tb.required < 0) {
            ByteBuffer hb = ByteBuffer.allocate(8);
            while (hb.hasRemaining() && sc.read(hb) > 0) {
                logger.info("for sure read all");
            }
            hb.flip();
            tb.required = hb.getLong();
        }
        ByteBuffer buff = tb.buffer;
        try {
            InetSocketAddress isa = (InetSocketAddress) sc.getRemoteAddress();
            int r = 0;
            buff.clear();
            while ((r = sc.read(buff)) > 0) {
                tb.received += r;
                logger.info("Received {}/{}/{} bytes from {}", r, tb.received, tb.required, isa);
                buff.flip();
                r = tb.channel.write(buff);
                logger.info("Write {}/{}/{} bytes to file", r, tb.received, tb.required, isa);
                buff.clear();
            }
            if (tb.required > tb.received) {
                sc.register(key.selector(), SelectionKey.OP_READ, tb);
            } else {
                sc.register(key.selector(), SelectionKey.OP_WRITE, tb);
            }
        } catch (Exception e) {
            logger.error("Error on read socket", e);
            StreamUtil.close(tb.channel);
            StreamUtil.close(sc);
        }
    }

    public void onWritable(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        TempBuff tb = (TempBuff) key.attachment();
        try {
            byte[] ba = "ok".getBytes(StandardCharsets.UTF_8);
            ByteBuffer buff = ByteBuffer.wrap(ba);
            buff.limit(ba.length);
            int r = 0;
            while (buff.hasRemaining() && (r = sc.write(buff)) > 0) {
                logger.info("Write {} bytes to {}", r, sc.getRemoteAddress());
            }
        } catch (IOException e) {
            logger.error("Error on write socket", e);
        } finally {
            StreamUtil.close(tb.channel);
            StreamUtil.close(sc);
        }
    }

    public void close() {
        live = false;
        try {
            this.thread.join();
        } catch (InterruptedException e) {
            logger.error("Be interrupted on join", e);
        }
        StreamUtil.close(selector);
        StreamUtil.close(ssc);
    }

    public static void main(String[] args) {
        BufferedReader br = null;
        Server server = new Server();
        try {
            server.start();
            String cmd = null;
            logger.info("Enter 'exit' to exit");
            br = new BufferedReader(new InputStreamReader(System.in));
            while ((cmd = br.readLine()) != null) {
                if ("exit".equalsIgnoreCase(cmd)) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("Error on start server", e);
        } finally {
            StreamUtil.close(br);
            server.close();
        }
        logger.info("bye");
    }
}
