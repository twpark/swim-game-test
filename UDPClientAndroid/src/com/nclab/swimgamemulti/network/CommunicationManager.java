package com.nclab.swimgamemulti.network;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class CommunicationManager extends Handler {
	public NetworkManager networkManager;
    private Handler gameHandler;

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
        networkManager.setServerAddress(serverAddress);
    }

    private String serverAddress;


    public Handler getUiHandler() {
        return uiHandler;
    }

    public void setUiHandler(Handler uiHandler) {
        this.uiHandler = uiHandler;
        this.networkManager.setUiHandler(uiHandler);
    }

    private Handler uiHandler;

    public static final int MSG_INGOING = 0;
    public static final int MSG_OUTGOING = 1;

	public CommunicationManager(Handler gameHandler) {
		networkManager = new NetworkManager(this);
		networkManager.startListening();
		networkManager.startSending();
        networkManager.startBurstWorker();
        this.gameHandler = gameHandler;
	}

	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
        GameMessage gameMsg;
        try {
            switch (msg.what) {
                case MSG_INGOING:
                    Bundle bundle = msg.getData();
                    byte[] payload = bundle.getByteArray("payload");
                    gameMsg = GameMessage.readFromBytes(payload);
                    gameHandler.obtainMessage(gameMsg.signal).sendToTarget();
                    break;
                case MSG_OUTGOING:
                    // Build GameMessage
                    gameMsg = new GameMessage();
                    gameMsg.timestamp = 0;
                    gameMsg.signal = -1;
                    gameMsg.status = 100;

                    networkManager.sendPacket(gameMsg.toByteArray());
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}
