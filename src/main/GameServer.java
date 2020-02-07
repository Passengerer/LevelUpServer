package main;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

import com.laowuren.levelup.others.Log;
import com.laowuren.levelup.others.Room;

import server.ServerThread;

public class GameServer {

	private final static String TAG = "GameServer";
	private final static int PORT = 9798;
	private static ServerSocket serverSocket = null;
	public static int ROOMAMOUNT = 3;
	
	public static HashMap<Integer, Room> Rooms; 

	public static void main(String[] args) {
		
		Rooms = new HashMap<Integer, Room>();

		try {
			serverSocket = new ServerSocket(PORT);

			Socket socket = null;

			while (true) {
				socket = serverSocket.accept();
				new ServerThread(socket).start();

			}
		} catch (Exception e) {
			Log.d(TAG, "error");
			e.printStackTrace();
		}
	}

}