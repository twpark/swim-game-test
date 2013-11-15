package com.nclab.swimgamemulti.network;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class CommunicationManager extends Handler {
	private NetworkManager networkManager;
	
	public CommunicationManager() {
		networkManager = new NetworkManager(this);
		networkManager.startListening();
		networkManager.startSending();
	}

	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
		
		Bundle bundle = msg.getData();
		byte[] payload = bundle.getByteArray("payload");
	}
}
