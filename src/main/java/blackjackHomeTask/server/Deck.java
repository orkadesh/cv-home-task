package blackjackHomeTask.server;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

class Deck {

    protected List<Card> cards;

    public Deck() {
        cards = new LinkedList<Card>();
        for (int suitInd = 0; suitInd < 4; suitInd++) {
            for (int cardOrdinalInd = 0; cardOrdinalInd < Card.values().length; cardOrdinalInd++) {
                cards.add(Card.values()[cardOrdinalInd]);
            }
        }
    }

    public String toString() {
        StringBuilder result = new StringBuilder("Deck contains: ");
        for (Card card : cards) {
            result.append(card + " ");
        }
        result.append('\n');
        return result.toString();
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card retrieveUpperCard() {
        if(cards.isEmpty()) return null;
        return cards.remove(cards.size() - 1);
    }

    public int size() {
        return cards.size();
    }
}

