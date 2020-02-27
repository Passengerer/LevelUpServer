package utils;

import com.laowuren.levelup.others.Card;
import com.laowuren.levelup.others.Rank;
import com.laowuren.levelup.others.Suit;

/**
 * Created by Administrator on 2020/1/28/028.
 */

public class CodeUtil {

    public final static byte CREATE = (byte)0xf0;
    public final static byte READY = (byte)0xf1;
    public final static byte FAILED1 = (byte)0xf2;	// 无空余房间
    public final static byte FAILED2 = (byte)0xf3;	// 房间人数已满
    public final static byte FAILED3 = (byte)0xf4;	// 错误请求
    public final static byte SUCCESS = (byte)0xf5;	// 请求成功
    public final static byte EXIT = (byte)0xfe;
    public final static byte BUFAN = (byte)0xf7;
    public final static byte SHUAIFAIL = (byte)0xf8;
    public final static byte HEARTBEAT = (byte)0xf9;	// 发送心跳信息
    public final static byte GAMEOVER = (byte)0xfa;	// 游戏结束
    public final static byte GAMESTART = (byte)0xfb;	// 游戏开始
    
    public final static byte ROOMID = (byte)0x60;		// 房间id 0110 00 00 后4位中前两位表示房间号，后两位表示玩家id, 初始化阶段表示庄家id
    public final static byte ZHUSUIT = (byte)0xd0;	//  主牌花色 1101 0000 后4位表示花色，4-小王，5-大王
    public final static byte FANSUIT = (byte)0xc0;	//  反牌花色 1100 0000 后4位表示花色，4-小王，5-大王，初始化阶段表示当前等级, 打牌阶段用来表示得分，一次最多传75分，超过需传多次
    public final static byte DINGSUIT = (byte)0xb0;	//  定牌花色 1011 0000 后4位表示花色
    public final static byte FANWANG = (byte)0xa0;	//  用王反 1010 0000 后4位表示大小王，0-小王，1大王
    public final static byte BIZHUANG = (byte)0x90;	// 	逼庄
    public final static byte MAIPAITURN = (byte)0x80;	// 	开始轮流埋牌
    public final static byte PLAYTURN = (byte)0x70;	// 	开始轮流打牌
    public final static byte PLAYCOUNT = (byte)0xe0;	// 	1110 0000
    public final static byte FIRSTPLAY = (byte)0x50; //  第一个出牌者

    public static byte getHeader(byte b){
        return (byte)(b & 0xf0);
    }

    public static byte getTail(byte b){
        return (byte)(b & 0x0f);
    }
    
    public static byte getHigher(byte b){
        return (byte)((b & 0xf0) >> 4);
    }
    
    public static Byte getCodeFromCard(Card card) {
    	byte code = -1;
    	if (card.getRank() == Rank.Joker_red) {
    		code = 0x4d;
    	}
    	else if (card.getRank() == Rank.Joker_black) {
    		code = 0x4e;
    	}
    	else {
    		byte header = -1;
    		byte tail = -1;
    		switch (card.getSuit()) {
			case Heart:
				header = 0;
				break;
			case Club:
				header = 1;
				break;
			case Diamond:
				header = 2;
				break;
			case Spade:
				header = 3;
				break;
			default:
				break;
			}
    		switch (card.getRank()) {
			case Three:
				tail = 0;
				break;
			case Four:
				tail = 1;
				break;
			case Five:
				tail = 2;
				break;
			case Six:
				tail = 3;
				break;
			case Seven:
				tail = 4;
				break;
			case Eight:
				tail = 5;
				break;
			case Nine:
				tail = 6;
				break;
			case Ten:
				tail = 7;
				break;
			case Jack:
				tail = 8;
				break;
			case Queen:
				tail = 9;
				break;
			case King:
				tail = 10;
				break;
			case Ace:
				tail = 11;
				break;
			case Deuce:
				tail = 12;
				break;
			}
    		code = (byte)(header << 4 | tail);
    	}
    	return code;
    }
    
    public static Card getCardFromCode(byte code){
        Card card;
        if (code == 0x4d) {
            card = new Card(null, Rank.Joker_black);
        }
        else if (code == 0x4e) {
            card = new Card(null, Rank.Joker_red);
        }
        else {
            Suit suit = null;
            Rank rank = null;
            switch(getHigher((code))){
                case 0:
                    suit = Suit.Heart;
                    break;
                case 1:
                    suit = Suit.Club;
                    break;
                case 2:
                    suit = Suit.Diamond;
                    break;
                case 3:
                    suit = Suit.Spade;
                    break;
            }
            switch(getTail((code))){
                case 0:
                    rank = Rank.Three;
                    break;
                case 1:
                    rank = Rank.Four;
                    break;
                case 2:
                    rank = Rank.Five;
                    break;
                case 3:
                    rank = Rank.Six;
                    break;
                case 4:
                    rank = Rank.Seven;
                    break;
                case 5:
                    rank = Rank.Eight;
                    break;
                case 6:
                    rank = Rank.Nine;
                    break;
                case 7:
                    rank = Rank.Ten;
                    break;
                case 8:
                    rank = Rank.Jack;
                    break;
                case 9:
                    rank = Rank.Queen;
                    break;
                case 10:
                    rank = Rank.King;
                    break;
                case 11:
                    rank = Rank.Ace;
                    break;
                case 12:
                    rank = Rank.Deuce;
                    break;
            }
            card = new Card(suit, rank);
        }
        return card;
    }

}
