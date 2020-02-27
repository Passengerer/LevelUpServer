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
	private boolean stop = false;

	public ListenThread(int roomId, int playerId, InputStream in) {
		this.roomId = roomId;
		this.playerId = playerId;
		this.in = in;
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (!stop) {
					try {
						Thread.sleep(5000);
						Log.d(TAG, "heart beat");
						GameServer.Rooms.get(roomId).send(CodeUtil.HEARTBEAT, playerId);
					} catch (IOException e) {
					} catch (InterruptedException e) {
					}
				}
				Log.d(TAG, "sending over");
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
				Log.d(TAG, "read exception");
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
		stop = true;
		Log.d(TAG, "listenning over");
	}

}
