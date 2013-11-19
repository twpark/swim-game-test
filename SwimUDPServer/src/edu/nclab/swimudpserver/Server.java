package edu.nclab.swimudpserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
	boolean isSending = true;
	CountDownLatch latch;
	
	HashSet<Long> packetSet;
	LinkedList<PacketID> packetTimeList;
	
    private int lastSeq = 1;
    private ConcurrentLinkedQueue<MyPacket> packetQueue = new ConcurrentLinkedQueue<MyPacket>();

    private int serverStatus = Consts.SERVER_STATUS_WAIT;

	public static void main(String[] args) {
		Server server = new Server();

        Logger.d("Server started");
		server.udpReceiverWorker.start();
		server.udpSenderWorker.start();

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
            if(remaining > 0)
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
			if (System.currentTimeMillis() - curPacketIssuedTime > Consts.PACKET_TTL) {
				packetTimeList.poll();
				Logger.d("Removed packet " + curPacketID.sequence
						+ " from client " + curPacketID.clientID
						+ ", current PacketList size = "
						+ packetTimeList.size());
			} else
				break;
		}
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

					ByteArrayInputStream byteIn = new ByteArrayInputStream(
							packet.getData(), 0, packet.getLength());
					DataInputStream dataIn = new DataInputStream(byteIn);

					int clientID = dataIn.readByte();
					int seq = dataIn.readInt();
                    int payloadSize = dataIn.readInt();
                    byte pBuffer[] = new byte[payloadSize];
                    int readSize = dataIn.read(pBuffer);
//                    System.out.println(readSize);

					long packetIDLong = makeLongPacketID(clientID, seq);

					// check if the packet is already in the set
					if (false == packetSet.contains(packetIDLong)) {
						// new packet
						Logger.d("Received: " + seq + " from client "
								+ clientID);
						packetSet.add(packetIDLong);
						packetTimeList.add(new PacketID(clientID, seq));

                        Logger.d(Logger.byteArrayToHex(pBuffer));

                        GameMessage gameMsg = GameMessage.readFromBytes(pBuffer);
                        Logger.d("a: " + gameMsg.signal);

					} else {
						// redundant packet
						// Logger.d("Received a redundant packet: " + sequence
						// + " from client " + clientID);
					}
					flushPacketTimeList();
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

    private Thread udpSenderWorker = new Thread() {
        @Override
        public void run() {
            Logger.d("Sender running...");
            byte clientId = 1;
            try {
                DatagramSocket socket = openSocket(Consts.SRC_PORT);

                while (isSending) {
                    long startTime = System.currentTimeMillis();
                    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                    DataOutputStream dataOut = new DataOutputStream(byteOut);

                    MyPacket myPacket = packetQueue.poll();
                    if (myPacket != null) {
                        if (startTime - myPacket.getTimestamp() > 2000) {
                            Logger.d("Packet " + myPacket.toString() + " removed");
                            continue;
                        }

                        dataOut.write(clientId);
                        dataOut.writeInt(myPacket.getSeq());
                        Logger.d("Sending " + myPacket.getSeq());

                        byte[] data = byteOut.toByteArray();

                        InetAddress addr = InetAddress.getByName(myPacket.getDestination());
                        
                        DatagramPacket packet = new DatagramPacket(data, data.length, addr, Consts.DEST_PORT);
                        socket.send(packet);

                        packetQueue.add(myPacket);
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
	
    private class MyPacket {
        long timestamp;
        int seq;
        String destination;

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

    public void addMyPacket(String dest) {
        lastSeq = lastSeq + 1;
        MyPacket myPacket = new MyPacket(lastSeq);
        myPacket.setDestination(dest);
        
        packetQueue.add(myPacket);
        Logger.d("Packet " + myPacket.toString() + " added");
    }
}
