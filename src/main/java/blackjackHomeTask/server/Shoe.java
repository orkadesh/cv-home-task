package blackjackHomeTask.server;

import java.util.LinkedList;

/**
 * Created by Gene on 7/31/2015.
 */
class Shoe extends Deck {

    private final int decksCount;

    public Shoe(int decksCount, boolean shuffle) {
        this.decksCount = decksCount;
        cards = new LinkedList<Card>();
        for (int deckInd = 0; deckInd < decksCount; deckInd++) {
            for (int suitInd = 0; suitInd < 4; suitInd++) {
                for (int cardOrdinalind = 0; cardOrdinalind < Card.values().length; cardOrdinalind++) {
                    cards.add(Card.values()[cardOrdinalind]);
                }
            }
        }
        if(shuffle) shuffle();
    }

    public Shoe() {
        this(6, true);
    }

    public int getDecksCount() {
        return decksCount;
    }

}
