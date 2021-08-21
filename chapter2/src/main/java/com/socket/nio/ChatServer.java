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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author carl
 */
public class ChatServer implements Runnable {
    static Logger logger = LoggerFactory.getLogger(ChatServer.class);

    private Selector selector = null;
    private ServerSocketChannel ssc = null;
    private Thread thread = new Thread(this);
    private Queue<String> queue = new ConcurrentLinkedDeque<>();
    private volatile boolean live = true;

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
                ByteBuffer outBuff = null;
                String outMsg = queue.poll();
                if (Util.strIsNotEmpty(outMsg)) {
                    outBuff = ByteBuffer.wrap(outMsg.getBytes(ChatClient.CHARSET));
                    outBuff.limit(outMsg.length());
                }
                Set<SelectionKey> set = selector.selectedKeys();
                for (Iterator<SelectionKey> it = set.iterator(); it.hasNext(); ) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isValid() && key.isAcceptable()) {
                        this.onAcceptable(key);
                    }
                    if (key.isValid() && key.isReadable()) {
                        this.onReadable(key);
                    }
                    if (key.isValid() && key.isWritable() && outBuff != null) {
                        SocketChannel sc = (SocketChannel) key.channel();
                        this.write(sc, outBuff);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error on socket I/O", e);
        }
    }

    private void onAcceptable(SelectionKey key) {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel sc = null;
        try {
            sc = ssc.accept();
            if (sc != null) {
                logger.info("Client {} connected", sc.getRemoteAddress());
                logger.info("Acceptable socketChannel {}", sc);
                sc.configureBlocking(false);
                sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, ByteBuffer.allocate(1024));
            }
        } catch (IOException e) {
            logger.error("Error on accept connection", e);
            StreamUtil.close(sc);
        }
    }

    private void onReadable(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        logger.info("Readable socketChannel {}", sc);
        ByteBuffer buff = (ByteBuffer) key.attachment();
        int r = 0;
        StringBuilder sb = new StringBuilder();
        String rs = null;
        String remote = null;
        buff.clear();
        try {
            remote = sc.getRemoteAddress().toString();
            while ((r = sc.read(buff)) > 0) {
                logger.info("Received {} bytes from {}", r, remote);
                buff.flip();
                sb.append(ChatClient.CHARSET.decode(buff));
                buff.clear();
                rs = sb.toString();
                // 不会读到半条
                if (rs.endsWith("\n")) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("Error on read socket", e);
            StreamUtil.close(sc);
            return;
        }
        if (Util.strIsNotEmpty(rs)) {
            String[] sa = rs.split("\n");
            for (String s : sa) {
                if (Util.strIsNotEmpty(s)) {
                    logger.info("{}: {}", remote, s);
                    queue.add(String.format("%s: %s\n", remote, s));
                    if ("exit".equalsIgnoreCase(s)) {
                        StreamUtil.close(sc);
                    }
                }
            }
        }
    }

    private void write(SocketChannel sc, ByteBuffer buff) {
        buff.position(0);
        int r = 0;
        try {
            while (buff.hasRemaining() && (r = sc.write(buff)) > 0) {
                logger.info("Write back {} bytes to {}", r, sc.getRemoteAddress());
            }
        } catch (IOException e) {
            logger.error("Error on write socket", e);
            StreamUtil.close(sc);
        }
    }

    public void close() {
        live = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            logger.error("Be interrupted on join", e);
        }
        StreamUtil.close(selector);
        StreamUtil.close(ssc);
    }

    public static void main(String[] args) {
        BufferedReader br = null;
        ChatServer server = new ChatServer();
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
            logger.error("Error on run server", e);
        } finally {
            StreamUtil.close(br);
            server.close();
        }
        logger.info("bye");
    }
}
