package util;

import java.util.Objects;

/**
 * @author carl
 */
public abstract class FileUtil {

    public static String getFile(String fileName) {
        return Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(fileName)).getFile();
    }
}
