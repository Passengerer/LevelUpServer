package others;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class Room {

	private final static String TAG = "Room";

	private int id;
	private Socket[] sockets;
	private int count;

	private Cards cards;
	
	private ObjectInputStream[] oiss;
	private ObjectOutputStream[] ooss;

	public Room(int id, Socket socket, ObjectInputStream ois, ObjectOutputStream oos) {
		Log.d(TAG, id + " init");
		this.id = id;
		sockets = new Socket[4];
		oiss = new ObjectInputStream[4];
		ooss = new ObjectOutputStream[4];
		sockets[0] = socket;
		oiss[0] = ois;
		ooss[0] = oos;
		count = 1;
		cards = new Cards(new Deck().getDeck(), new Deck().getDeck());
	}

	public void addSocket(Socket socket, ObjectInputStream ois, ObjectOutputStream oos) {
		if (count < 4) {
			sockets[count] = socket;
			oiss[count] = ois;
			ooss[count++] = oos;
			Log.d(TAG, id + " add. count: " + count);
			
			if (count == 4) {
				sendEachClient("ready");
				play();
			}
		}
	}

	public void play() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "play");
				while (!sockets[0].isClosed()) {
					cards.shuffle();
					deal();
				}
			}
		}).start();
	}
	
	public void deal() {
		for (int i = 0; i < 4; ++i) {
			
		}
	}
	
	protected void sendEachClient(String msg) {
		for (int i = 0; i < 4; ++i) {
			try {
				ooss[i].writeObject(new Message(Message.TEXT, msg, null, 0));
			}catch (Exception e) {
				Log.d(TAG, "write obj exception");
			}
		}
	}

}
