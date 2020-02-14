package com.laowuren.levelup.others;

import java.util.Comparator;

import utils.CodeUtil;

public class CodeComparator implements Comparator<Byte> {
	
	private CardComparator com;
	
	public CodeComparator() {}
	
	public void setCardComparator(CardComparator com) {
		this.com = com;
	}

	/**
	 * 返回o2 - o1
	 */
	@Override
	public int compare(Byte o1, Byte o2) {
		Card c1 = CodeUtil.getCardFromCode(o1);
		Card c2 = CodeUtil.getCardFromCode(o2);
		return com.compare(c2, c1);
	}

}
