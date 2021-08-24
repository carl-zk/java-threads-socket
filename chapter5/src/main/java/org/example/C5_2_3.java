package org.example;

import util.StreamUtil;

import java.io.*;

/**
 * @author carl
 */
public class C5_2_3 implements Serializable {
    private static final long serialVersionUID = 6363396329131509163L;

    private String name;
    private int age;
    private boolean male;

    public void print() {
        System.out.println(name);
        System.out.println(age);
        System.out.println(male);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        System.out.println("My writeObject()");
        oos.writeObject(name);
        oos.writeInt(age);
        oos.writeBoolean(male);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        System.out.println("My readObject()");
        this.name = (String) ois.readObject();
        this.age = ois.readInt();
        this.male = ois.readBoolean();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        File file = new File("/Users/carl/workspace/java-threads-socket/target/C5_2_3.bin");
        if (file.exists()) {
            System.out.println("Read from file");
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new FileInputStream(file));
                C5_2_3 a = (C5_2_3) ois.readObject();
                a.print();
            } finally {
                StreamUtil.close(ois);
            }
        }
        C5_2_3 o1 = new C5_2_3();
        o1.name = "Mac_j";
        o1.age = 38;
        o1.male = true;

        System.out.println("Write to file");
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(o1);
        } finally {
            StreamUtil.close(oos);
        }
        System.out.println("done");
    }
}
