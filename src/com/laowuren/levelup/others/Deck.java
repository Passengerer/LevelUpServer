package com.laowuren.levelup.others;

public class Deck {

	private Cards deck = new Cards();
	
	public Deck() {
		for (int i = 0; i < 4; ++i) {
			for (int j = 0; j < 13; ++j) {
				Card card = new Card(Suit.values()[i], Rank.values()[j]);
				deck.cards.add(card);
			}
		}
		deck.cards.add(new Card(null, Rank.Joker_black));
		deck.cards.add(new Card(null, Rank.Joker_red));
	}
	
	public Cards getDeck() {
		return deck;
	}
	
}
