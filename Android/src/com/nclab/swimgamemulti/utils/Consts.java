package com.nclab.swimgamemulti.utils;

/**
 * User: Changhoon
 * Date: 13. 11. 13
 * Time: �ㅽ썑 11:25
 */
public class Consts {
    public final static boolean LOGGING_ENABLED = true;
    public final static boolean FILE_LOGGING_ENABLED = true;

    /**
     * Network constants
     */

    public final static int MIN_PACKETSIZE = 128;
    public final static int RECV_TIMEOUT = 0;

//    public final static String SERVER_HOSTNAME = "pomato.kaist.ac.kr";
    public final static String SERVER_HOSTNAME = "192.168.0.8";
    public final static int SERVER_PORT = 31342;
    public final static int SRC_PORT = 31343;
    public final static int CLIENT_PORT = 31344;


    /**
     * Game status constants
     */

    public static final int GAME_STATUS_NONE = 0;
    public static final int GAME_STATUS_WAIT = 1;
    public static final int GAME_STATUS_READY = 2;
    public static final int GAME_STATUS_START = 3;
    public static final int GAME_STATUS_ATTACK = 4;
    public static final int GAME_STATUS_DEFENCE = 5;
    public static final int GAME_STATUS_KNOCKDOWN = 7;
    public static final int GAME_STATUS_REST = 8;
    public static final int GAME_STATUS_VICTORY = 9;
    public static final int GAME_STATUS_GAME_OVER = 10;

    /**
     * Game configuration constants
     */

    public static final int DEFAULT_FRAME_RATE = 60;

    /**
     * Game signal constants
     */

    public static final int SIGNAL_CLIENT_HOST = 0;
    public static final int SIGNAL_CLIENT_GUEST = 1;
    public static final int SIGNAL_CLIENT_READY = 2;
    public static final int SIGNAL_CLIENT_START = 3;
}