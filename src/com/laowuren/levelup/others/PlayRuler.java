package com.laowuren.levelup.others;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import utils.CodeUtil;

public class PlayRuler {
	
	public final static int DAN = 1;
	public final static int DUI = 2;
	public final static int LIANDUI = 3;
	public final static int SHUAI = 4;
	
	private Card zhu;
	
	private CodeComparator com;
	
	public PlayRuler() {}
	
	public PlayRuler(Card zhu) {
		this.zhu = zhu;
	}
	
	public void setCom(CodeComparator com) {
		this.com = com;
	}
	
	public CodeComparator getCom() {
		return com;
	}
	
	public void setZhu(Card card) {
		this.zhu = card;
	}
	
	public Card getZhu() {
		return zhu;
	}
	
	/**
	 * 
	 * @param card
	 * @return 如果是主，返回null；不是则返回其花色
	 */
	public Suit getSuit(Card card) {
		Log.d("PlayRuler", "getSuit");
		if (card.getSuit() == null) {
			return null;
		}
		if (card.getRank() == zhu.getRank() || card.getRank() == Rank.Deuce) {
			return null;
		}
		if (zhu.getSuit() != null && card.getSuit() == zhu.getSuit()) {
			return null;
		}else {
			Log.d("getSuit", card.getSuit().toString());
			return card.getSuit();
		}
	}
	
	public boolean checkSuit(ArrayList<Byte> cards) {
		Log.d("PlayRuler", "checkSuit");
		cards.sort(com);
		Suit zhuSuit = zhu.getSuit();
		Suit suit = CodeUtil.getCardFromCode(cards.get(0)).getSuit();
		Rank rank = CodeUtil.getCardFromCode(cards.get(0)).getRank();
		if (suit == null || rank == Rank.Deuce || rank == zhu.getRank()) {				
			if (zhuSuit == null) {		// 无小主
				ArrayList<Byte> zhuCards = CardsParser.getZhu(cards, zhu.getRank());
				if (cards.size() == zhuCards.size())
					return true;
				else return false;
			}else {						// 有小主
				ArrayList<Byte> zhuCards = CardsParser.getZhu(cards, zhu.getRank());
				ArrayList<Byte> xiaozhuCards = null;
				switch (zhu.getSuit()) {
				case Heart:
					xiaozhuCards = CardsParser.getHeart(cards, zhu.getRank());
					break;
				case Club:
					xiaozhuCards = CardsParser.getClub(cards, zhu.getRank());
					break;
				case Diamond:
					xiaozhuCards = CardsParser.getDiamond(cards, zhu.getRank());
					break;
				case Spade:
					xiaozhuCards = CardsParser.getSpade(cards, zhu.getRank());
					break;
				}
				if (zhuCards.size() + xiaozhuCards.size() == cards.size())
					return true;
				else return false;
			}
		}else {
			if (suit == Suit.Heart) {
				ArrayList<Byte> heartCards = CardsParser.getHeart(cards, zhu.getRank());
				if (cards.size() == heartCards.size())
					return true;
				else return false;
			}
			if (suit == Suit.Club) {
				ArrayList<Byte> clubCards = CardsParser.getClub(cards, zhu.getRank());
				if (cards.size() == clubCards.size())
					return true;
				else return false;
			}
			if (suit == Suit.Diamond) {
				ArrayList<Byte> diamondCards = CardsParser.getDiamond(cards, zhu.getRank());
				if (cards.size() == diamondCards.size())
					return true;
				else return false;
			}
			if (suit == Suit.Spade) {
				ArrayList<Byte> spadeCards = CardsParser.getSpade(cards, zhu.getRank());
				if (cards.size() == spadeCards.size())
					return true;
				else return false;
			}
			return false;
		}
	}

	public ArrayList<Byte> checkShuai(ArrayList<Byte> cards, ArrayList<Byte> p1,
			ArrayList<Byte> p2, ArrayList<Byte> p3) {
		Log.d("PlayRuler", "checkShuai");
		boolean dan;
		boolean dui;
		boolean liandui;
		ArrayList<Byte> ret = new ArrayList<>();
		Suit cardsSuit = getSuit(CodeUtil.getCardFromCode(cards.get(0)));
		ArrayList<Byte> suitP1 = null;
		ArrayList<Byte> suitP2 = null;
		ArrayList<Byte> suitP3 = null;
		
		ArrayList<Byte>cardsDan = CardsParser.getDan(cards);
		ArrayList<Byte>cardsDui = CardsParser.getDuiExceptLiandui(cards, zhu);
		LinkedHashMap<Byte, Integer> cardsLiandui = CardsParser.getLiandui(cards, zhu);
		if (cardsSuit == null) {
			Log.d("checkShuai", "null");
			suitP1 = CardsParser.getZhu(p1, zhu.getRank());
			suitP2 = CardsParser.getZhu(p2, zhu.getRank());
			suitP3 = CardsParser.getZhu(p3, zhu.getRank());
			
			if (zhu.getSuit() != null) {
				switch (zhu.getSuit()) {
				case Heart:
					suitP1.addAll(CardsParser.getHeart(p1, zhu.getRank()));
					suitP2.addAll(CardsParser.getHeart(p2, zhu.getRank()));
					suitP3.addAll(CardsParser.getHeart(p3, zhu.getRank()));
					break;
				case Club:
					suitP1.addAll(CardsParser.getClub(p1, zhu.getRank()));
					suitP2.addAll(CardsParser.getClub(p2, zhu.getRank()));
					suitP3.addAll(CardsParser.getClub(p3, zhu.getRank()));
					break;
				case Diamond:
					suitP1.addAll(CardsParser.getDiamond(p1, zhu.getRank()));
					suitP2.addAll(CardsParser.getDiamond(p2, zhu.getRank()));
					suitP3.addAll(CardsParser.getDiamond(p3, zhu.getRank()));
					break;
				case Spade:
					suitP1.addAll(CardsParser.getSpade(p1, zhu.getRank()));
					suitP2.addAll(CardsParser.getSpade(p2, zhu.getRank()));
					suitP3.addAll(CardsParser.getSpade(p3, zhu.getRank()));
					break;
				}
			}
		}else if (cardsSuit == Suit.Heart) {
			Log.d("checkShuai", "heart");
			suitP1 = CardsParser.getHeart(p1, zhu.getRank());;
			suitP2 = CardsParser.getHeart(p2, zhu.getRank());;
			suitP3 = CardsParser.getHeart(p3, zhu.getRank());
		}else if (cardsSuit == Suit.Club) {
			Log.d("checkShuai", "club");
			suitP1 = CardsParser.getClub(p1, zhu.getRank());
			suitP2 = CardsParser.getClub(p2, zhu.getRank());
			suitP3 = CardsParser.getClub(p3, zhu.getRank());
		}else if (cardsSuit == Suit.Diamond) {
			Log.d("checkShuai", "diamond");
			suitP1 = CardsParser.getDiamond(p1, zhu.getRank());
			suitP2 = CardsParser.getDiamond(p2, zhu.getRank());
			suitP3 = CardsParser.getDiamond(p3, zhu.getRank());
		}else if (cardsSuit == Suit.Spade) {
			Log.d("checkShuai", "spade");
			suitP1 = CardsParser.getSpade(p1, zhu.getRank());
			suitP2 = CardsParser.getSpade(p2, zhu.getRank());
			suitP3 = CardsParser.getSpade(p3, zhu.getRank());
		}
		if (cardsLiandui.isEmpty()) {
			Log.d("liandui", "empty");
			liandui = true;
		}else {
			// 获取最后添加的即最小的连对
			byte min = -1;
			for (byte b : cardsLiandui.keySet()) {
				min = b;
			}
			
			if (!suitP1.isEmpty()) {
				Log.d("suitP1", "not empty");
				Log.d("suitP1", suitP1.toString());
				LinkedHashMap<Byte, Integer> p1Liandui = CardsParser.getLiandui(suitP1, zhu);
				if (!p1Liandui.isEmpty()) {
					Log.d("p1Liandui", "not empty");
					Iterator<Entry<Byte, Integer>> iterator = p1Liandui.entrySet().iterator();
					Entry<Byte, Integer> entry = null;
					while (iterator.hasNext()) {
						entry = iterator.next();
						// codeCom按从小到大排序，< 0表示前者大于后者
						if (com.compare(entry.getKey(), min) < 0 && 
								entry.getValue() >= cardsLiandui.get(min)) {

							Log.d(">= cardsLiandui", "can't shuaipai");
							ret.addAll(cards);
							int index = ret.indexOf(min);
							ArrayList<Byte> remove = new ArrayList<>();
							for (int i = 0; i < cardsLiandui.get(min); ++i) {
								remove.add(ret.get(index + i * 2));
								remove.add(ret.get(index + i * 2));
							}
							removeCards(ret, remove);
							Log.d("return", ret.toString());
							return ret;
						}
					}
				}
			}
			
			if (!suitP2.isEmpty()) {
				Log.d("suitP2", "not empty");
				Log.d("suitP2", suitP2.toString());
				LinkedHashMap<Byte, Integer> p2Liandui = CardsParser.getLiandui(suitP2, zhu);
				if (!p2Liandui.isEmpty()) {
					Log.d("p2Liandui", "not empty");
					Iterator<Entry<Byte, Integer>> iterator = p2Liandui.entrySet().iterator();
					Entry<Byte, Integer> entry = null;
					while (iterator.hasNext()) {
						entry = iterator.next();
						if (com.compare(entry.getKey(), min) < 0 && 
								entry.getValue() >= cardsLiandui.get(min)) {

							Log.d(">= cardsLiandui", "can't shuaipai");
							ret.addAll(cards);
							int index = ret.indexOf(min);
							ArrayList<Byte> remove = new ArrayList<>();
							for (int i = 0; i < cardsLiandui.get(min); ++i) {
								remove.add(ret.get(index + i * 2));
								remove.add(ret.get(index + i * 2));
							}
							removeCards(ret, remove);
							Log.d("return", ret.toString());
							return ret;
						}
					}
				}
			}
			
			if (!suitP3.isEmpty()) {
				Log.d("suitP3", "not empty");
				Log.d("suitP3", suitP3.toString());
				LinkedHashMap<Byte, Integer> p3Liandui = CardsParser.getLiandui(suitP3, zhu);
				if (!p3Liandui.isEmpty()) {
					Log.d("p3Liandui", "not empty");
					Iterator<Entry<Byte, Integer>> iterator = p3Liandui.entrySet().iterator();
					Entry<Byte, Integer> entry = null;
					while (iterator.hasNext()) {
						entry = iterator.next();
						if (com.compare(entry.getKey(), min) < 0 && 
								entry.getValue() >= cardsLiandui.get(min)) {

							Log.d(">= cardsLiandui", "can't shuaipai");
							ret.addAll(cards);
							int index = ret.indexOf(min);
							ArrayList<Byte> remove = new ArrayList<>();
							for (int i = 0; i < cardsLiandui.get(min); ++i) {
								remove.add(ret.get(index + i * 2));
								remove.add(ret.get(index + i * 2));
							}
							removeCards(ret, remove);
							Log.d("return", ret.toString());
							return ret;
						}
					}
				}
			}
			liandui = true;
		}
		if (cardsDui.isEmpty()) {
			Log.d("dui", "empty");
			dui = true;
		}else {
			byte min = cardsDui.get(cardsDui.size() - 1);
			Log.d("min", "" + min);
			
			if (!suitP1.isEmpty()) {
				Log.d("suitP1", "not empty");
				ArrayList<Byte> p1Dui = CardsParser.getDui(suitP1);
				if (!p1Dui.isEmpty()) {
					Log.d("p1dui", p1Dui.toString());
					if (com.compare(p1Dui.get(0), min) < 0) {
						Log.d(">= cardsDui", "can't shuaipai");
						ret.addAll(cards);
						ArrayList<Byte> remove = new ArrayList<>();
						remove.add(min);
						remove.add(min);
						removeCards(ret, remove);
						Log.d("return", ret.toString());
						return ret;
					}
				}
			}
			
			if (!suitP2.isEmpty()) {
				Log.d("suitP2", "not empty");
				ArrayList<Byte> p2Dui = CardsParser.getDui(suitP2);
				if (!p2Dui.isEmpty()) {
					Log.d("p2dui", p2Dui.toString());
					if (com.compare(p2Dui.get(0), min) < 0) {
						Log.d(">= cardsDui", "can't shuaipai");
						ret.addAll(cards);
						ArrayList<Byte> remove = new ArrayList<>();
						remove.add(min);
						remove.add(min);
						removeCards(ret, remove);
						Log.d("return", ret.toString());
						return ret;
					}
				}
			}
			
			if (!suitP3.isEmpty()) {
				Log.d("suitP3", "not empty");
				ArrayList<Byte> p3Dui = CardsParser.getDui(suitP3);
				if (!p3Dui.isEmpty()) {
					Log.d("p3dui", p3Dui.toString());
					if (com.compare(p3Dui.get(0), min) < 0) {
						Log.d(">= cardsDui", "can't shuaipai");
						ret.addAll(cards);
						ArrayList<Byte> remove = new ArrayList<>();
						remove.add(min);
						remove.add(min);
						removeCards(ret, remove);
						Log.d("return", ret.toString());
						return ret;
					}
				}
			}
			dui = true;
		}
		if (cardsDan.isEmpty()) {
			Log.d("dan", "empty");
			dan = true;
		}else {
			byte min = cardsDan.get(cardsDan.size() - 1);
			Log.d("min", "" + min);
			
			if (!suitP1.isEmpty()) {
				Log.d("suitP1", "not empty");
				// 有大于甩牌中最小牌即可
				if (com.compare(suitP1.get(0), min) < 0) {
					Log.d(">= cardsDui", "can't shuaipai");
					ret.addAll(cards);
					ret.remove(ret.size() - 1);
					return ret;
				}
			}
			
			if (!suitP2.isEmpty()) {
				Log.d("suitP2", "not empty");
				// 有大于甩牌中最小牌即可
				if (com.compare(suitP2.get(0), min) < 0) {
					Log.d(">= cardsDui", "can't shuaipai");
					ret.addAll(cards);
					ret.remove(ret.size() - 1);
					return ret;
				}
			}
			
			if (!suitP3.isEmpty()) {
				Log.d("suitP3", "not empty");
				// 有大于甩牌中最小牌即可
				if (com.compare(suitP3.get(0), min) < 0) {
					Log.d(">= cardsDui", "can't shuaipai");
					ret.addAll(cards);
					ret.remove(ret.size() - 1);
					return ret;
				}
			}
			dan = true;
		}
		return null;
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

    public boolean checkYingZhu(Card card){
        Rank rank = card.getRank();
        if (rank == Rank.Joker_red || rank == Rank.Joker_black ||
                rank == Rank.Deuce || rank == zhu.getRank()){
            return true;
        }else{
            return false;
        }
    }
	
	public int getType(ArrayList<Byte> cards) {
		// 只有1张牌
		if (cards.size() == 1) {
			return DAN;
		}
		if (cards.size() % 2 == 1) {
			return SHUAI;
		}
		ArrayList<Byte> dan = CardsParser.getDan(cards);
		if (dan != null && dan.size() > 1) {
			return SHUAI;
		}
		ArrayList<Byte> dui = CardsParser.getDui(cards);
		// 有对
		if (dui != null) {
			// 只有一对
			if (dui.size() == 1) {
				if (cards.size() == 2)
					return DUI;
				else return SHUAI;
			}else {
				// 不止一对
				dui.sort(com);
				LinkedHashMap<Byte, Integer> liandui = CardsParser.getLiandui(cards, zhu);
				if (liandui == null) {
					return SHUAI;
				}else {
					if (liandui.containsKey(dui.get(0)) && liandui.get(dui.get(0)) == dui.size()) {
						return LIANDUI;
					}else {
						return SHUAI;
					}
				}
			}
		}
		return -1;
	}
}
