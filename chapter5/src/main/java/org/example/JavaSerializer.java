package org.example;

import util.StreamUtil;

import java.io.*;

/**
 * @author carl
 */
public class JavaSerializer implements ObjectSerializer {
    @Override
    public byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        try {
            oos.writeObject(obj);
            oos.flush();
            return bos.toByteArray();
        } finally {
            StreamUtil.close(oos);
        }
    }

    /**
     * 不需要关闭 OutputStream
     *
     * @param obj
     * @param os
     * @throws IOException
     */
    @Override
    public void serialize(Object obj, OutputStream os) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(obj);
        oos.flush();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException, ClassNotFoundException {
        return deserialize(new ByteArrayInputStream(bytes), clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(InputStream is, Class<T> clazz) throws IOException, ClassNotFoundException {
        return (T) deserialize(is);
    }

    @Override
    public Object deserialize(InputStream is) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = is instanceof ObjectInputStream ? (ObjectInputStream) is : new ObjectInputStream(is);
        try {
            return ois.readObject();
        } finally {
            StreamUtil.close(ois);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        return deserialize(new ByteArrayInputStream(bytes));
    }

    @Override
    public <T> T deserialize(byte[] b, T co) throws IOException, ClassNotFoundException {
        return (T) deserialize(b, co.getClass());
    }
}
