package blackjack.server;

import blackjack.client.*;
import blackjack.cards.*;

import java.io.*;
import java.net.Socket;

/**
 * Serve player, whose internet socket was received upon this object creation.
 * Represents blackjack server side logic in single game.
 * @author yevhen bilous
 */
public class SinglePlayerHandler extends AbstractPlayerHandler implements Runnable {

    /**
     * Creates handler with predefined values of minimal bet {@link SinglePlayerHandler#minimalBet}
     * and initial player's money amount {@link SinglePlayerHandler#playerMoney}
     *
     * @param clientSocket socket, associated with connected client
     */
    public SinglePlayerHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * Creates handler with specified values of minimal bet, player's money amount and client socket.
     *
     * @param clientSocket       socket, associated with connected client
     * @param playerMoneyOnStart initial amount of money which player has
     * @param minimalBet         minimal bet on round start
     */
    public SinglePlayerHandler(Socket clientSocket, int playerMoneyOnStart, int minimalBet) {
        this.clientSocket = clientSocket;
        this.minimalBet = minimalBet;
        this.playerMoney = playerMoneyOnStart;
    }

    /**
     * Contains logic of server-client communication and game mechanic (single game).
     */
    public void run() {
        // initializing io streams, player money variable
        int initialPlayerMoney = playerMoney;
        OutputStream socketOutputStream ;
        InputStream socketInputStream ;
        try {
            socketOutputStream = clientSocket.getOutputStream();
            socketInputStream = clientSocket.getInputStream();
        } catch (IOException e) {
            System.err.println("Problem with client's socket: " + e);
            return;
        }
        PrintWriter socketWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socketOutputStream)));
        BufferedInputStream bufferedInputStream = new BufferedInputStream(socketInputStream);

        // game round starts here
        // printing greetings if connection was successful
        sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, greetings + minimalBet + "$;", "Your money: "
                + playerMoney + "\n");
        // initializing shoe, dealer hand, net variable and "continue game" flag
        boolean playerWantAnotherGame = true;
        int net = 0;
        Deck shoe = new Shoe(6, true);
        Hand dealerHand = new Hand();
        Hand playerHand = new Hand();

        // game loop
        while (playerWantAnotherGame) {
            String incomingBuffer;
            Integer bet;

            // receive bet from player
            bet = makeBet(bufferedInputStream, socketWriter);

            // passing info from server to player
            sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Player's bet: " + bet);
            shoe.shuffle();
            dealerHand.clear();
            dealerHand.retrieveCardFromDeck(shoe);
            playerHand.clear();
            playerHand.retrieveCardFromDeck(shoe, 2);
            sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Dealer's cards: " + dealerHand);
            sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Player's cards: " + playerHand);
            sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Your money:     " + playerMoney);

            // variable which represents game result
            GameResult gameResult;

            // if player has a blackjack, dealer doesn't need to take more cards - player wins immediately
            if (playerHand.isBlackjack()) {
                gameResult = GameResult.PLAYER_BLACKJACK;
            } else {
                // variable shows if player has already stand or bust
                boolean boxReady = false;
                // HIT/STAND/DOUBLE loop - continues upon hitting more than 22 or choosing STAND
                while (!boxReady) {
                    sendMessage(socketWriter, ClientMode.CLIENT_SEND_TO_SERVER, chooseActionInstruction);
                    incomingBuffer = receiveIncomingData(bufferedInputStream);
                    if (incomingBuffer == null) {
                        System.err.println("Impossible to reach player. Disconnect.");
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    char playerDecision = getMessage(incomingBuffer).charAt(0);
                    switch (playerDecision) {
                        case 'h':
                        case 'H': {
                            playerHand.retrieveCardFromDeck(shoe);
                            break;
                        }
                        case 'd':
                        case 'D': {
                            bet *= 2;
                            playerHand.retrieveCardFromDeck(shoe);
                            boxReady = true;
                            break;
                        }
                        case 's':
                        case 'S': {
                            boxReady = true;
                            break;
                        }
                    }
                    if (playerHand.isBusted()) boxReady = true;
                    sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Dealer's cards: " + dealerHand);
                    sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Player's cards: " + playerHand);
                }

                // HIT/STAND/DOUBLE loop ended
                // determining game result and writing it to gameResult variable
                sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Dealer retrieves cards...");
                while (!dealerHand.reachesDealerHit()) {
                    dealerHand.retrieveCardFromDeck(shoe);
                }
                if (playerHand.isBusted()) {
                    gameResult = GameResult.DEALER_WINS;
                    sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Dealer's cards: " + dealerHand);
                    sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Player's cards: " + playerHand);
                } else {
                    sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Dealer's cards: " + dealerHand);
                    sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Player's cards: " + playerHand);
                    if (dealerHand.isBusted()) {
                        gameResult = GameResult.PLAYER_WINS;
                    } else {
                        if (playerHand.getMaxScore() == dealerHand.getMaxScore()) {
                            gameResult = GameResult.STAY;
                        } else {
                            if (playerHand.getMaxScore() > dealerHand.getMaxScore()) {
                                gameResult = GameResult.PLAYER_WINS;
                            } else {
                                gameResult = GameResult.DEALER_WINS;
                            }
                        }
                    }
                }
            }

            // doing appropriate actions for each variant of game end
            switch (gameResult) {
                case PLAYER_BLACKJACK: {
                    net = (int) Math.round(1.5 * bet);
                    sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Blackjack! Player wins.",
                            "Net: " + net);
                    playerMoney += net;
                    break;
                }
                case PLAYER_WINS: {
                    net = bet;
                    sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Player wins.", "Net: " + net);
                    break;
                }
                case DEALER_WINS: {
                    net = -bet;
                    sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Dealer wins.", "Net: " + net);
                    break;
                }
                case STAY: {
                    net = 0;
                    sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Stay.", "Net: " + net);
                }
            }

            // calculating new value of player's money amount and sending info to player
            playerMoney += net;
            sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Your money: " + playerMoney);

            // checking of player wants to continue game
            // 'y' or 'Y' means continue, any other symbol means game ending an closing connection
            sendMessage(socketWriter, ClientMode.CLIENT_SEND_TO_SERVER, "Want to play another round? (y/n)");
            incomingBuffer = receiveIncomingData(bufferedInputStream);
            if (incomingBuffer == null) {
                System.err.println("Problem with client's socket: disconnect.");
                return;
            }
            char playerDecision = getMessage(incomingBuffer).charAt(0);
            if (!(playerDecision == 'y' || playerDecision == 'Y')) {
                playerWantAnotherGame = false;
            }
        }

        // end of game: sending information to player and closing socket
        sendMessage(socketWriter, ClientMode.CLIENT_DISCONNECT, "Your results: "
                + (playerMoney - initialPlayerMoney) + "$", "Goodbye.");
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Unable to close client's socket.");
        }
        System.out.println("Player disconnected.");
    }

}
