package com.laowuren.levelup.others;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import main.GameServer;
import utils.CodeUtil;

public class Room {

	private final static String TAG = "Room";

	private int id;
	private Socket[] sockets;
	public int count;
	private static boolean EXIT = false;
	
	private Object obj = new Object();
	
	private ArrayList<Byte> dipai;

	private Deck deck;
	private byte rank0_2 = 0x00;
	private byte rank1_3 = 0x00;
	private byte suit = 0x10;
	private boolean hasSetZhu = false;
	private boolean hasDing = false;
	private boolean hasFanWang = false;
	private boolean isFirstGame = true;
	private boolean dealing = true;
	private boolean reDeal = false;	// 是否需要重新发牌
	private boolean noFan = false;
	private boolean isMaipai = true;
	private int zhuangjia = -1;
	private int turn = -1;
	private int showSuitPlayer = -1;

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
		new Thread(new ListenThread(id, 0, ins[0])).start();
		outs[0] = out;
		count = 1;
		deck = new Deck();
		dipai = new ArrayList<>();
	}
	
	protected void destroy() {
		for (int i = 0; i < 4; ++i) {
			try {
				if (!sockets[i].isClosed()) {
					sockets[i].close();
				}
				if (ins[i] != null) {
					ins[i].close();
				}
				if (outs[i] != null) {
					outs[i].close();
				}
			}catch (Exception e) {}
		}
	}

	public synchronized void handle(byte code, int playerId) {
		if (code == CodeUtil.EXIT) {
			sendEachClient(CodeUtil.EXIT);
			EXIT = true;
			destroy();
			GameServer.Rooms.remove(id);
			Log.d(TAG, "destroy");
		}
		if (!hasSetZhu && CodeUtil.getHeader(code) == CodeUtil.ZHUSUIT) {
			if (zhuangjia == -1) {
				zhuangjia = playerId;
				turn = zhuangjia;
			}
			hasSetZhu = true;
			suit = CodeUtil.getTail(code);
			showSuitPlayer = playerId;
			sendEachClient((byte)(CodeUtil.ZHUSUIT | (byte)suit << 2 | (byte)playerId));
			return;
		}
		if (CodeUtil.getHeader(code) == CodeUtil.FANSUIT) {
			if (playerId == showSuitPlayer) {	// 定牌
				hasDing = true;
				sendEachClient((byte)(CodeUtil.DINGSUIT | (byte)suit << 2 | (byte)playerId));
				return;
			}
			if (CodeUtil.getTail(code) == 4 || CodeUtil.getTail(code) == 5) {
				hasFanWang = true;
				if (CodeUtil.getTail(code) <= suit)
					return;
				if (CodeUtil.getTail(code) == 4) {
					suit = 0x04;
					sendEachClient((byte)(CodeUtil.FANWANG | (byte)0x00 << 2 | (byte)playerId));
				}
				else {
					suit = 0x05;
					sendEachClient((byte)(CodeUtil.FANWANG | (byte)0x01 << 2 | (byte)playerId));
				}
				return;
			}
			// 已经定牌，其他花色不能再反
			if (hasDing || hasFanWang)
				return;
			// 第一局抢庄，未捞底牌，仍可以抢，但不能用王抢
			suit = CodeUtil.getTail(code);
			if (isFirstGame && dealing) {
				if (suit == 4 || suit == 5) {
					return;
				}else {
					zhuangjia = playerId;
					turn = zhuangjia;
				}
			}
			sendEachClient((byte)(CodeUtil.FANSUIT | (byte)suit << 2 | (byte)playerId));
			return;
		}
		if (CodeUtil.getHeader(code) == CodeUtil.BIZHUANG) {
			noFan = true;
			return;
		}
		// 收到底牌
		if (isMaipai) {
			dipai.add(code);
			if (dipai.size() == 8) {
				synchronized (obj) {
					obj.notify();
				}
			}
		}
	}

	public void addSocket(Socket socket, InputStream in, OutputStream out) {
		if (count < 4) {
			sockets[count] = socket;
			ins[count] = in;
			new Thread(new ListenThread(id, count, ins[count])).start();
			outs[count++] = out;
			Log.d(TAG, id + " add. count: " + count);

			if (count == 4) {
				//startListenning();
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
				// while (!EXIT) {
				//deck.shuffle();
				//deck.printAll();
				try {
					Thread.sleep(3000);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (isFirstGame)
					deal(0);
				else deal(zhuangjia);
				if (dealing == false && suit == 0x10) {	// 逼庄家亮花色
					if (zhuangjia != -1) {
						sendEachClient(CodeUtil.BIZHUANG);
					}else {	// 重新发牌
						reDeal = true;
					}
				}
				while (suit == 0x10 && !reDeal) {	// 还未亮花色，等待
					if (noFan) {
						reDeal = true;
					}
				}
				if (reDeal) {
					//continue;
				}
				sendDipaiTo(zhuangjia);
				// 开始轮流说话
				turn = zhuangjia;
				Log.d("turn " + turn, "" + (byte)(CodeUtil.STARTTURN | turn));
				sendEachClient((byte)(CodeUtil.STARTTURN | turn));
				synchronized (obj) {
					while (!EXIT) {
						try {
							obj.wait();
							turn = (turn + 1) % 4;
							Log.d("next turn", "" + turn);
							sendEachClient((byte)(CodeUtil.STARTTURN | turn));
						}catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				Log.d(TAG, "play thread over");
			}
		}).start();
	}

	protected void deal(int first) {
		for (int i = 0; i < 25; ++i) {
			
			for (int j = 0; j < 4; ++j)
				send(deck.get(i * 4 + j), (j + first) % 4);
			
			try {
				Thread.sleep(700);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for (int i = 100; i < 108; ++i) {
			//dipai.add(deck.get(i));
			dipai.add((byte)0x00);
		}
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		dealing = false;
	}
	
	protected void sendDipaiTo(int i) {
		for (int j = 0;j < 8; ++j)
			send(dipai.get(j), i);
		dipai.clear();
	}

	protected void startListenning() {
		for (int i = 0; i < 4; ++i)
			new Thread(new ListenThread(id, i, ins[i])).start();
	}

	protected void sendEachClient(byte instruct) {
		for (int i = 0; i < 4; ++i) {
			send(instruct, i);
		}
	}

	protected synchronized void send(byte instruct, int i) {
		Log.d(TAG, "send " + instruct);
		try {
			outs[i].write(instruct);
			outs[i].flush();
		} catch (Exception e) {
			Log.d(TAG, "write msg exception");
			e.printStackTrace();
		}
	}

}
