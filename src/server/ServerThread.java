package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;

import main.GameServer;
import others.Log;
import others.Message;
import others.Room;

public class ServerThread extends Thread {

	private final static String TAG = "ServerThread";

	private Socket socket;
	private ObjectInputStream ois = null;
	private ObjectOutputStream oos = null;

	private boolean flag = true;

	public ServerThread(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {

		try {
			ois = new ObjectInputStream(socket.getInputStream());
			oos = new ObjectOutputStream(socket.getOutputStream());
		} catch (Exception e) {
			Log.d(TAG, "io exception");
			e.printStackTrace();
		}

		String acceptStr = null;

		while (flag && !socket.isClosed()) {

			acceptStr = null;

			try {
				Message message = (Message)ois.readObject();
				if (message.getWhat() == Message.TEXT) {
					acceptStr = message.getText();
					Log.d(TAG, "accept: " + acceptStr);
				}
			}catch (Exception e) {
				Log.d(TAG, "read exception");
				e.printStackTrace();
			}

			if (acceptStr != null) {
				handleMessage(acceptStr);
			}
		}
		Log.d(TAG, "thread over");
	}

	protected void handleMessage(String message) {
		Log.d(TAG, "handle");

		if ("create room".equalsIgnoreCase(message)) {

			if (GameServer.Rooms.size() >= GameServer.ROOMAMOUNT) {
				Log.d(TAG, ">= room amount");
				try {
					oos.writeObject(new Message(Message.TEXT, "full", null, 0));
					oos.flush();
					
					Thread.sleep(1000);
					if (ois != null)
						ois.close();
					if (oos != null)
						oos.close();
					if (!socket.isClosed())
						socket.close();
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

			Room room = new Room(roomId, socket, ois, oos);
			GameServer.Rooms.put(roomId, room);

			try {
				oos.writeObject(new Message(Message.TEXT, "" + roomId, null, 0));
				oos.flush();
				
				Thread.sleep(1000);
				flag = false;
			} catch (Exception e) {
				Log.d(TAG, "ex 1");
			}

		} else {
			int id = -1;
			try {
				id = Integer.parseInt(message);
			} catch (Exception e) {
			}

			if (GameServer.Rooms.containsKey(id)) {
				GameServer.Rooms.get(id).addSocket(socket, ois, oos);
				flag = false;
			} else {
				try {
					oos.writeObject(new Message(Message.TEXT, "input error", null, 0));
					oos.flush();
					
					Thread.sleep(1000);
					if (ois != null)
						ois.close();
					if (oos != null)
						oos.close();
					if (!socket.isClosed())
						socket.close();
				} catch (Exception e) {
					Log.d(TAG, "ex 2");
				}
			}
		}
	}

}
