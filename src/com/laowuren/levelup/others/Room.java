package com.laowuren.levelup.others;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import utils.CodeUtil;

public class Room {

	private final static String TAG = "Room";

	private int id;
	private Socket[] sockets;
	public int count;

	private Deck deck;

	private InputStream[] ins;
	private OutputStream[] outs;

	public Room(int id, Socket socket, InputStream in, OutputStream out) {
		Log.d(TAG, id + " init");
		this.id = id;
		sockets = new Socket[4];
		ins = new InputStream[4];
		outs = new OutputStream[4];
		sockets[0] = socket;
		ins[0] = in;
		outs[0] = out;
		count = 1;
		deck = new Deck();
	}

	public void addSocket(Socket socket, InputStream in, OutputStream out) {
		if (count < 4) {
			sockets[count] = socket;
			ins[count] = in;
			outs[count++] = out;
			Log.d(TAG, id + " add. count: " + count);

			if (count == 4) {
				startListenning();
				sendEachClient(CodeUtil.READY);
				play();
			}
		}
	}

	protected void play() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "play");
				// while (!sockets[0].isClosed()) {
				deck.shuffle();
				deck.printAll();
				try {
					Thread.sleep(3000);
				} catch (Exception e) {
					e.printStackTrace();
				}
				deal();
				// }
			}
		}).start();
	}

	protected void deal() {
		try {
			for (int i = 0; i < 25; ++i) {
				for (int j = 0; j < 4; ++j) {
					outs[j].write(deck.get(i * 4 + j));
					outs[j].flush();
				}
				Thread.sleep(700);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void startListenning() {
		// for (int i = 0; i < 4; ++i)
		 new Thread(new ListenThread(id, 0, ins[0])).start();
	}

	protected void sendEachClient(byte instruct) {
		for (int i = 0; i < 4; ++i) {
			try {
				outs[i].write(instruct);
				;
				outs[i].flush();
			} catch (Exception e) {
				Log.d(TAG, "write msg exception");
			}
		}
	}

}
