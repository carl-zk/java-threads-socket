package com.socket.aio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StreamUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author carl
 */
public class Server implements CompletionHandler<AsynchronousSocketChannel, Object> {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private AsynchronousServerSocketChannel assc = null;

    public void start() throws IOException {
        assc = AsynchronousServerSocketChannel.open();
        assc.bind(new InetSocketAddress(9000));
        assc.accept(null, this);
    }

    @Override
    public void completed(AsynchronousSocketChannel asc, Object attachment) {
        assc.accept(null, this);
        ReadCH reader = new ReadCH(asc);
        asc.read(reader.getBuffer(), 0, reader);
    }

    @Override
    public void failed(Throwable exc, Object attachment) {
        this.close();
        logger.error("Error on accept connection", exc);
    }

    public void close() {
        StreamUtil.close(assc);
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
            logger.error("Error on run server", e);
        } finally {
            StreamUtil.close(br);
            server.close();
        }
        logger.info("bye");
    }
}
