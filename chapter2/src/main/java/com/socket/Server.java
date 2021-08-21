package com.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StreamUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author carl
 */
public class Server implements Runnable {
    static Logger logger = LoggerFactory.getLogger(Server.class);

    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private ServerSocket serverSocket = null;

    public void start() throws IOException {
        serverSocket = new ServerSocket(9000);
        threadPool.execute(this);
    }

    @Override
    public void run() {
        Socket socket;
        try {
            while ((socket = serverSocket.accept()) != null) {
                logger.info("Client {} connected", socket.getRemoteSocketAddress());
//                threadPool.execute(new Processor(socket));
                threadPool.execute(new MultiFileProcessor(socket));
            }
        } catch (IOException e) {
            logger.error("error on process connection", e);
        }
    }

    public void close() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("error on close server socket", e);
            }
        }
        threadPool.shutdownNow();
    }

    static final String EXIT = "exit";

    public static void main(String[] args) {
        Server server = new Server();
        BufferedReader br = null;
        try {
            server.start();
            logger.info("Enter 'exit' to exit");
            br = new BufferedReader(new InputStreamReader(System.in));
            String cmd;
            while ((cmd = br.readLine()) != null) {
                if (EXIT.equalsIgnoreCase(cmd)) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("Error on run server", e);
        } finally {
            StreamUtil.close(br);
            server.close();
        }
    }
}
