package com.laowuren.levelup.others;

public class Deck {

	private Cards deck = new Cards();
	
	public Deck() {
		for (int i = 0; i < 4; ++i) {
			for (int j = 0; j < 13; ++j) {
				Card card = new Card(Suit.values()[i], Rank.values()[j]);
				deck.add(card);
			}
		}
		deck.add(new Card(null, Rank.Joker_black));
		deck.add(new Card(null, Rank.Joker_red));
	}
	
	public Cards getDeck() {
		return deck;
	}
	
}
