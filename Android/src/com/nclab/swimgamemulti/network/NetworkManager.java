package com.nclab.swimgamemulti.network;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.nclab.swimgamemulti.utils.Consts;
import com.nclab.swimgamemulti.utils.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * User: Changhoon
 * Date: 13. 11. 13
 */
public class NetworkManager {
    private boolean isReceiving = false;
    private boolean isSending = false;

    private int lastSeq = 1;
    private HashMap<Integer, UDPPacket> packetMap = new HashMap<Integer, UDPPacket>();
    private ConcurrentLinkedQueue<UDPPacket> receivedPacketQueue = new ConcurrentLinkedQueue<UDPPacket>();
    private ConcurrentLinkedQueue<UDPPacket> packetQueue = new ConcurrentLinkedQueue<UDPPacket>();
    
    private Handler receiveHandler;

    public NetworkManager(Handler receiveHandler) {
    	this.receiveHandler = receiveHandler;
    }
    
    private Thread udpReceiverWorker = new Thread() {
        @Override
        public void run() {
            Logger.d("Receiver running...");
            try {
                DatagramSocket socket = openSocket(Consts.CLIENT_PORT);
                while (isReceiving) {
                    byte buffer[] = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.setSoTimeout(Consts.RECV_TIMEOUT);
                    socket.receive(packet);

                    ByteArrayInputStream byteIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    DataInputStream dataIn = new DataInputStream(byteIn);

                    int serverId = dataIn.readInt();
                    int seq = dataIn.readInt();
                    int payloadSize = dataIn.readInt();
                    byte pBuffer[] = new byte[payloadSize];
                    int readSize = dataIn.read(pBuffer);
                    if (packetMap.containsKey(seq)) {
//                        Logger.d("Packet " + seq + " already received");
                    } else {
                        UDPPacket udpPacket = new UDPPacket(seq, pBuffer);
                        packetMap.put(seq, udpPacket);
//                        receivedPacketQueue.add(udpPacket);
                        Bundle bundle = new Bundle();
                        bundle.putByteArray("payload", udpPacket.getPayload());
                        Message message = receiveHandler.obtainMessage(CommunicationManager.MSG_INGOING);
                        message.setData(bundle);
                        message.sendToTarget();
                        Logger.d("Received " + seq + " from " + serverId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    
    // TODO: Flush expired packets

    private Thread udpSenderWorker = new Thread() {
        @Override
        public void run() {
            Logger.d("Sender running...");
            int clientId = 1;
            try {
                DatagramSocket socket = openSocket(Consts.SRC_PORT);
                InetAddress addr = InetAddress.getByName(Consts.SERVER_HOSTNAME);

                while (isSending) {
                    long startTime = System.currentTimeMillis();
                    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                    DataOutputStream dataOut = new DataOutputStream(byteOut);

                    UDPPacket udpPacket = packetQueue.poll();
                    if (udpPacket != null) {
                        if (startTime - udpPacket.getTimestamp() > 2000) {
                            Logger.d("Packet " + udpPacket.toString() + " removed");
                            continue;
                        }

                        dataOut.writeInt(clientId);
                        dataOut.writeInt(udpPacket.getSeq());
                        dataOut.writeInt(udpPacket.getPayload().length);
                        Logger.d("Payload size: " + udpPacket.getPayload().length);
                        dataOut.write(udpPacket.getPayload());
                        Logger.d("Sending " + Logger.byteArrayToHex(udpPacket.getPayload()));

                        byte[] data = byteOut.toByteArray();
                        DatagramPacket packet = new DatagramPacket(data, data.length, addr, Consts.SERVER_PORT);
                        socket.send(packet);

                        packetQueue.add(udpPacket);
                    }

                    long remaining = System.currentTimeMillis() - startTime;
                    if (remaining <= 10) {
                        Thread.sleep(remaining);
                    }
                }
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Opens a datagram (UDP) socket
     *
     * @return: a datagram socket used for sending/receiving
     * @throws: MeasurementError
     *             if an error occurs
     */
    private DatagramSocket openSocket(int port) throws Exception {
        DatagramSocket sock = null;

        // Open datagram socket
        try {
            sock = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new Exception("Socket creation failed");
        }

        return sock;
    }


    /**
     *  UDP networking
     */
    public void toggleSending() {
        if (!udpSenderWorker.isAlive()) {
            isSending = true;
            udpSenderWorker.start();
        } else {
            isSending = false;
        }
    }

    public void toggleReceiving() {
        if (!udpReceiverWorker.isAlive()) {
            isReceiving = true;
            udpReceiverWorker.start();
        } else {
            isReceiving = false;
        }
    }

    public void startSending() {
        if (!udpSenderWorker.isAlive()) {
            isSending = true;
            udpSenderWorker.start();
        }
    }

    public void stopSending() {
        isSending = false;
    }

    public void startListening() {
        if (!udpReceiverWorker.isAlive()) {
            isReceiving = true;
            udpReceiverWorker.start();
        }
    }

    public void stopListening() {
        isReceiving = false;
    }

    public void sendPacket(byte[] payload) {
        lastSeq = lastSeq + 1;
        UDPPacket udpPacket = new UDPPacket(lastSeq, payload);
        packetQueue.add(udpPacket);
        Logger.d("Packet " + udpPacket.toString() + " added");
    }

}
