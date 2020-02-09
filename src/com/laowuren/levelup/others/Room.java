package com.laowuren.levelup.others;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.ArrayList;
import java.util.Comparator;

import main.GameServer;
import utils.CodeUtil;

public class Room {

	private final static String TAG = "Room";

	private int id;
	private Socket[] sockets;
	public int count;
	private static boolean EXIT = false;
	private GAMESTATE stat = GAMESTATE.DEAL;
	private CardComparator cardComparator = new CardComparator();
	private CodeComparator codeComparator = new CodeComparator();
	private PlayRuler ruler;

	private Object bizhuangLock = new Object();
	private Object maipaiLock = new Object();
	private Object playLock = new Object();
	private boolean canNotify = false;

	private ArrayList<Byte> dipai;
	private ArrayList<Byte>[] playCards;
	private ArrayList<Byte>[] handCards;

	private Deck deck;
	private byte[] ranks;
	private byte suit = 0x10;
	private Card zhuCard;
	private boolean hasSetZhu = false;
	private boolean hasDing = false;
	private boolean hasFanWang = false;
	private boolean isFirstGame = true;
	private boolean reDeal = false; // 是否需要重新发牌
	private boolean noFan = false;
	private boolean isBizhuang = false;
	private int maipairenID;
	private int zhuangjia = -1;
	private int turn = -1;
	private int firstPlayerId = -1;
	private int showSuitPlayer = -1;
	private int bufanCount = 0;
	private int playCardsCount;

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
		ruler = new PlayRuler();
		deck.shuffle();
		dipai = new ArrayList<>();
		ranks = new byte[2];
		ranks[0] = 0x00;
		ranks[1] = 0x00;
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
			} catch (Exception e) {
			}
		}
	}
	
	protected void removeCards(ArrayList<Byte> handCards, ArrayList<Byte> playCards) {
		for (byte p : playCards) {
			for (int i = 0; i < handCards.size(); ++i) {
				if (handCards.get(i) == p) {
					handCards.remove(i);
					break;
				}
			}
		}
	}

	public synchronized void handle(byte code, int playerId) {
		if (code == CodeUtil.EXIT) {
			EXIT = true;
			sendEachClient(CodeUtil.EXIT);
			destroy();
			GameServer.Rooms.remove(id);
			Log.d(TAG + " " + id, "destroy");
			return;
		}
		if (stat != GAMESTATE.PLAY && CodeUtil.getHeader(code) == CodeUtil.BIZHUANG) {
			noFan = true;
			synchronized (bizhuangLock) {
				bizhuangLock.notify();
			}
			return;
		}
		if (stat != GAMESTATE.PLAY && CodeUtil.getHeader(code) == CodeUtil.ZHUSUIT) {
			if (hasSetZhu)
				return;
			if (isBizhuang) {
				isBizhuang = false;
				noFan = false;
				synchronized (bizhuangLock) {
					bizhuangLock.notify();
				}
			}
			if (isFirstGame) {
				zhuangjia = playerId;
				turn = zhuangjia;
			}
			hasSetZhu = true;
			suit = CodeUtil.getTail(code);
			Log.d("zhusuit", "" + suit);
			showSuitPlayer = playerId;
			sendEachClient((byte) (CodeUtil.ZHUSUIT | (byte) suit << 2 | (byte) playerId));
			return;
		}
		if (stat != GAMESTATE.PLAY && CodeUtil.getHeader(code) == CodeUtil.FANSUIT) {
			if (playerId == showSuitPlayer) { // 定牌
				hasDing = true;
				sendEachClient((byte) (CodeUtil.DINGSUIT | (byte) suit << 2 | (byte) playerId));
				return;
			}
			if (CodeUtil.getTail(code) == 4 || CodeUtil.getTail(code) == 5) {
				if (isFirstGame && stat == GAMESTATE.DEAL)
					return;
				hasFanWang = true;
				if (CodeUtil.getTail(code) <= suit)
					return;
				if (CodeUtil.getTail(code) == 4) {
					suit = 0x04;
					showSuitPlayer = playerId;
					sendEachClient((byte) (CodeUtil.FANWANG | (byte) 0x00 << 2 | (byte) playerId));
				} else {
					suit = 0x05;
					showSuitPlayer = playerId;
					sendEachClient((byte) (CodeUtil.FANWANG | (byte) 0x01 << 2 | (byte) playerId));
				}
				if (stat == GAMESTATE.MAIPAI) {
					maipairenID = playerId;
					sendEachClient((byte) (CodeUtil.MAIPAITURN | (maipairenID << 2 | turn)));
					sendCardsTo(playerId, dipai);
				}
				return;
			}
			// 已经定牌，其他花色不能再反
			if (hasDing || hasFanWang)
				return;
			// 第一局抢庄，未捞底牌，仍可以抢，但不能用王抢
			suit = CodeUtil.getTail(code);
			showSuitPlayer = playerId;
			if (isFirstGame && stat == GAMESTATE.DEAL) {
				zhuangjia = playerId;
				turn = zhuangjia;
			}
			sendEachClient((byte) (CodeUtil.FANSUIT | (byte) suit << 2 | (byte) playerId));
			if (stat == GAMESTATE.MAIPAI) {
				maipairenID = playerId;
				sendEachClient((byte) (CodeUtil.MAIPAITURN | (maipairenID << 2 | turn)));
				sendCardsTo(playerId, dipai);
			}
			return;
		}
		if (stat == GAMESTATE.PLAY && (code & 0xe0) == 0xe0) {
			playCardsCount = code & 0x1f;
			Log.d("playCards", playCardsCount + "");
			return;
		}
		if (stat == GAMESTATE.PLAY) {
			playCards[playerId].add(code);
			if (playCards[playerId].size() == playCardsCount) {
				Log.d("playerId", "" + playerId);
				Log.d("firstPlayerId", "" + firstPlayerId);
				Log.d("play", "" + playCards[playerId]);
				playCards[playerId].sort(codeComparator);
				handCards[(playerId + 1) % 4].sort(codeComparator);
				handCards[(playerId + 2) % 4].sort(codeComparator);
				handCards[(playerId + 3) % 4].sort(codeComparator);
				sendEachCards(playCards[playerId]);
				if (playerId == firstPlayerId && 
						ruler.getType(playCards[playerId]) == PlayRuler.SHUAI) {
					Log.d("shuaipai", "true");
					ArrayList<Byte> tuihuiCards = ruler.checkShuai(playCards[playerId],
							handCards[(playerId + 1) % 4], handCards[(playerId + 2) % 4], 
							handCards[(playerId + 3) % 4]);
					if (tuihuiCards == null) {
						// 甩牌成功
						Log.d("shuai pai", "success");
						synchronized (playLock) {
							playLock.notify();
						}
					}else {
						// 甩牌失败,等待几秒,退回部分牌
						Log.d("shuai pai", "failed");
						try {
							Thread.sleep(4000);
						}catch (Exception e) {}
						sendEachClient(CodeUtil.SHUAIFAIL);
						for (byte b : tuihuiCards) {
							playCards[playerId].remove((Object)b);
						}
						sendEachCards(playCards[playerId]);
						sendCardsTo(playerId, tuihuiCards);
						removeCards(handCards[playerId], playCards[playerId]);
						synchronized (playLock) {
							playLock.notify();
						}
					}
				}else {
					Log.d("shuaipai", "false");
					removeCards(handCards[playerId], playCards[playerId]);
					synchronized (playLock) {
						playLock.notify();
					}
				}
			}
		}
		// 收到底牌
		if (stat == GAMESTATE.MAIPAI && code != CodeUtil.BUFAN) {
			dipai.add(code);
			if (dipai.size() == 8) {
				Log.d("receive dipai", dipai.toString());
				bufanCount = 0;
				maipairenID = playerId;
				removeCards(handCards[maipairenID], dipai);
				canNotify = true;
			}
		}
		if (code == CodeUtil.BUFAN) {
			++bufanCount;
			canNotify = true;
		}
		if (canNotify) {
			canNotify = false;
			synchronized (maipaiLock) {
				maipaiLock.notify();
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
				sendEachClient(CodeUtil.READY);
				playCards = new ArrayList[4];
				playCards[0] = new ArrayList<Byte>();
				playCards[1] = new ArrayList<Byte>();
				playCards[2] = new ArrayList<Byte>();
				playCards[3] = new ArrayList<Byte>();
				handCards = new ArrayList[4];
				handCards[0] = new ArrayList<Byte>();
				handCards[1] = new ArrayList<Byte>();
				handCards[2] = new ArrayList<Byte>();
				handCards[3] = new ArrayList<Byte>();
				play();
			}
		}
	}

	protected void play() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// while (!EXIT) {

				deck.shuffle();
				if (isFirstGame) {
					deal(0);
				} else {
					deal(zhuangjia);
				}
				sendEachClient(CodeUtil.SUCCESS);
				if (!hasSetZhu) {
					if (!isFirstGame) {
						isBizhuang = true;
						sendEachClient((byte)(CodeUtil.BIZHUANG | zhuangjia));
						synchronized (bizhuangLock) {
							try {
								bizhuangLock.wait();
								if (noFan) {
									reDeal = true;
								}else {
									reDeal = false;
								}
							}catch (Exception e) {e.printStackTrace();}
						}
					}else {
						reDeal = true;
					}
				}
				if (reDeal) {
					Log.d("redeal", "redeal");
					//continue;
				}
				// 开始轮流埋牌
				turn = zhuangjia;
				maipairenID = zhuangjia;
				Log.d("turn " + turn, "" + (byte) (CodeUtil.MAIPAITURN | (maipairenID << 2 | turn)));
				sendEachClient((byte) (CodeUtil.MAIPAITURN | (maipairenID << 2 | turn)));
				stat = GAMESTATE.MAIPAI;
				sendCardsTo(zhuangjia, dipai);
				dipai.clear();
				synchronized (maipaiLock) {
					while (!EXIT) {
						try {
							maipaiLock.wait();
							Log.d("bufan", "" + bufanCount);
							if (bufanCount == 3) {
								break;
							}
							turn = (turn + 1) % 4;
							sendEachClient((byte) (CodeUtil.MAIPAITURN | (maipairenID << 2 | turn)));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				setZhuCardAndComparator();
				// 开始轮流打牌
				turn = zhuangjia;
				firstPlayerId = turn;
				sendEachClient((byte)(CodeUtil.FIRSTPLAY | turn));
				sendEachClient((byte) (CodeUtil.PLAYTURN | turn));
				stat = GAMESTATE.PLAY;
				synchronized (playLock) {
					while (!EXIT) {
						try {
							playLock.wait();
							turn = (turn + 1) % 4;
							Log.d("next turn", "" + turn);
							sendEachClient((byte) (CodeUtil.PLAYTURN | turn));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				Log.d(TAG, "play thread over");
			}
		}).start();
	}

	protected void deal(int first) {
		try {
			Thread.sleep(2500);
			for (int i = 0; i < 25; ++i) {

				for (int j = 0; j < 4; ++j) {
					byte code = deck.get(i * 4 + j);
					/*send(code, (j + first) % 4);
					handCards[(j + first) % 4].add(code);*/
					if (i < 1) {
						send((byte)0x00, j);
						handCards[j].add((byte)0x00);
					}
					else if(i < 3) {
						send((byte)0x02, j);
						handCards[j].add((byte)0x02);
					}
					else if(i < 5) {
						send((byte)0x03, j);
						handCards[j].add((byte)0x03);
					}
					else if (i < 7) {
						send((byte)0x04, j);
						handCards[j].add((byte)0x04);
					}
					else if (i < 9) {
						send((byte)0x05, j);
						handCards[j].add((byte)0x05);
					}
					else {
						send((byte)0x16, j);
						handCards[j].add((byte)0x16);
					}
				}
				Thread.sleep(600);
				Log.d("hand0", handCards[0].toString());
				Log.d("hand1", handCards[1].toString());
				Log.d("hand2", handCards[2].toString());
				Log.d("hand3", handCards[3].toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (int i = 100; i < 108; ++i) {
			dipai.add(deck.get(i));
		}
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void setZhuCardAndComparator() {
		Suit s = null;
		if (suit > 3) {
			s = null;
		}else {
			s = Suit.values()[suit];
		}
		Rank r = Rank.values()[ranks[zhuangjia % 2]];
		zhuCard = new Card(s, r);
		cardComparator.setZhu(zhuCard);
		codeComparator.setCardComparator(cardComparator);
		ruler.setZhu(zhuCard);
		ruler.setCom(codeComparator);
	}

	protected void sendCardsTo(int i, ArrayList<Byte> cards) {
        Log.d("send cards", cards.toString());
		for (int j = 0; j < cards.size(); ++j) {
			try {
				send(cards.get(j), i);
				handCards[i].add(cards.get(j));
				Thread.sleep(100);
			}catch (Exception e) {}
		}
	}
	
	protected void sendEachCards(ArrayList<Byte> cards) {
        sendEachClient((byte) (CodeUtil.PLAYCOUNT | cards.size()));
        try{
            Thread.sleep(100);
        }catch (Exception e){}
        for (byte card : cards){
            sendEachClient(card);
            try{
                Thread.sleep(50);
            }catch (Exception e){}
        }
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

	private enum GAMESTATE {
		DEAL, MAIPAI, PLAY
	}

}
