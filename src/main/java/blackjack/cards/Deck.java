package blackjack.cards;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents standard 52-card deck. Doesn't have internal shuffle mechanism, so it must
 * be done by {@link Deck#shuffle()} manually
 * @author yevhen bilous
 */

public class Deck {

    protected List<Card> cards;

    /**
     * Basic constructor creates standard 52-card deck. Because of using {@link Card}, there is no difference
     * between card suits (i.e. that, for example, all aces are equivalent)
     */
    public Deck() {
        cards = new LinkedList<Card>();
        for (int suitInd = 0; suitInd < 4; suitInd++) {
            for (int cardOrdinalInd = 0; cardOrdinalInd < Card.values().length; cardOrdinalInd++) {
                cards.add(Card.values()[cardOrdinalInd]);
            }
        }
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Card card : cards) {
            result.append(card);
            result.append(" ");
        }
        result.append('\n');
        return result.toString();
    }

    /**
     * Shuffles cards in deck.
     */
    public void shuffle() {
        Collections.shuffle(cards);
    }

    /**
     * Retrieves card from deck. Must be careful - deck doesn't hold retrieved cards, so
     * each retrieved card is removed permanently.
     *
     * @return uuper card in deck
     */
    public Card retrieveUpperCard() {
        if (cards.isEmpty()) return null;
        return cards.remove(cards.size() - 1);
    }

    public int size() {
        return cards.size();
    }
}

