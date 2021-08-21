package com.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StreamUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Paths;

/**
 * @author carl
 */
public class Processor implements Runnable {
    static Logger logger = LoggerFactory.getLogger(Processor.class);

    private final Socket socket;

    public Processor(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        PrintWriter pw = null;
        byte[] buff = new byte[10240];
        try {
            bis = new BufferedInputStream(socket.getInputStream());
            pw = new PrintWriter(socket.getOutputStream(), true);
            bos = new BufferedOutputStream(new FileOutputStream(Paths.get("target/rev.java").toString()));
            int r;
            int t = 0;
            while (!socket.isInputShutdown() && (r = bis.read(buff)) > 0) {
                t += r;
                logger.info("Received {}/{} bytes", r, t);
                bos.write(buff, 0, r);
                bos.flush();
            }
            logger.info("Received {} bytes and done", t);
            pw.println(Client.OK);
        } catch (IOException e) {
            logger.error("Error on transport", e);
        } finally {
            StreamUtil.close(bos);
            StreamUtil.close(bis);
            StreamUtil.close(pw);
            StreamUtil.close(socket);
        }
    }
}
