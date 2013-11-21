package edu.nclab.swimudpserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * User: Changhoon
 * Date: 13. 11. 19
 * Time: 오후 1:14
 */
public class GameMessage {
    public long timestamp;
    public int signal;
    public int status;

    public static GameMessage readFromBytes(byte[] bytes) {
        GameMessage msg = new GameMessage();
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            msg.timestamp = ois.readLong();
            msg.signal = ois.readInt();
            msg.status = ois.readInt();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msg;
    }

    public byte[] toByteArray() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeLong(timestamp);
            oos.writeInt(signal);
            oos.writeInt(status);
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
