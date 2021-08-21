package com.socket.nio;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


/**
 * @author carl
 */
public abstract class Util {
    public static String getStdmfDateTime() {
        DateTimeFormatter df = DateTimeFormat.forStyle("MF");
        return df.print(DateTime.now());
    }

    public static void main(String[] args) {
        // Aug 21, 2021 2:01:43 PM CST
        System.out.println(Util.getStdmfDateTime());
    }
}
