package com.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StreamUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Paths;

/**
 * 字节流
 *
 * @author carl
 */
public class Client {
    static Logger logger = LoggerFactory.getLogger(Client.class);
    public static final String OK = "ok";

    public static void main(String[] args) {
        BufferedReader sr = null;
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        Socket socket = null;
        byte[] buff = new byte[10240];
        try {
            socket = new Socket("127.0.0.1", 9000);
            bos = new BufferedOutputStream(socket.getOutputStream());
            String filePath = Paths.get("/Users/carl/workspace/java-threads-socket/chapter2/src/main/java/com/socket/Client.java").toAbsolutePath().toString();
            bis = new BufferedInputStream(new FileInputStream(filePath));
            int t = bis.available();
            int r;
            while ((r = bis.read(buff)) > 0) {
                t -= r;
                logger.info("Read {} bytes from the file. {} bytes remain.", r, t);
                bos.write(buff, 0, r);
                bos.flush();
            }
            socket.shutdownOutput();
            sr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String s = sr.readLine();
            if (OK.equalsIgnoreCase(s)) {
                logger.info("Transport the file successfully");
            }
        } catch (IOException e) {
            logger.error("Error on connection", e);
        } finally {
            StreamUtil.close(sr);
            StreamUtil.close(bis);
            StreamUtil.close(bos);
            StreamUtil.close(socket);
        }
        logger.info("done");
    }
}
