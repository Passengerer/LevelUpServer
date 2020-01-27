package com.laowuren.levelup.others;

import java.io.ObjectInputStream;

public class ListenThread extends Thread {
	
	private final static String TAG = "ListenThread";
	
	private int roomId;
	private int playerId;
	private ObjectInputStream ois;
	
	public ListenThread(int roomId, int playerId, ObjectInputStream ois) {
		this.roomId = roomId;
		this.playerId = playerId;
		this.ois = ois;
	}
	
	@Override
	public void run() {
		while (ois != null) {
			try {
				MyMessage message = (MyMessage)ois.readObject();
				
			}catch (Exception e) {
				Log.d(TAG, "room:" + roomId + " player:" + playerId + "exception");
				e.printStackTrace();
			}
			
		}
	}

}
