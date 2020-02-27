package com.laowuren.levelup.others;

import java.io.IOException;
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
	
	private int[] sendErrorTimes;
	
	private boolean DESTROY = false;
	private boolean EXIT = false;
	private Thread playThread;
	private GAMESTATE stat = GAMESTATE.DEAL;
	private CardComparator cardComparator = new CardComparator();
	private CodeComparator codeComparator = new CodeComparator();
	private CompareUtil compareUtil = new CompareUtil();
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
	private int chupaiCount = 0;
	private int playCardsCount;
	private int score = 0;

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
		sendErrorTimes = new int[4];
	}

	public void destroy() {
		if (DESTROY) {
			return;
		}
		if (!DESTROY) {
			DESTROY = true;
		}
		sendEachClient(CodeUtil.EXIT);
		for (int i = 0; i < 4; ++i) {
			try {
				Thread.sleep(2000);
				if (!sockets[i].isClosed()) {
					sockets[i].close();
					sockets[i] = null; 
				}
				if (ins[i] != null) {
					ins[i].close();
					ins[i] = null; 
				}
				if (outs[i] != null) {
					outs[i].close();
					outs[i]= null; 
				}
			} catch (Exception e) {
			}
		}
		playThread.interrupt();
		Log.d(TAG + " " + id, "destroy");
		GameServer.Rooms.remove(id);
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
				Log.d("zhuang", "" + zhuangjia);
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
					sendCardsTo(playerId, dipai, true);
					dipai.clear();
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
				sendEachClient((byte) (CodeUtil.DINGSUIT | ((byte) suit << 2) | ((byte) playerId)));
			}else {
				sendEachClient((byte) (CodeUtil.FANSUIT | ((byte) suit << 2) | ((byte) playerId)));
			}
			Log.d("suit", "" + suit);
			if (stat == GAMESTATE.MAIPAI) {
				maipairenID = playerId;
				sendEachClient((byte) (CodeUtil.MAIPAITURN | (maipairenID << 2 | turn)));
				sendCardsTo(playerId, dipai, true);
				dipai.clear();
			}
			return;
		}
		if (stat == GAMESTATE.PLAY && (code & 0xe0) == 0xe0) {
			++chupaiCount;
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
				sendEachCards(playCards[playerId]);
				if (playerId == firstPlayerId && ruler.getType(playCards[playerId]) == PlayRuler.SHUAI) {
					Log.d("shuaipai", "true");
					handCards[(playerId + 1) % 4].sort(codeComparator);
					handCards[(playerId + 2) % 4].sort(codeComparator);
					handCards[(playerId + 3) % 4].sort(codeComparator);
					ArrayList<Byte> tuihuiCards = ruler.checkShuai(playCards[playerId], handCards[(playerId + 1) % 4],
							handCards[(playerId + 2) % 4], handCards[(playerId + 3) % 4]);
					if (tuihuiCards == null) {
						// 甩牌成功
						Log.d("shuai pai", "success");
						removeCards(handCards[playerId], playCards[playerId]);
						synchronized (playLock) {
							playLock.notify();
						}
					} else {
						// 甩牌失败,等待几秒,退回部分牌
						Log.d("shuai pai", "failed");
						try {
							Thread.sleep(4000);
						} catch (Exception e) {
						}
						sendEachClient(CodeUtil.SHUAIFAIL);
						for (byte b : tuihuiCards) {
							playCards[playerId].remove((Object) b);
						}
						sendEachCards(playCards[playerId]);
						sendCardsTo(playerId, tuihuiCards, false);
						removeCards(handCards[playerId], playCards[playerId]);
						synchronized (playLock) {
							playLock.notify();
						}
					}
				} else {
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
	
	protected void clearHandCards() {
		for (int i = 0; i < handCards.length; ++i) {
			handCards[i].clear();
		}
	}
	
	protected void onGameInit() {
		// 初始化各种变量
		clearHandCards();
		sendEachClient(CodeUtil.GAMESTART);
		suit = 0x10;
		zhuCard = null;
		hasSetZhu = false;
		hasDing = false;
		hasFanWang = false;
		reDeal = false;
		noFan = false;
		isBizhuang = false;
		maipairenID = -1;
		showSuitPlayer = -1;
		bufanCount = 0;
		chupaiCount = 0;
		playCardsCount = 0;
		score = 0;
		stat = GAMESTATE.DEAL;
		canNotify = false;
		dipai.clear();
		if (isFirstGame) {
			sendEachClient((byte)(CodeUtil.FANSUIT | ranks[0]));
		}else {
			sendEachClient((byte)(CodeUtil.ROOMID | zhuangjia));
			sendEachClient((byte)(CodeUtil.FANSUIT | ranks[zhuangjia % 2]));
		}
		deck.shuffle();
		deck.shuffle();
	}

	protected void play() {
		playThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!EXIT) {
					onGameInit();
					if (isFirstGame) {
						deal(0);
						sendEachClient((byte)(CodeUtil.ROOMID | zhuangjia));
					} else {
						deal(zhuangjia);
					}
					Log.d("send zhuang", "" + zhuangjia);
					sendEachClient(CodeUtil.SUCCESS);
					if (!hasSetZhu) {
						if (!isFirstGame) {
							isBizhuang = true;
							sendEachClient((byte) (CodeUtil.BIZHUANG | zhuangjia));
							synchronized (bizhuangLock) {
								try {
									bizhuangLock.wait();
									if (noFan) {
										reDeal = true;
									} else {
										reDeal = false;
									}
								} catch (InterruptedException e) {
									EXIT = true;
									break;
								}
							}
						} else {
							reDeal = true;
						}
					}
					if (reDeal) {
						Log.d("redeal", "redeal");
						continue;
					}
					// 开始轮流埋牌
					turn = zhuangjia;
					maipairenID = zhuangjia;
					Log.d("turn " + turn, "" + (byte) (CodeUtil.MAIPAITURN | (maipairenID << 2 | turn)));
					sendEachClient((byte) (CodeUtil.MAIPAITURN | (maipairenID << 2 | turn)));
					stat = GAMESTATE.MAIPAI;
					sendCardsTo(zhuangjia, dipai, true);
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
							} catch (InterruptedException e) {
								EXIT = true;
								break;
							}
						}
					}
					if (EXIT) {
						break;
					}
					setZhuCardAndComparator();
					// 开始轮流打牌
					turn = zhuangjia;
					firstPlayerId = turn;
					sendEachClient((byte) (CodeUtil.FIRSTPLAY | turn));
					sendEachClient((byte) (CodeUtil.PLAYTURN | turn));
					stat = GAMESTATE.PLAY;
					synchronized (playLock) {
						while (!EXIT) {
							try {
								playLock.wait();
								turn = (turn + 1) % 4;
								if (chupaiCount == 4) {
									chupaiCount = 0;
									handlePlayCards();
									Thread.sleep(2500);
									Log.d("hand0", handCards[0].toString());
									if (handCards[0].isEmpty()) {
										// 玩家没有手牌了，该局牌结束
										break;
									}
									sendEachClient((byte) (CodeUtil.FIRSTPLAY | turn));
									sendEachClient((byte) (CodeUtil.PLAYTURN | turn));
								} else {
									Log.d("next turn", "" + turn);
									sendEachClient((byte) (CodeUtil.PLAYTURN | turn));
								}
							} catch (InterruptedException ie) {
								EXIT = true;
								break;
							}
						}
					}
					onGameEnd();
					try {
						Thread.sleep(3000);
					}catch (Exception e) {}
					Log.d(TAG, "game over");
				}
				Log.d(TAG, "thread over");
			}
		});
		playThread.start();
	}

	protected void onGameEnd() {
		sendEachClient(CodeUtil.GAMEOVER);
		for (int i = 0; i < 4; ++i) {
			sendCardsTo(i, dipai, false);
		}
		isFirstGame = false;
		// 计算得分、庄家、等级等信息
		if (score < 80) {
			zhuangjia = (zhuangjia + 2) % 4;
			if (score == 0) {
				ranks[zhuangjia % 2] += 3;
			} else if (score < 40) {
				ranks[zhuangjia % 2] += 2;
			} else {
				ranks[zhuangjia % 2] += 1;
			}
			if (ranks[zhuangjia % 2] > Rank.Ace.ordinal()) {
				ranks[zhuangjia % 2] %= (Rank.Ace.ordinal() + 1);
			}
		} else {
			zhuangjia = (zhuangjia + 1) % 4;
			if (score >= 160) {
				ranks[zhuangjia % 2] += 2;
			} else if (score >= 120) {
				ranks[zhuangjia % 2] += 1;
			}
			if (ranks[zhuangjia % 2] > Rank.Ace.ordinal()) {
				ranks[zhuangjia % 2] %= (Rank.Ace.ordinal() + 1);
			}
		}
		Log.d("final score", "" + score);
		Log.d("new zhuang", "" + zhuangjia);
	}

	protected void handlePlayCards() {
		int max = compareUtil.compareCards(playCards[firstPlayerId], playCards[(firstPlayerId + 1) % 4],
				playCards[(firstPlayerId + 2) % 4], playCards[(firstPlayerId + 3) % 4]);
		Log.d("zhuang", "" + zhuangjia);
		max = (max + firstPlayerId) % 4; // 牌最大者id
		Log.d("max", "" + max);
		if ((max + zhuangjia) % 2 == 1) {
			int turnScore = 0;
			for (int i = 0; i < 4; ++i) {
				for (byte b : playCards[i]) {
					switch (CodeUtil.getCardFromCode(b).getRank()) {
					case Five:
						turnScore += 5;
						break;
					case Ten:
					case King:
						turnScore += 10;
						break;
					default:
					}
				}
			}
			if (handCards[0].isEmpty()) {	// 牌打完了，这是最后一轮
				Card card = CodeUtil.getCardFromCode((playCards[max].get(0)));
				if (ruler.checkYingZhu(card)) {
					int dipaiScore = 0;
					for (byte b : dipai) {
						switch (CodeUtil.getCardFromCode(b).getRank()) {
						case Five:
							dipaiScore += 5;
							break;
						case Ten:
						case King:
							dipaiScore += 10;
							break;
						default:
						}
					}
					ArrayList<Byte> dui = CardsParser.getDui(playCards[max]);
					if (!dui.isEmpty()) {
						Card firstDui = CodeUtil.getCardFromCode(dui.get(0));
						if (ruler.checkYingZhu(firstDui)) {
							dipaiScore *= 2;
						}
					}
					turnScore += dipaiScore;
				}
			}
			Log.d("score", "" + score);
			score += turnScore;
			int shang = turnScore / 75;
			int yushu = turnScore % 75;
			for(int i = 0; i < shang; ++i) {
				sendEachClient((byte) (CodeUtil.FANSUIT | 0x0f));
			}
			sendEachClient((byte) (CodeUtil.FANSUIT | (yushu / 5)));
		}
		turn = firstPlayerId = max;
		for (int i = 0; i < 4; ++i) {
			playCards[i].clear();
		}
	}

	protected void deal(int first) {
		try {
			Thread.sleep(2500);
			for (int i = 0; i < 25; ++i) {

				for (int j = 0; j < 4; ++j) {
					byte code = deck.get(i * 4 + j);
					
					send(code, (j + first) % 4);
					handCards[(j + first) % 4].add(code);
					 
					/*if (i < 1) {
						send((byte) 0x00, j);
						handCards[j].add((byte) 0x10);
					} else if (i < 3) {
						if (j == 0)
						send((byte) 0x11, j);
						else if (i == 1) { 
							send((byte)0x13, j);
						}else send((byte)0x14, j);
						handCards[j].add((byte) 0x00);
					} else if (i < 5) {
						if (j == 0)
						send((byte) 0x12, j);
						else send((byte)0x05, j);
						handCards[j].add((byte) 0x20);
					} else if (i < 7) {
						if (j == 0)
						send((byte) 0x13, j);
						else if (i == 5)
							send((byte)0x4e, j);
						else send((byte)0x24, j);
						handCards[j].add((byte) 0x30);
					} else if (i < 8) {
						if (j == 0)
						send((byte) 0x04, j);
						else send((byte)0x0c, j);
						handCards[j].add((byte) 0x30);
					} else {
						send((byte) 0x26, j);
						handCards[j].add((byte) 0x16);
					}*/
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
			Thread.sleep(4000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void setZhuCardAndComparator() {
		Suit s = null;
		if (suit > 3) {
			s = null;
		} else {
			s = Suit.values()[suit];
		}
		Rank r = Rank.values()[ranks[zhuangjia % 2]];
		zhuCard = new Card(s, r);
		cardComparator.setZhu(zhuCard);
		codeComparator.setCardComparator(cardComparator);
		ruler.setZhu(zhuCard);
		ruler.setCom(codeComparator);
		compareUtil.setRuler(ruler);
	}

	protected void sendCardsTo(int i, ArrayList<Byte> cards, boolean addToHand) {
		Log.d("send cards", cards.toString());
		for (int j = 0; j < cards.size(); ++j) {
			try {
				send(cards.get(j), i);
				if (addToHand) {
					handCards[i].add(cards.get(j));
				}
				Thread.sleep(100);
			} catch (Exception e) {
			}
		}
	}

	protected void sendEachCards(ArrayList<Byte> cards) {
		sendEachClient((byte) (CodeUtil.PLAYCOUNT | cards.size()));
		try {
			Thread.sleep(100);
		} catch (Exception e) {
		}
		for (byte card : cards) {
			sendEachClient(card);
			try {
				Thread.sleep(50);
			} catch (Exception e) {
			}
		}
	}

	protected void sendEachClient(byte instruct) {
		for (int i = 0; i < 4; ++i) {
			try {
				send(instruct, i);
			}catch (Exception e) {}
		}
	}

	public synchronized void send(byte instruct, int i) throws IOException {
		Log.d(TAG, "send " + instruct);
		try {
			outs[i].write(instruct);
			outs[i].flush();
			sendErrorTimes[i] = 0;
		} catch (IOException e) {
			Log.d(TAG, "writing msg exception: " + i);
			++sendErrorTimes[i];
			if (sendErrorTimes[i] >= 3)
				destroy();
		}
	}

	private enum GAMESTATE {
		DEAL, MAIPAI, PLAY
	}

}
