package blackjack.cards;

import java.util.LinkedList;

/**
 * Represents special device called shoe, which holds several card decks inside it.
 * In fact, it's just a card deck with bigger cards capacity.
 * @author yevhen bilous
 */
public class Shoe extends Deck {

    private final int decksCount;

    /**
     * Allows to create shoe with as many cards in it, as needed.
     *
     * @param decksCount number of decks on shoe
     * @param shuffle    shuffle of not to shuffle initial card set
     */
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
        if (shuffle) shuffle();
    }

    /**
     * Basic constructor creates shoe which contains 6 decks (standard decision in casino) and shuffles cards.
     */
    public Shoe() {
        this(6, true);
    }

    public int getDecksCount() {
        return decksCount;
    }

}
