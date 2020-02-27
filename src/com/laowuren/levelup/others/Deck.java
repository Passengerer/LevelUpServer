package com.laowuren.levelup.others;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import utils.CodeUtil;

public class Deck {

	private List<Byte> deck = new ArrayList<>();

	public Deck() {
		int temp = 0;
		for (int t = 0; t < 2; ++t) {
			for (int i = 0; i < 4; ++i) {
				for (int j = 0; j < 13; ++j) {
					temp = (byte) (i << 4) | (byte) j;
					deck.add((byte) temp);
				}
			}
			deck.add((byte) 0x4d); // Joker_black
			deck.add((byte) 0x4e); // Joker_red
		}
		shuffle();
	}

	public byte get(int index) {
		return deck.get(index);
	}
	
	public void shuffle() {
		/*List<Byte> newDeck = new ArrayList<>();
		int index;
		int amount = deck.size();
		Random ran = new Random();
		for (int i = 0; i < amount; ++i) {
			index = ran.nextInt(deck.size());
			newDeck.add(deck.get(index));
			deck.remove(index);
		}
		deck = newDeck;*/
		Collections.shuffle(deck);
		List<Byte> newDeck = new ArrayList<>();
		for (int i = 60; i < deck.size(); ++i) {
			newDeck.add(deck.get(i));
		}
		for (int i = 30; i < 60; ++i) {
			newDeck.add(deck.get(i));
		}
		for (int i = 0; i < 30; ++i) {
			newDeck.add(deck.get(i));
		}
		deck = newDeck;
	}
	
	public void printAll() {
		for (byte b : deck) {
			System.out.println(CodeUtil.getCardFromCode(b).toString());
		}
	}

}
