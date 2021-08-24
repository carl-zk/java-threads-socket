package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author carl
 */
public interface ObjectSerializer {
    /**
     * serialize object to byte array
     *
     * @param obj
     * @return
     * @throws IOException
     */
    byte[] serialize(Object obj) throws IOException;

    void serialize(Object obj, OutputStream os) throws IOException;

    <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException, ClassNotFoundException;

    <T> T deserialize(InputStream is, Class<T> clazz) throws IOException, ClassNotFoundException;

    Object deserialize(InputStream is) throws IOException, ClassNotFoundException;

    Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException;

    <T> T deserialize(byte[] b, T co) throws IOException, ClassNotFoundException;
}
