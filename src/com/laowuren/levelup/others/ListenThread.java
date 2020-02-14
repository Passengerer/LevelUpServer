package com.laowuren.levelup.others;

import java.io.IOException;
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
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(10000);
						Log.d(TAG, "heart beat");
						GameServer.Rooms.get(roomId).send(CodeUtil.HEARTBEAT, playerId);
					} catch (IOException e) {
						Log.d(TAG, "io exception");
						GameServer.Rooms.get(roomId).destroy();
						break;
					} catch (InterruptedException e) {
					}
				}

			}
		}).start();
	}

	@Override
	public void run() {

		while (in != null) {

			byte b;
			try {
				if ((b = (byte) in.read()) != -1) {
					Log.d(TAG, "code " + (int) b);
					GameServer.Rooms.get(roomId).handle(b, playerId);
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					if (in != null) {
						in.close();
						in = null;
					}
				} catch (Exception ex) {
				}
			}
		}
	}

}
