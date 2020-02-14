package com.laowuren.levelup.others;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import utils.CodeUtil;

public class CompareUtil {
	
	private PlayRuler ruler;
	
	public CompareUtil() {}
	
	public void setRuler(PlayRuler ruler) {
		this.ruler = ruler;
	}
	
	public int compareCards(ArrayList<Byte> cards0, ArrayList<Byte> cards1, 
			ArrayList<Byte> cards2, ArrayList<Byte> cards3) {
		// 判断该轮牌谁大
		Log.d("0", cards0.toString());
		Log.d("1", cards1.toString());
		Log.d("2", cards2.toString());
		Log.d("3", cards3.toString());
		int maxId = 0;
		ArrayList<Byte> max = cards0;
		if (checkValid(cards0, cards1)) {
			Log.d("cards0", cards0.toString());
			Log.d("cards1", cards1.toString());
			if (!isGreater(cards0, cards1)) {
				maxId = 1;
				max = cards1;
			}
		}
		if (checkValid(cards0, cards2)) {
			Log.d("cards0", cards0.toString());
			Log.d("max", max.toString());
			if (!isGreater(max, cards2)) {
				maxId = 2;
				max = cards2;
			}
		}
		if (checkValid(cards0, cards3)) {
			Log.d("cards3", cards3.toString());
			Log.d("max", max.toString());
			if (!isGreater(max, cards3)) {
				maxId = 3;
				max = cards3;
			}
		}
		Log.d("maxId", "" + maxId);
		return maxId;
	}
	
	/**
	 * 
	 * @param c1
	 * @param c2
	 * @return 返回c1是否大于等于c2
	 */
	protected boolean isGreater(ArrayList<Byte> c1, ArrayList<Byte> c2) {
		Log.d("c1", c1.toString());
		Log.d("c2", c2.toString());
		Suit c1Suit = CodeUtil.getCardFromCode(c1.get(0)).getSuit();
		Suit c2Suit = CodeUtil.getCardFromCode(c2.get(0)).getSuit();
		LinkedHashMap<Byte, Integer> firstLiandui = CardsParser.getLiandui(c1, ruler.getZhu());
		if (!firstLiandui.isEmpty()) {
			int firstMaxLength = 0;
			byte firstLianDuiByte = -1;
			for (byte b : firstLiandui.keySet()) {
				if (firstLiandui.get(b) > firstMaxLength) {
					firstLianDuiByte = b;
				}
			}
			LinkedHashMap<Byte, Integer> othersLiandui = CardsParser.getLiandui(c2, ruler.getZhu());
			int othersMaxLength = 0;
			byte otherLianDuiByte = -1;
			for (byte b : othersLiandui.keySet()) {
				if (othersLiandui.get(b) > othersMaxLength) {
					otherLianDuiByte = b;
				}
			}
			if (c1Suit != null && c2Suit == null) {
				return false;
			}else {
				// 该方法返回第二个参数 - 第一个参数
				int compare = ruler.getCom().compare(otherLianDuiByte, firstLianDuiByte);
				return compare >= 0;
			}
		}
		ArrayList<Byte> c1Dui = CardsParser.getDui(c1);
		if (!c1Dui.isEmpty()) {
			ArrayList<Byte> c2Dui = CardsParser.getDui(c2);
			for (int i = 0; i < c1Dui.size(); ++i) {
				if (ruler.getCom().compare(c2Dui.get(i), c1Dui.get(i)) != 0) {
					return ruler.getCom().compare(c2Dui.get(i), c1Dui.get(i)) >= 0;
				}
			}
		}
		// 前面都未比较出结果，比较剩下的单牌
		ArrayList<Byte> c1Dan = CardsParser.getDan(c1);
		if (!c1Dan.isEmpty()) {
			ArrayList<Byte> c2Dan = CardsParser.getDan(c2);
			for (int i = 0; i < c1Dan.size(); ++i) {
				if (ruler.getCom().compare(c2Dan.get(i), c1Dan.get(i)) != 0) {
					return ruler.getCom().compare(c2Dan.get(i), c1Dan.get(i)) >= 0;
				}
			}
		}
		// c1等于c2，返回true
		return true;
	}
	
	// 判断牌型是否符合首家牌型
	protected boolean checkValid(ArrayList<Byte> first, ArrayList<Byte> others) {
		// 1.花色统一，都是首家花色或都是主
		Suit firstSuit = ruler.getSuit(CodeUtil.getCardFromCode(first.get(0)));
		Suit othersSuit = ruler.getSuit(CodeUtil.getCardFromCode(others.get(0)));
		if (!ruler.checkSuit(others) || (othersSuit != firstSuit && othersSuit != null)) {
			return false;
		}
		// 2.首家有连对，跟牌也应有相应数量的连对
		LinkedHashMap<Byte, Integer> firstLiandui = CardsParser.getLiandui(first, ruler.getZhu());
		if (!firstLiandui.isEmpty()) {
			int firstMaxLength = 0;
			int firstLianDuiCount = 0;
			for (int length : firstLiandui.values()) {
				firstMaxLength = length > firstMaxLength ? length : firstMaxLength;
				firstLianDuiCount += length;
			}
			LinkedHashMap<Byte, Integer> othersLiandui = CardsParser.getLiandui(others, ruler.getZhu());
			if (othersLiandui.isEmpty()) {
				return false;
			}else {
				int othersMaxLength = 0;
				int othersLianDuiCount = 0;
				for (int length : firstLiandui.values()) {
					othersMaxLength = length > othersMaxLength ? length : othersMaxLength;
					othersLianDuiCount += length;
				}
				if (othersMaxLength < firstMaxLength || othersLianDuiCount < firstLianDuiCount) {
					return false;
				}
			}
		}
		// 3.首家有对子，跟牌也应有相应数量的对子
		ArrayList<Byte> firstDui = CardsParser.getDui(first);
		if (!firstDui.isEmpty()) {
			ArrayList<Byte> othersDui = CardsParser.getDui(others);
			if (othersDui.size() < firstDui.size()) {
				return false;
			}
		}
		return true;
	}

}
