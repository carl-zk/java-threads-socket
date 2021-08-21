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

/**
 * @author carl
 */
public class MultiFileProcessor implements Runnable {
    static Logger logger = LoggerFactory.getLogger(MultiFileProcessor.class);

    private Socket socket;

    public MultiFileProcessor(Socket socket) {
        this.socket = socket;
    }

    public static int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
    }

    @Override
    public void run() {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        PrintWriter pw = null;
        byte[] buff = new byte[10240];
        int i = 0;
        byte[] h = new byte[4];
        try {
            bis = new BufferedInputStream(socket.getInputStream());
            pw = new PrintWriter(socket.getOutputStream(), true);
            while (!socket.isInputShutdown() && bis.read(h) == 4) {
                int t = byteArrayToInt(h), total = t, r = 0;
                if (t < 1) {
                    pw.println("error");
                    break;
                }
                logger.info("Incoming file size = {}", t);
                String fn = String.format("target/rec%d.txt", i);
                bos = new BufferedOutputStream(new FileOutputStream(fn));
                try {
                    while (t > 0 && (r = bis.read(buff)) > 0) {
                        t -= r;
                        logger.info("Received {} bytes, {} bytes remain", r, t);
                        bos.write(buff, 0, r);
                        bos.flush();
                    }
                    logger.info("Received {} bytes as file {}", total, fn);
                    pw.println(Client.OK);
                    i++;
                } finally {
                    StreamUtil.close(bos);
                }
            }
        } catch (IOException e) {
            logger.error("Error on transport", e);
        } finally {
            StreamUtil.close(bis);
            StreamUtil.close(pw);
            StreamUtil.close(socket);
        }
    }
}
