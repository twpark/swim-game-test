package edu.nclab.swimudpserver;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class Server {

    public static final int SERVER_STATUS_WAIT = 1;
    public static final int SERVER_STATUS_READYING = 2;
    public static final int SERVER_STATUS_ALL_READY = 3;
    public static final int SERVER_STATUS_STARTED = 4;

    boolean isServerRunning = true;
    boolean isReceiving = true;
    boolean isSending = false;
    CountDownLatch latch;

    DatagramSocket srcSocket = null;

    HashSet<Long> packetSet;
    LinkedList<PacketID> packetTimeList;
    HashMap<Integer, String> clientIDIPMap;
    HashMap<Integer, Long> clientIDLastReceivedRemoteTimeMap;
    HashMap<Integer, Long> clientIDLastSentLocalTimeMap;
    HashMap<Integer, Long> clientIDTimeDifferenceMap;

    int packetsPerSecond = 1;
    int burstDuration = 20;
    String prefix = "";

    private int lastSeq = 1;
    private ConcurrentLinkedQueue<MyPacket> packetQueue = new ConcurrentLinkedQueue<MyPacket>();

    private int serverStatus = Consts.SERVER_STATUS_WAIT;

    FileLogger fileLogger;

    public static void main(String[] args) {
        Server server = new Server();

        Logger.d("Server started");
        server.udpReceiverWorker.start();
        //server.udpSenderWorker.start();

        GameMessage gMsg = new GameMessage();
        gMsg.timestamp = 0;
        gMsg.signal = -1;
        gMsg.status = 100;
        try {
            byte[] a = gMsg.toByteArray();
            Logger.d(Logger.byteArrayToHex(a));
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            server.run();
            /* for debugging
            Thread.sleep(1000);
			server.addMyPacket("143.248.92.63");
			Thread.sleep(500);
			server.addMyPacket("143.248.92.63");
			Thread.sleep(500);
			server.addMyPacket("143.248.92.63");
			Thread.sleep(1000);
			Thread.sleep(1000);
			Thread.sleep(1000);
			server.addMyPacket("143.248.92.63");
			server.addMyPacket("143.248.92.63");
			server.addMyPacket("143.248.92.63");
			server.addMyPacket("143.248.92.63");
			server.addMyPacket("143.248.92.63");
			server.addMyPacket("143.248.92.63");
			server.addMyPacket("143.248.92.63");
			server.addMyPacket("143.248.92.63");
			Thread.sleep(1000);
			*/
            server.latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Server() {
        latch = new CountDownLatch(2);
        packetSet = new HashSet<Long>();
        packetTimeList = new LinkedList<PacketID>();
        clientIDIPMap = new HashMap<Integer, String>();
        clientIDLastReceivedRemoteTimeMap = new HashMap<Integer, Long>();
        clientIDLastSentLocalTimeMap = new HashMap<Integer, Long>();
        clientIDTimeDifferenceMap = new HashMap<Integer, Long>();

        srcSocket = getSrcSocket();

        fileLogger = new FileLogger("UDP");
    }

    public void run() throws Exception {
        Logger.d("Waiting for players to be ready");
        long curTime = 0, prevTime = 0;
        long frameInterval = 1000 / Consts.FRAME_RATE;

        while (isServerRunning) {
            prevTime = curTime;
            curTime = System.currentTimeMillis();

            update();

            long remaining = frameInterval - (curTime - prevTime);
            if (remaining > 0)
                Thread.sleep(remaining);
        }
    }

    private void update() {

    }

    private class PacketID {
        public int clientID;
        public int sequence;
        public long issuedTime;

        public PacketID(int cID, int seq) {
            this(cID, seq, System.currentTimeMillis());
        }

        public PacketID(int cID, int seq, long time) {
            clientID = cID;
            sequence = seq;
            issuedTime = time;
        }
    }

    // make a long packet ID to use as a key of packetIDSet
    private long makeLongPacketID(int id, int seq) {
        long longID = id;

        longID <<= 32;
        longID += seq;

        return longID;
    }

    private void flushPacketTimeList() {
        while (false == packetTimeList.isEmpty()) {
            PacketID curPacketID = packetTimeList.peek();
            long curPacketIssuedTime = curPacketID.issuedTime;
            if (System.currentTimeMillis() - curPacketIssuedTime > Consts.RECEIVED_PACKET_TTL) {
                packetTimeList.poll();
                Logger.d("Removed packet " + curPacketID.sequence
                        + " from client " + curPacketID.clientID
                        + ", current PacketList size = "
                        + packetTimeList.size());
            } else
                break;
        }
    }

    private DatagramSocket getSrcSocket() {
        if (srcSocket == null) {
            try {
                srcSocket = openSocket(Consts.SRC_PORT);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return srcSocket;
    }

    private Thread udpReceiverWorker = new Thread() {
        @Override
        public void run() {
            Logger.d("Receiver running at port " + Consts.RECV_PORT + "...");

            DatagramSocket socket;
            try {
                socket = openSocket(Consts.RECV_PORT);
            } catch (Exception e) {
                e.printStackTrace();

                return;
            }

            while (isReceiving) {
                try {
                    byte buffer[] = new byte[128];

                    DatagramPacket packet = new DatagramPacket(buffer,
                            buffer.length);
                    socket.setSoTimeout(Consts.RCV_TIMEOUT);
                    socket.receive(packet);

                    long currentTime = System.currentTimeMillis();

                    ByteArrayInputStream byteIn = new ByteArrayInputStream(
                            packet.getData(), 0, packet.getLength());
                    DataInputStream dataIn = new DataInputStream(byteIn);

                    int clientID = dataIn.readByte();
                    int seq = dataIn.readInt();
                    int packetType = dataIn.readInt();
                    long senderTime = dataIn.readLong();
                    int remoteSeq = dataIn.readInt();

                    if (packetType == Consts.PACKET_BURST_START) {
                        packetsPerSecond = dataIn.readInt();
                        burstDuration = dataIn.readInt();
                        int prefixSize = dataIn.readInt();
                        byte[] prefixBuffer = new byte[prefixSize];
                        dataIn.read(prefixBuffer, 0, prefixSize);
                        prefix = new String(prefixBuffer);

                        fileLogger.refreshLog("BURST-" + burstDuration + "sec-" + packetsPerSecond + "pps-" + prefix);


                        broadcastBurstPacket();
                        packetSet.clear();
                        packetTimeList.clear();
                        continue;
                    }

                    if (false == clientIDIPMap.containsKey(clientID))
                        clientIDIPMap.put(clientID, packet.getAddress().getHostAddress());

                    clientIDLastReceivedRemoteTimeMap.put(clientID, senderTime);

                    // process system packets
                    if (packetType == Consts.PACKET_TYPE_REGSYNC_1) {
                        // send the immediate ack and store the sender time
                        sendDirectly(clientID, Consts.PACKET_TYPE_REGSYNC_2);
                    }
                    else if (packetType == Consts.PACKET_TYPE_REGSYNC_3) {
                        long RTT = currentTime - clientIDLastSentLocalTimeMap.get(clientID);
                        long timeDiff = currentTime - senderTime - (RTT / 2);

                        Logger.d("SYNC PACKET 3 RECEIVED: Timediff calculated: " + timeDiff);

                        clientIDTimeDifferenceMap.put(clientID, timeDiff);
                        if (isSending == false) {
                            isSending = true;
                            udpSenderWorker.start();
                            // TODO add sound....
                        }
                    }
                    else { // non-system packets
                        // check if the packet is already in the set
                        long packetIDLong = makeLongPacketID(clientID, seq);
                        if (false == packetSet.contains(packetIDLong)) {
                            // new packet
                            Logger.d("Received: " + seq + " from client "
                                    + clientID);
                            packetSet.add(packetIDLong);
                            packetTimeList.add(new PacketID(clientID, seq));

                            Logger.d("Packet type " + packetType);

                            if (packetType == Consts.PACKET_TYPE_FORWARD) { // forwarding, client to client
                                long latency = currentTime - (senderTime + clientIDTimeDifferenceMap.get(clientID));

                                String msg = "ReceivedPacket " + seq + " remoteNum= " + remoteSeq + " from= " + clientID + " latency= " + latency + " timediff= " + clientIDTimeDifferenceMap.get(clientID);
                                Logger.d(msg);

                                fileLogger.log(msg);

                                broadcastForwardPacket(clientID, senderTime + clientIDTimeDifferenceMap.get(clientID), seq);
                            }
                        } else {
                            // redundant packet
                            // Logger.d("Received a redundant packet: " + sequence
                            // + " from client " + clientID);
                        }
                        flushPacketTimeList();
                    }

                } catch (SocketTimeoutException e) {
                    flushPacketTimeList();
                } catch (Exception e) {
                    e.printStackTrace();
                    isReceiving = false;
                }
            }

            socket.close();
            latch.countDown();
        }
    };

    private void broadcastBurstPacket() {
        for (int clientID : clientIDIPMap.keySet()) {
            addMyPacket(clientIDIPMap.get(clientID), Consts.PACKET_BURST_START, 0, true, 0, 0);
        }
    }

    private void broadcastForwardPacket(int senderID, long timeStamp, int remoteSeq) {
        for (int clientID : clientIDIPMap.keySet()) {
            if (clientID != senderID)
                addMyPacket(clientIDIPMap.get(clientID), Consts.PACKET_TYPE_FORWARD, senderID, true, timeStamp, remoteSeq);
        }
    }

    private void sendDirectly(int clientID, int packetType) {
        try {
            DatagramSocket socket = getSrcSocket();

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(byteOut);

            dataOut.write((byte)clientID);
            dataOut.writeInt(0);
            dataOut.writeInt(packetType);
            long currentTime = System.currentTimeMillis();
            clientIDLastSentLocalTimeMap.put(clientID, currentTime);
            dataOut.writeLong(currentTime);
            dataOut.writeInt(0);
            Logger.d("Sending directly to " + clientID + " packetType " + packetType);

            byte[] data = byteOut.toByteArray();

            InetAddress addr = InetAddress.getByName(clientIDIPMap.get(clientID));

            DatagramPacket packet = new DatagramPacket(data, data.length, addr, Consts.DEST_PORT);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Thread udpSenderWorker = new Thread() {
        @Override
        public void run() {
            Logger.d("Sender running...");
            byte clientId = 1;
            try {
                DatagramSocket socket = getSrcSocket();

                long timeToSend = System.currentTimeMillis();
                while (isSending) {
                    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                    DataOutputStream dataOut = new DataOutputStream(byteOut);

                    MyPacket myPacket = null;
                    synchronized (this) {
                        myPacket = packetQueue.poll();
                        if (myPacket != null) {
                            if (System.currentTimeMillis() - myPacket.getTimestamp() > Consts.SENDING_PACKET_TTL) {
                                Logger.d("Packet " + myPacket.toString() + " removed");
                                continue;
                            }
                            packetQueue.add(myPacket);
                        } else continue;
                    }

                    if (myPacket.getPacketType() == Consts.PACKET_BURST_START) {
                        dataOut.writeByte(0);
                        dataOut.writeInt(0);
                        dataOut.writeInt(Consts.PACKET_BURST_START);
                        dataOut.writeLong(0);
                        dataOut.writeInt(0);
                        dataOut.writeInt(packetsPerSecond);
                        dataOut.writeInt(burstDuration);
                        dataOut.writeInt(prefix.getBytes().length);
                        dataOut.writeBytes(prefix);
                    } else if (myPacket.isForward() == true) {
                        dataOut.writeByte(myPacket.getSenderID());
                        dataOut.writeInt(myPacket.getSeq());
                        dataOut.writeInt(Consts.PACKET_TYPE_FORWARD);
                        dataOut.writeLong(myPacket.getTimeStamp());
                        dataOut.writeInt(myPacket.getRemoteSeq());
                    }
                    else {
                        dataOut.writeByte(-1);
                        dataOut.writeInt(myPacket.getSeq());
                        dataOut.writeInt(myPacket.getPacketType());
                        dataOut.writeLong(myPacket.getTimeStamp());
                        dataOut.writeInt(myPacket.getSeq());
                    }
                    //Logger.d("Sending " + myPacket.getSeq());

                    byte[] data = byteOut.toByteArray();

                    InetAddress addr = InetAddress.getByName(myPacket.getDestination());

                    DatagramPacket packet = new DatagramPacket(data, data.length, addr, Consts.DEST_PORT);

                    try {
                        socket.send(packet);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                    timeToSend += (1000 / Consts.PACKETS_PER_SEC);

                    long remaining = timeToSend - System.currentTimeMillis();
                    if (remaining > 0)
                        Thread.sleep(remaining);
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
     * @throws: MeasurementError if an error occurs
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

    public class MyPacket {
        long timestamp;
        int seq;
        String destination;
        long timeStamp;

        public int getPacketType() {
            return packetType;
        }

        public void setPacketType(int packetType) {
            this.packetType = packetType;
        }

        int packetType;

        public int getSenderID() {
            return senderID;
        }

        public void setSenderID(int senderID) {
            this.senderID = senderID;
        }

        int senderID;

        public boolean isForward() {
            return forward;
        }

        public void setForward(boolean forward) {
            this.forward = forward;
        }

        boolean forward;

        public int getRemoteSeq() {
            return remoteSeq;
        }

        public void setRemoteSeq(int remoteSeq) {
            this.remoteSeq = remoteSeq;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        int remoteSeq;

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public MyPacket(long timestamp, int seq) {
            this.timestamp = timestamp;
            this.seq = seq;
        }

        public MyPacket(int seq) {
            this.timestamp = System.currentTimeMillis();
            this.seq = seq;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public int getSeq() {
            return seq;
        }

        public void setSeq(int seq) {
            this.seq = seq;
        }

        @Override
        public String toString() {
            return "[" + timestamp + ", " + seq + "]";
        }
    }

    public void addMyPacket(String dest, int packetType, int senderID, boolean forward, long timeStamp, int remoteSeq) {
        lastSeq = lastSeq + 1;
        MyPacket myPacket = new MyPacket(lastSeq);
        myPacket.setDestination(dest);
        myPacket.setTimeStamp(timeStamp);
        myPacket.setRemoteSeq(remoteSeq);
        myPacket.setForward(forward);
        myPacket.setSenderID(senderID);
        myPacket.setPacketType(packetType);

        synchronized (this) {
            packetQueue.add(myPacket);
        }

        //Logger.d("Packet " + myPacket.toString() + " added");
    }
}
