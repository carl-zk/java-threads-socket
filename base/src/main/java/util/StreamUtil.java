package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author carl
 */
public abstract class StreamUtil {
    private static Logger logger = LoggerFactory.getLogger(StreamUtil.class);

    public static <T extends Closeable> void close(T o) {
        if (o != null) {
            try {
                o.close();
            } catch (IOException e) {
                logger.error("close IO failed", e);
            }
        }
    }
}
