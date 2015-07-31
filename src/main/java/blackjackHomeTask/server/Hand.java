package blackjackHomeTask.server;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Gene on 7/31/2015.
 */
public class Hand {

    public static final int UPPER_HIT = 17;
    public static final int MAX_SCORES = 21;

    protected List<Card> cards;

    public Hand(Card... initialCards) {
        cards = new LinkedList<Card>();
        for (Card card : initialCards) {
            cards.add(card);
        }
    }

    public void receiveCard(Card card) {
        cards.add(card);
    }

    public void retrieveCardFromDeck(Deck deck) {
        cards.add(deck.retrieveUpperCard());
    }

    public void retrieveCardFromDeck(Deck deck, int count) {
        for(int times = 0; times<count; times++) {
            cards.add(deck.retrieveUpperCard());
        }
    }

    public boolean isBlackjack() {
        if (cards.size() == 2 && cards.contains(Card.ACE) && cards.contains(Card.TEN))
            return true;
        else return false;
    }

    public boolean reachesDealerHit() {
        List<Integer> cardsScores = getCardsScores();
        if(cardsScores.size()==1) {
            if(cardsScores.get(0)<UPPER_HIT) return false;
        }
        else {
            if(cardsScores.get(0)<UPPER_HIT && cardsScores.get(1)<UPPER_HIT) return false;
        }
        return true;
    }

    public List<Integer> getCardsScores() {
        List<Integer> sums = new LinkedList<Integer>();
        int noAcesSum = 0;
        int acesCount = 0;
        for (Card card : cards) {
            if(card!=Card.ACE) noAcesSum += cardValue(card);
            else acesCount++;
        }
        if (acesCount == 0) {
            sums.add(noAcesSum);
            return sums;
        } else {
            int acesAreOnesSum = noAcesSum + acesCount;
            sums.add(acesAreOnesSum);
            int bigAceSum = acesAreOnesSum;
            if (acesAreOnesSum + 10 <= MAX_SCORES)
                bigAceSum += 10;
            else
                bigAceSum += acesCount * 10;
            sums.add(bigAceSum);
            return sums;
        }
    }

    public String getCardsScoresString() {
        List<Integer> cardsScores = getCardsScores();
        if(cardsScores.size()==1) return cardsScores.get(0).toString();
        else return cardsScores.get(0) + "/" + cardsScores.get(1);
    }

    public int cardValue(Card card) {
        if (card == Card.ACE) return 1;
        if (card == Card.JACK || card == Card.QUEEN || card == Card.KING) {
            return 10;
        } else return card.ordinal();
    }

    public void clear() {
        cards.clear();
    }

    public boolean isBusted() {
        return (getCardsScores().get(0)>21);
    }

    public int getMaxScore() {
        List<Integer> scores = getCardsScores();
        if(scores.size()==1) return scores.get(0);
        else {
            if(scores.get(1)>21) return scores.get(0);
            else return Math.max(scores.get(0), scores.get(0));
        }
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for(Card card: cards) {
            result.append(card);
            result.append(" ");
        }
        result.append("[");
        result.append(getCardsScoresString());
        result.append("]");
        return result.toString();
    }



}
