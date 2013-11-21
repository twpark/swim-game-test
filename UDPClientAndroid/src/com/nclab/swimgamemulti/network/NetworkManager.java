package com.nclab.swimgamemulti.network;

import android.os.Handler;
import com.nclab.swimgamemulti.GameActivity;
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

    DatagramSocket globalSenderSocket = null;

    public boolean sendNTP = false;
    public boolean sendOne = false;

    private long lastSentLocalTime = 0;
    private long timeDifference = 0;

    public static final int PACKET_TYPE_NORMAL = 0;
    public static final int PACKET_TYPE_FORWARD = 1;

    public static final int PACKET_TYPE_REGSYNC_1 = 11;
    public static final int PACKET_TYPE_REGSYNC_2 = 12;
    public static final int PACKET_TYPE_REGSYNC_3 = 13;

    public static final int PACKET_REFRESH_LOG = 21;
    public static final int PACKET_BURST_START = 22;

    private int lastSeq = 1;
    private FileLogger fileLogger;

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    private String serverAddress;

    public Handler getUiHandler() {
        return uiHandler;
    }

    public void setUiHandler(Handler uiHandler) {
        this.uiHandler = uiHandler;
    }

    private Handler uiHandler;

    public int getClientID() {
        return clientID;
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    private int clientID = 0;

    private HashMap<Integer, UDPPacket> packetMap = new HashMap<Integer, UDPPacket>();
    private ConcurrentLinkedQueue<UDPPacket> receivedPacketQueue = new ConcurrentLinkedQueue<UDPPacket>();
    private ConcurrentLinkedQueue<UDPPacket> packetQueue = new ConcurrentLinkedQueue<UDPPacket>();
    
    private Handler receiveHandler;

    public NetworkManager(Handler receiveHandler) {
    	this.receiveHandler = receiveHandler;
        fileLogger = new FileLogger("UDP");
        setServerAddress(Consts.SERVER_HOSTNAME);
    }

    private int burstDuration = 20;
    private int packetsPerSecond = 1;
    private int countDown = 5;

    private boolean burstStarted = false;

    private Thread burstWorker = new Thread() {
        @Override
        public void run() {
            while(true) {
                synchronized(this) {
                    while(burstStarted == false) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                work();

                burstStarted = false;
                String msg = "Burst finished";
                uiHandler.obtainMessage(GameActivity.MSG_DEBUG_VIEW_ADD, msg).sendToTarget();
            }
        }

        public void work() {
            long periodMilliseconds = 1000 / packetsPerSecond;
            long burstDurationMilliseconds = burstDuration * 1000;

            try {
                String msg = "Starting... waiting countdown " + countDown + " seconds";
                uiHandler.obtainMessage(GameActivity.MSG_DEBUG_VIEW_ADD, msg).sendToTarget();

                Thread.sleep(countDown * 1000);

                long nextSendTime = System.currentTimeMillis();
                long finishTime = System.currentTimeMillis() + burstDurationMilliseconds;

                msg = "Start bursting " + packetsPerSecond + " pkts/s";
                uiHandler.obtainMessage(GameActivity.MSG_DEBUG_VIEW_ADD, msg).sendToTarget();

                while (true) {
                    long currentTime = System.currentTimeMillis();

                    // finish condition
                    if (finishTime < currentTime)
                        break;

                    // adjust packet sending rate
                    nextSendTime += periodMilliseconds;
                    if (nextSendTime > currentTime) {
                        Thread.sleep(nextSendTime - currentTime);
                    }

                    // send
                    sendPacket(null);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    };
    
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

                    long currentTime = System.currentTimeMillis();

                    ByteArrayInputStream byteIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    DataInputStream dataIn = new DataInputStream(byteIn);

                    byte senderId = dataIn.readByte();
                    int seq = dataIn.readInt();
                    int packetType = dataIn.readInt();
                    long senderTime = dataIn.readLong();
                    int remoteSeq = dataIn.readInt();


                    if (packetType == PACKET_BURST_START) {
                        if (burstStarted == true) {
                            continue;
                        }
                        burstStarted = true;

                        packetMap.clear();
                        lastSeq = 0;

                        packetsPerSecond = dataIn.readInt();
                        burstDuration = dataIn.readInt();
                        int prefixSize = dataIn.readInt();
                        byte[] prefixBuffer = new byte[prefixSize];
                        dataIn.read(prefixBuffer, 0, prefixSize);
                        String prefix = new String(prefixBuffer);
                        fileLogger.refreshLog("BURST-" + burstDuration + "sec-" + packetsPerSecond + "pps-" + prefix);

                        uiHandler.obtainMessage(GameActivity.MSG_PLAY_SOUND, "EFFECT_HAMMER").sendToTarget();

                        String msg = "Burst signal received: " + packetsPerSecond + " pps " + burstDuration + " seconds";
                        Logger.d(msg);
                        uiHandler.obtainMessage(GameActivity.MSG_DEBUG_VIEW_SET, msg).sendToTarget();

                        synchronized(burstWorker) {
                            burstWorker.notifyAll();
                        }
                    }
                    else if (packetType == PACKET_TYPE_REGSYNC_2) {
                        long RTT = currentTime - lastSentLocalTime;
                        long timeDiff = currentTime - senderTime - (RTT / 2);

                        timeDifference = timeDiff;
                        sendDirectly(PACKET_TYPE_REGSYNC_3);

                        String msg = "Timediff calculated: " + timeDiff;
                        uiHandler.obtainMessage(GameActivity.MSG_PLAY_SOUND, "EFFECT_RESURRECTION_COUNT").sendToTarget();

                        Logger.d(msg);
                        uiHandler.obtainMessage(GameActivity.MSG_DEBUG_VIEW_ADD, msg).sendToTarget();
                        fileLogger.log(msg);

                    }
                    else if (packetType == PACKET_REFRESH_LOG) {
                        int prefixSize = dataIn.readInt();
                        byte[] prefixBuffer = new byte[prefixSize];
                        dataIn.read(prefixBuffer, 0, prefixSize);
                        String prefix = new String(prefixBuffer);
                        fileLogger.refreshLog(prefix);
                    }
                    else { // non-system packets
                        if (packetMap.containsKey(seq)) {
//                        Logger.d("Packet " + seq + " already received");
                        } else {
                            long latency = currentTime - (senderTime + timeDifference);

                            String msg = "ReceivedPacket " + seq + " remoteNum= " + remoteSeq + " from= " + senderId + " latency= " + latency + " timediff= " + timeDifference;
                            Logger.d(msg);
                            uiHandler.obtainMessage(GameActivity.MSG_DEBUG_VIEW_ADD, msg).sendToTarget();

                            fileLogger.log(msg);
                            packetMap.put(seq, new UDPPacket(0, null));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void sendDirectly(int packetType) {
        try {
            DatagramSocket socket = getSenderSocket();

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(byteOut);

            dataOut.write((byte)clientID);
            dataOut.writeInt(0);
            dataOut.writeInt(packetType);
            long currentTime = System.currentTimeMillis();
            lastSentLocalTime = currentTime;
            dataOut.writeLong(currentTime);
            dataOut.writeInt(0);

            Logger.d("Sending packetType " + packetType);

            byte[] data = byteOut.toByteArray();

            InetAddress addr = InetAddress.getByName(serverAddress);

            DatagramPacket packet = new DatagramPacket(data, data.length, addr, Consts.SERVER_PORT);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    // TODO: Flush expired packets

    private Thread udpSenderWorker = new Thread() {
        @Override
        public void run() {
            Logger.d("Sender running...");

            try {
                DatagramSocket socket = getSenderSocket();

                while (isSending) {
                    long startTime = System.currentTimeMillis();

                    if(sendNTP == true) {
                        sendNTP = false;
                        sendDirectly(PACKET_TYPE_REGSYNC_1);
                        continue;
                    }

                    if(sendOne == true) {
                        sendOne = false;
                        sendPacket(null);
                    }

                    UDPPacket udpPacket = packetQueue.poll();
                    if (udpPacket != null) {
                        if (startTime - udpPacket.getTimestamp() > 2000) {
                            Logger.d("Packet " + udpPacket.toString() + " removed");
                            continue;
                        }

                        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                        DataOutputStream dataOut = new DataOutputStream(byteOut);

                        dataOut.write(clientID);
                        dataOut.writeInt(udpPacket.getSeq());
                        dataOut.writeInt(PACKET_TYPE_FORWARD);
                        dataOut.writeLong(udpPacket.getTimestamp());
                        dataOut.writeInt(udpPacket.getSeq());
                        Logger.d("Sending clientID: " + clientID + " seq: " + udpPacket.getSeq() + " time: " + udpPacket.getTimestamp());

                        byte[] data = byteOut.toByteArray();

                        InetAddress addr = InetAddress.getByName(serverAddress);
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

    private DatagramSocket getSenderSocket() throws Exception {
        if (globalSenderSocket == null) {
            globalSenderSocket = openSocket(Consts.SRC_PORT);
        }

        return globalSenderSocket;
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

    public void startBurstWorker() {
        if (!burstWorker.isAlive())
            burstWorker.start();
    }

    public void sendPacket(byte[] payload) {
        lastSeq = lastSeq + 1;
        UDPPacket udpPacket = new UDPPacket(lastSeq, payload);
        packetQueue.add(udpPacket);
        Logger.d("Packet " + udpPacket.toString() + " added");
    }

}
