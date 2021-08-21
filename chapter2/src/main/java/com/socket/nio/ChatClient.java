package com.socket.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StreamUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author carl
 */
public class ChatClient implements Runnable {
    static Logger logger = LoggerFactory.getLogger(ChatClient.class);
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    private Selector selector;
    private SocketChannel socketChannel;
    private Thread thread = new Thread(this);
    private ByteBuffer buff = ByteBuffer.allocate(1024);
    private Queue<String> queue = new ConcurrentLinkedDeque<>();
    private volatile boolean live = true;

    public void start() throws IOException {
        this.selector = Selector.open();
        this.socketChannel = SocketChannel.open();
        logger.info("socketChannel {}", socketChannel);
        this.socketChannel.configureBlocking(false);
        this.socketChannel.connect(new InetSocketAddress("127.0.0.1", 9000));
        if (this.socketChannel.finishConnect()) {
            this.socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            this.thread.start();
        }
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
                    SocketChannel sc = null;
                    int r = 0;
                    String s = null;
                    if (key.isValid() && key.isReadable()) {
                        sc = (SocketChannel) key.channel();
                        logger.info("Readable socketChannel {}", sc);
                        StringBuilder sb = new StringBuilder();
                        buff.clear();
                        while ((r = sc.read(buff)) > 0) {
                            logger.info("Received {} bytes from {}", r, sc.getRemoteAddress());
                            buff.flip();
                            sb.append(CHARSET.decode(buff));
                            buff.clear();
                            s = sb.toString();
                            if (s.endsWith("\n")) {
                                break;
                            }
                        }
                        // 可能粘包，需要切分
                        String[] sa = s.split("\n");
                        for (String a : sa) {
                            if (Util.strIsNotEmpty(a)) {
                                logger.info(a);
                            }
                        }
                    }

                    if (key.isValid() && key.isWritable() && !queue.isEmpty()) {
                        s = queue.poll();
                        sc = (SocketChannel) key.channel();
                        logger.info("Writable socketChannel {}", sc);
                        ByteBuffer buf = ByteBuffer.wrap(s.getBytes(CHARSET));
                        buf.limit(s.length());
                        while (buf.hasRemaining() && (r = sc.write(buf)) > 0) {
                            logger.info("Write {} bytes to server", r);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error on socket I/O", e);
        } finally {
            StreamUtil.close(selector);
            StreamUtil.close(socketChannel);
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
        StreamUtil.close(socketChannel);
    }

    public boolean isAlive() {
        return this.thread.isAlive();
    }

    public void send(String s) {
        queue.add(s);
    }

    public static void main(String[] args) {
        BufferedReader ir = null;
        ChatClient client = new ChatClient();
        try {
            client.start();
            String cmd = null;
            ir = new BufferedReader(new InputStreamReader(System.in));
            logger.info("Enter 'exit' to exit");
            while ((cmd = ir.readLine()) != null && client.isAlive()) {
                if (Util.strIsNotEmpty(cmd)) {
                    client.send(cmd.concat("\n"));
                    if ("exit".equalsIgnoreCase(cmd)) {
                        client.close();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error on run client", e);
        } finally {
            StreamUtil.close(ir);
            client.close();
        }
        logger.info("done");
    }
}
