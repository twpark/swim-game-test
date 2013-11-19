package com.nclab.swimgamemulti.network;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.nclab.swimgamemulti.utils.Consts;
import com.nclab.swimgamemulti.utils.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * User: Changhoon
 * Date: 13. 11. 13
 */
public class NetworkManager {
    private String localAddr;
    private boolean isReceiving = false;
    private boolean isSending = false;

    private int lastSeq = 1;
    private HashMap<Integer, UDPPacket> packetMap = new HashMap<Integer, UDPPacket>();
    private ConcurrentLinkedQueue<UDPPacket> receivedPacketQueue = new ConcurrentLinkedQueue<UDPPacket>();
    private ConcurrentLinkedQueue<UDPPacket> packetQueue = new ConcurrentLinkedQueue<UDPPacket>();
    
    private Handler receiveHandler;

    public NetworkManager(Handler receiveHandler) {
    	this.receiveHandler = receiveHandler;
        this.localAddr = getIpAddress();
    }
    
    private Thread udpReceiverWorker = new Thread() {
        @Override
        public void run() {
            Logger.d("Receiver running...");
            try {
                DatagramSocket socket = openSocket(Consts.CLIENT_PORT);
                while (isReceiving) {
                    byte buffer[] = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.setSoTimeout(Consts.RECV_TIMEOUT);
                    Logger.d("Waiting for packet");
                    socket.receive(packet);
                    Logger.d("Packet received");

                    ByteArrayInputStream byteIn = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    DataInputStream dataIn = new DataInputStream(byteIn);

                    int serverId = dataIn.readInt();
                    int seq = dataIn.readInt();
                    String addr = dataIn.readUTF();
                    int payloadSize = dataIn.readInt();
                    byte pBuffer[] = new byte[payloadSize];
                    int readSize = dataIn.read(pBuffer);
                    if (packetMap.containsKey(seq)) {
                        Logger.d("Packet " + seq + " already received");
                    } else {
                        Logger.d("Processing received packet");
                        UDPPacket udpPacket = new UDPPacket(seq, addr, pBuffer);
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
                        dataOut.writeUTF(udpPacket.getAddr());
                        dataOut.writeInt(udpPacket.getPayload().length);
                        dataOut.write(udpPacket.getPayload());
//                        Logger.d("Sending " + Logger.byteArrayToHex(udpPacket.getPayload()));

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
        UDPPacket udpPacket = new UDPPacket(lastSeq, localAddr, payload);
        packetQueue.add(udpPacket);
        Logger.d("Packet " + udpPacket.toString() + " added");
    }

    public String getLocalIpAddress() {
        try {
            String ipv4;
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    System.out.println("ip1--:" + inetAddress);
                    System.out.println("ip2--:" + inetAddress.getHostAddress());

                    // for getting IPV4 format
                    if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4 = inetAddress.getHostAddress())) {

                        String ip = inetAddress.getHostAddress().toString();
                        Logger.d("IP: " + ipv4);
                        return ipv4;
                    }
                }
            }
        } catch (Exception ex) {
            Logger.e(ex.toString());
        }
        return null;
    }

    public String getIpAddress() {
        String ip = "NA";
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet("http://ip2country.sourceforge.net/ip2c.php?format=JSON");
            // HttpGet httpget = new HttpGet("http://whatismyip.com.au/");
            // HttpGet httpget = new HttpGet("http://www.whatismyip.org/");
            HttpResponse response;

            response = httpclient.execute(httpget);
            //Log.i("externalip",response.getStatusLine().toString());

            HttpEntity entity = response.getEntity();
            entity.getContentLength();
            String str = EntityUtils.toString(entity);
            JSONObject json_data = new JSONObject(str);
            ip = json_data.getString("ip");
        } catch (Exception e) {
            Logger.e(e.toString());
            e.printStackTrace();
        }

        return ip;
    }
}
