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
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class Server {

    public static final int SERVER_STATUS_WAIT = 1;
    public static final int SERVER_STATUS_READYING = 2;
    public static final int SERVER_STATUS_ALL_READY = 3;
    public static final int SERVER_STATUS_STARTED = 4;

    private static final int SIGNAL_READY = 100;
    private static final int SIGNAL_DESIGNATE_ID = 101;
    private static final int SIGNAL_START_REQUEST = 102;
    private static final int SIGNAL_START_ACK = 103;

    boolean isServerRunning = true;
	boolean isReceiving = true;
	boolean isSending = true;
	CountDownLatch latch;

    Map<Integer, Player> playerMap;
    ConcurrentLinkedQueue<GamePacket> receivedPacketQueue;

	TreeSet<Long> packetSet;
	LinkedList<PacketID> packetTimeList;

    int readyCount = 0;
	
    private int lastSeq = 1;
    private int lastGeneratedId = 0;
    private ConcurrentLinkedQueue<MyPacket> packetQueue = new ConcurrentLinkedQueue<MyPacket>();

    private int serverStatus = SERVER_STATUS_WAIT;

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
//            server.addMyPacket("143.248.92.63");
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

        playerMap = new HashMap<Integer, Player>();
        receivedPacketQueue = new ConcurrentLinkedQueue<GamePacket>();
		packetSet = new TreeSet<Long>();
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
        while (receivedPacketQueue.isEmpty()) {}

        GamePacket gamePacket = receivedPacketQueue.poll();

        switch (gamePacket.getGameMsg().signal) {
            case SIGNAL_READY:
                if (serverStatus == SERVER_STATUS_WAIT) {
                    serverStatus = SERVER_STATUS_READYING;
                    readyCount = 1;
                    Player newPlayer = new Player(generateId(), gamePacket.getHostname());
                    newPlayer.setStatus(Consts.GAME_STATUS_READY);
                    int newId = newPlayer.getId();
                    playerMap.put(newId, newPlayer);
                    Logger.d("Player " + newPlayer.getId() + " got ready");

                    GameMessage gameMsg = new GameMessage();
                    gameMsg.id = newId;
                    gameMsg.signal = SIGNAL_DESIGNATE_ID;
                    gameMsg.status = serverStatus;
                    sendGameMessage(newId, gameMsg);
                    // Return id packet
                } else if (serverStatus == SERVER_STATUS_READYING) {
                    if (!playerMap.containsKey(gamePacket.getId())) {
                        Player newPlayer = new Player(generateId(), gamePacket.getHostname());
                        newPlayer.setStatus(Consts.GAME_STATUS_READY);
                        playerMap.put(newPlayer.getId(), newPlayer);
                        int newId = newPlayer.getId();
                        playerMap.put(newId, newPlayer);

                        GameMessage gameMsg = new GameMessage();
                        gameMsg.id = newId;
                        gameMsg.signal = SIGNAL_DESIGNATE_ID;
                        gameMsg.status = serverStatus;
                        sendGameMessage(newId, gameMsg);

                        readyCount = readyCount + 1;
                        // Return id packet
                    } else {
                        Logger.d("Player " + gamePacket.getId() + " already registered");
                    }
                }

                if (readyCount == 2) {
                    serverStatus = SERVER_STATUS_ALL_READY;

                }
                break;
            case SIGNAL_START_REQUEST:
                if (serverStatus == SERVER_STATUS_ALL_READY) {
                    for (Player player : playerMap.values()) {
                        GameMessage gameMsg = new GameMessage();
                        gameMsg.signal = SIGNAL_START_ACK;
                        sendGameMessage(player.getId(), gameMsg);
                    }
                    serverStatus = SERVER_STATUS_STARTED;
                    Logger.d("Game started with " + playerMap.size() + " players");
                }
                break;
        };
    }

    private class GamePacket {
        public int id;
        public String hostname;
        public int seq;
        public byte[] payload;
        public GameMessage gameMsg;

        private GamePacket(int id, String hostname, int seq, byte[] payload) {
            this.id = id;
            this.hostname = hostname;
            this.seq = seq;
            this.payload = payload;
            this.gameMsg = GameMessage.readFromBytes(payload);
        }

        private int getId() {
            return id;
        }

        private String getHostname() {
            return hostname;
        }

        private int getSeq() {
            return seq;
        }

        private byte[] getPayload() {
            return payload;
        }

        private GameMessage getGameMsg() {
            return gameMsg;
        }
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

					int clientID = dataIn.readInt();
					int seq = dataIn.readInt();
                    String sourceAddr = dataIn.readUTF();
                    int payloadSize = dataIn.readInt();
                    byte pBuffer[] = new byte[payloadSize];
                    int readSize = dataIn.read(pBuffer);

					long packetIDLong = makeLongPacketID(clientID, seq);

					// check if the packet is already in the set
					if (false == packetSet.contains(packetIDLong)) {
						// new packet
						Logger.d("Received: " + seq + " from client "
								+ clientID);
						packetSet.add(packetIDLong);
						packetTimeList.add(new PacketID(clientID, seq));

                        Logger.d(Logger.byteArrayToHex(pBuffer));

                        GamePacket gamePacket = new GamePacket(clientID, sourceAddr, seq, pBuffer);
                        Logger.d(sourceAddr);

                        receivedPacketQueue.add(gamePacket);
                        Logger.d("a: " + gamePacket.getGameMsg().signal);

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

                        dataOut.writeInt(clientId);
                        dataOut.writeInt(myPacket.getSeq());
                        dataOut.writeUTF("server"); // TODO: Get server's real ip address and put in here.
                        dataOut.writeInt(myPacket.payload.length);
                        dataOut.write(myPacket.payload);
                        Logger.d("Sending " + myPacket.getSeq() + " to " + myPacket.getDestination());

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
        byte[] payload;

        public String getDestination() {
			return destination;
		}

		public void setDestination(String destination) {
			this.destination = destination;
		}

		public MyPacket(long timestamp, int seq, byte[] payload) {
            this.timestamp = timestamp;
            this.seq = seq;
            this.payload = payload;
        }

        public MyPacket(int seq, byte[] payload) {
            this.timestamp = System.currentTimeMillis();
            this.seq = seq;
            this.payload = payload;
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

    public void sendGameMessage(int destId, GameMessage gameMsg) {
        sendPacket(destId, gameMsg.toByteArray());
    }

    public void sendPacket(int destId, byte[] payload) {
        if (playerMap.containsKey(destId)) {
            lastSeq = lastSeq + 1;
            MyPacket myPacket = new MyPacket(lastSeq, payload);
            String destHostname =  playerMap.get(destId).getHostname();
            myPacket.setDestination(destHostname);
            packetQueue.add(myPacket);
            Logger.d("Packet " + myPacket.toString() + " added");
        } else {
            Logger.d("Id " + destId + " is not registered");
        }
    }

    public void addMyPacket(String destAddr) {
        lastSeq = lastSeq + 1;
        MyPacket myPacket = new MyPacket(lastSeq, new byte[]{});
        myPacket.setDestination(destAddr);
        packetQueue.add(myPacket);
        Logger.d("Packet " + myPacket.toString() + " added");
    }

    private int generateId() {
        lastGeneratedId = lastGeneratedId + 1;
        return lastGeneratedId;
    }
}
