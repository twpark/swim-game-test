package edu.nclab.swimudpserver;


public class Consts {
    public final static int MIN_PACKETSIZE = 128;
    public final static int DEST_PORT = 31344;
    public final static int SRC_PORT = 31343;
    public final static int RECV_PORT = 31342;
    
    public final static int RCV_TIMEOUT = 10;

    public final static int PACKETS_PER_SEC = 200;
    
    public final static int RECEIVED_PACKET_TTL = 6000; // ms
    public final static int SENDING_PACKET_TTL = 2000;

    public static final int FRAME_RATE = 60;

    public static final int PACKET_TYPE_NORMAL = 0;
    public static final int PACKET_TYPE_FORWARD = 1;

    public static final int PACKET_TYPE_REGSYNC_1 = 11;
    public static final int PACKET_TYPE_REGSYNC_2 = 12;
    public static final int PACKET_TYPE_REGSYNC_3 = 13;
    public static final int PACKET_BURST_START = 22;

    public static final int PACKET_REFRESH_LOG = 21;

    public static final int GAME_STATUS_NONE = 0;
    public static final int GAME_STATUS_WAIT = 1;
    public static final int GAME_STATUS_READY = 2;
    public static final int GAME_STATUS_START = 3;
    public static final int GAME_STATUS_ATTACK = 4;
    public static final int GAME_STATUS_DEFENCE = 5;
    public static final int GAME_STATUS_REST = 6;
    public static final int GAME_STATUS_VICTORY = 7;
    public static final int GAME_STATUS_GAME_OVER = 8;

    public static final int SERVER_STATUS_WAIT = 1;
    public static final int SERVER_STATUS_READYING = 2;
    public static final int SERVER_STATUS_ALL_READY = 3;
    public static final int SERVER_STATUS_STARTED = 4;
}
