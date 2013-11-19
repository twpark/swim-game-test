package edu.nclab.swimudpserver;

/**
 * Created with IntelliJ IDEA.
 * User: nclab
 * Date: 13. 11. 19
 * Time: 오후 4:40
 * To change this template use File | Settings | File Templates.
 */
public class Player {
    private int id;
    private String hostname;
    private int lastSeq; // Sequence number of last received packet from this player.

    private int hp;
    private int status;

    public Player(int id, String hostname) {
        this.id = id;
        this.hostname = hostname;
        this.lastSeq = 0;
        this.hp = Consts.DEFAULT_PLAYER_HP;
        this.status = Consts.GAME_STATUS_WAIT;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getLastSeq() {
        return lastSeq;
    }

    public void setLastSeq(int lastSeq) {
        this.lastSeq = lastSeq;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
