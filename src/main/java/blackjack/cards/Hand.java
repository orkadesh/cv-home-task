package blackjack.cards;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents abstraction of player's "hand", where playe could be dealer or actual player
 * in casino. Hand holds some amount of cards and provide methods to determine game situations, such
 * as blackjack, bust etc.
 * @author yevhen bilous
 */
public class Hand {

    public static final int UPPER_HIT = 17;
    public static final int MAX_SCORES = 21;

    protected List<Card> cards;

    public Hand(Card... initialCards) {
        cards = new LinkedList<Card>();
        cards.addAll(Arrays.asList(initialCards));
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
        return cards.size() == 2 && cards.contains(Card.ACE) && cards.contains(Card.TEN);
    }

    /**
     * Determines if dealer hit it's upper scores limit. In most games limit is set to 17,
     * in class it's hold by {@link Hand#MAX_SCORES} final variable. This method doesn't return
     * true if dealer reaches soft hit in hand (which means that aces are in fact counted as 1 score).
     * @return true, if dealer reaches hard limit, and false otherwise
     */
    public boolean reachesDealerHit() {
        List<Integer> cardsScores = getCardsScores();
        if(cardsScores.size()==1) {
            if(cardsScores.get(0)<UPPER_HIT) return false;
        }
        else {
            if(cardsScores.get(0)<UPPER_HIT || cardsScores.get(1)<UPPER_HIT) return false;
        }
        return true;
    }

    /**
     * Calculates number of scores which cards in hand represent. Numerical cards has value described
     * by their number, "figures" count as 10, aces could be 1 or 11. Aces are reason why method returns
     * list instead of just one integer value. If there is one ace, it's counted as 1 and 11; if there
     * are several aces, firstly they all are counted as 1, then only one as 11 - if scores are less than 21,
     * and all as 11 - if scores are bigger than 21.
     * @return one-element list if there are no aces in hand; two-element list otherwise
     */
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

    /**
     * Convert not convenient representation of scores as list to string value.
     * @return string, which contains one or two values of hand's scores
     */
    public String getCardsScoresString() {
        List<Integer> cardsScores = getCardsScores();
        if(cardsScores.size()==1) return cardsScores.get(0).toString();
        else return cardsScores.get(0) + "/" + cardsScores.get(1);
    }

    /**
     * Returns card value. Aces are counted as 1 - in methods, where they must be considered
     * as having two values, this method is not used directly.
     */
    public int cardValue(Card card) {
        if (card == Card.ACE) return 1;
        if (card == Card.JACK || card == Card.QUEEN || card == Card.KING) {
            return 10;
        } else return card.ordinal();
    }

    /**
     * Set cards amount in hand to zero.
     */
    public void clear() {
        cards.clear();
    }

    /**
     * Checks if player busted - hit hard limit of 21 score (hard limit means all aces alternative scores
     * combinations are bigger than specified limit).
     * @return true, if hand's scores are bigger than 21 (if there are aces, they are counted as 1)
     */
    public boolean isBusted() {
        return (getCardsScores().get(0)>21);
    }

    /**
     * Checks if hand holds aces, and returns maxximum limit, which is not bigger than bust limit.
     * @return hand's scores, if there any aces in hand - combination with maximum value under limit
     */
    public int getMaxScore() {
        List<Integer> scores = getCardsScores();
        if(scores.size()==1) return scores.get(0);
        else {
            if(scores.get(1)>21) return scores.get(0);
            else return Math.max(scores.get(0), scores.get(0));
        }
    }

    /**
     * Builds string which contains cards values and scores in square brackets
     * @return string representation of cards in hand: cards and scores
     */
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
