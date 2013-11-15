package com.nclab.swimgamemulti.network;

public class UDPPacket {
	long timestamp;
	int seq;
	byte[] payload;

	public UDPPacket(long timestamp, int seq, byte[] payload) {
		this.timestamp = timestamp;
		this.seq = seq;
		this.payload = payload;
	}

	public UDPPacket(int seq, byte[] payload) {
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
