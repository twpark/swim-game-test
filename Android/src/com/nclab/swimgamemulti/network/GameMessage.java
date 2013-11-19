package com.nclab.swimgamemulti.network;

import java.io.*;

/**
 * User: Changhoon
 * Date: 13. 11. 19
 * Time: 오후 1:14
 */
/**
 * User: Changhoon
 * Date: 13. 11. 19
 * Time: 오후 1:14
 */
public class GameMessage {
    public int id;
    public long timestamp;
    public int signal;
    public int status;

    public GameMessage() {}

    public GameMessage(int id, long timestamp, int signal, int status) {
        this.id = id;
        this.timestamp = timestamp;
        this.signal = signal;
        this.status = status;
    }

    public static GameMessage readFromBytes(byte[] bytes) {
        GameMessage msg = new GameMessage();
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
            msg.id = dis.readInt();
            msg.timestamp = dis.readLong();
            msg.signal = dis.readInt();
            msg.status = dis.readInt();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msg;
    }

    public byte[] toByteArray() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(id);
            dos.writeLong(timestamp);
            dos.writeInt(signal);
            dos.writeInt(status);
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

