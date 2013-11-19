package com.nclab.swimgamemulti.network;

public class UDPPacket {
	long timestamp;
	int seq;
    String addr;
	byte[] payload;

	public UDPPacket(long timestamp, int seq, String addr, byte[] payload) {
		this.timestamp = timestamp;
		this.seq = seq;
        this.addr = addr;
		this.payload = payload;
	}

	public UDPPacket(int seq, String addr, byte[] payload) {
		this.timestamp = System.currentTimeMillis();
		this.seq = seq;
        this.addr = addr;
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

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	@Override
	public String toString() {
		return "[" + timestamp + ", " + seq + "]";
	}

	@Override
	public int hashCode() {
		return seq;
	}
}
