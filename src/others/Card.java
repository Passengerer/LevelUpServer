package others;

import java.io.Serializable;

public class Card implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Suit suit;
	private Rank rank;
	
	public Card() {}
	
	public Card(Suit suit, Rank rank) {
		this.suit = suit;
		this.rank = rank;
	}
	
	public void setSuit(Suit suit) {
		this.suit = suit;
	}
	
	public Suit getSuit() {
		return this.suit;
	}
	
	public void setRank(Rank rank) {
		this.rank = rank;
	}
	
	public Rank getRank() {
		return this.rank;
	}
	
	@Override
	public String toString() {
		if (suit == null)
			return new String(rank.toString());
		else
			return new String(suit.toString() + "_" + rank.toString());
	}
	
}
