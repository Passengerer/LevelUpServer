package com.laowuren.levelup.others;

import java.io.InputStream;

import main.GameServer;
import utils.CodeUtil;

public class ListenThread extends Thread {
	
	private final static String TAG = "ListenThread";
	
	private int roomId;
	private int playerId;
	private InputStream in;
	
	public ListenThread(int roomId, int playerId, InputStream in) {
		this.roomId = roomId;
		this.playerId = playerId;
		this.in = in;
	}
	
	@Override
	public void run() {
		
		while (in != null) {

			byte b;
			try {
				if ((b = (byte)in.read()) != -1) {
					Log.d(TAG, "code " + (int)b);
					GameServer.Rooms.get(roomId).handle(b, playerId);
				}
			} catch (Exception e) {
				e.printStackTrace();
				GameServer.Rooms.get(roomId).handle(CodeUtil.EXIT, playerId);
				try {
					if (in != null) {
						in.close();
					}
				}catch (Exception ex) {}
			}
		}
	}

}
