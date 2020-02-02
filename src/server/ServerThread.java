package server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

import com.laowuren.levelup.others.Log;
import com.laowuren.levelup.others.Room;
import utils.CodeUtil;

import main.GameServer;

public class ServerThread extends Thread {

	private final static String TAG = "ServerThread";

	private Socket socket;
	private InputStream in = null;
	private OutputStream out = null;

	private boolean flag = true;

	public ServerThread(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {

		try {
			in = socket.getInputStream();
			out = socket.getOutputStream();
		} catch (Exception e) {
			Log.d(TAG, "io exception");
			e.printStackTrace();
		}

		while (flag && !socket.isClosed()) {

			byte b;
			try {
				if ((b = (byte)in.read()) != -1) {
					Log.d(TAG, "code " + (int)b);
					handle(b);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Log.d(TAG, "thread over");
	}

	protected void handle(byte instruct) {
		Log.d(TAG, "handle");

		if (instruct == CodeUtil.CREATE) {

			if (GameServer.Rooms.size() >= GameServer.ROOMAMOUNT) {
				Log.d(TAG, ">= room amount");
				try {
					out.write(CodeUtil.FAILED1);
					out.flush();
				} catch (Exception e) {
					Log.d(TAG, "ex 0");
				}
				return;
			}

			Random random = new Random();
			int roomId = 0;

			do {
				roomId = random.nextInt(GameServer.ROOMAMOUNT);
			} while (GameServer.Rooms.containsKey(roomId));

			Room room = new Room(roomId, socket, in, out);
			GameServer.Rooms.put(roomId, room);

			try {
				out.write(CodeUtil.ROOMID | (byte)(roomId << 2) | (byte)(room.count - 1));
				out.flush();

				Thread.sleep(1000);
				flag = false;
			} catch (Exception e) {
				Log.d(TAG, "ex 1");
			}

		} else if (CodeUtil.getHeader(instruct) == CodeUtil.ROOMID) {
			
			int id = CodeUtil.getTail(instruct);

			if (GameServer.Rooms.containsKey(id)) {
				try {
					if (GameServer.Rooms.get(id).count < 4) {
						out.write(CodeUtil.ROOMID | (byte)(id << 2) | (byte)GameServer.Rooms.get(id).count);
						out.flush();
						GameServer.Rooms.get(id).addSocket(socket, in, out);
					} else {
						out.write(CodeUtil.FAILED2);
						out.flush();
					}
					Thread.sleep(1000);
				} catch (Exception e) {
					Log.d(TAG, "full ex");
				}
				flag = false;
			} else {
				try {
					out.write(CodeUtil.FAILED3);
					out.flush();

					Thread.sleep(1000);
				} catch (Exception e) {
					Log.d(TAG, "ex 2");
				}
			}
		}
	}

}
