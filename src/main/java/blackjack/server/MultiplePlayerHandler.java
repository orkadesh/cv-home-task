package blackjack.server;

import blackjack.cards.Deck;
import blackjack.cards.Hand;
import blackjack.client.ClientMode;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * Represents server logic, which communicates with separate user in multiplier blackjack
 * game. Synchronization barriers, received through one available constructor, serves for
 * synchronization between several players and dealer, so this class must be used only in tandem
 * with {@link MultiplePlayerServer}.
 * @author yevhen bilous
 */
public class MultiplePlayerHandler extends AbstractPlayerHandler implements Runnable {

    // variables that must be shared among threads
    protected int playerIndex;
    protected Deck shoe;
    protected Hand dealerHand;
    protected BlockingQueue<Integer> queue;

    // variables which serve for synchronization purpose
    protected Semaphore queueSemaphore;
    protected CyclicBarrier dealerChoiceBarrier;
    protected CyclicBarrier dealerDoneBarrier;
    protected CyclicBarrier betBarrier;

    // flags indicate if terminated thread already passed some game phase
    protected boolean betPassed = false;
    protected boolean queuePassed = false;
    protected boolean dealerChoicePassed = false;
    protected boolean dealerDonePassed = false;

    /**
     * All variables must be received from an instance of {@link MultiplePlayerServer}, and serve for
     * synchronization purposes.
     */
    public MultiplePlayerHandler(Socket clientSocket, int playerIndex, BlockingQueue<Integer> queue,
                                 Deck shoe, Hand dealerHand, CyclicBarrier betBarrier, Semaphore queueSemaphore,
                                 CyclicBarrier dealerChoiceBarrier, CyclicBarrier dealerDoneBarrier) {
        System.out.println("New server thread. Index: " + playerIndex);
        this.queue = queue;
        this.clientSocket = clientSocket;
        this.playerIndex = playerIndex;
        this.shoe = shoe;
        this.dealerHand = dealerHand;
        this.queueSemaphore = queueSemaphore;
        this.dealerChoiceBarrier = dealerChoiceBarrier;
        this.dealerDoneBarrier = dealerDoneBarrier;
        this.betBarrier = betBarrier;
    }

    /**
     * Contains logic of server-client communication and game mechanic (multiplayer game).
     */
    public void run() {
        int initialPlayerMoney = playerMoney;
        String incomingBuffer;
        OutputStream socketOutputStream;
        InputStream socketInputStream;
        try {
            socketOutputStream = clientSocket.getOutputStream();
            socketInputStream = clientSocket.getInputStream();
        } catch (IOException e) {
            System.err.println("Connection with client was lost.");
            awaitAllBarriers();
            return;
        }
        PrintWriter socketWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socketOutputStream)));
        BufferedInputStream bufferedInputStream = new BufferedInputStream(socketInputStream);
        // session login starts

        sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, greetings, "Your money: " + playerMoney + "\n");
        Integer bet = makeBet(bufferedInputStream, socketWriter);
        if (bet == null) {
            disconnect(socketWriter);
        }
        sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Player's bet: " + bet);
        sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Waiting for bets from other players...");
        try {
            betBarrier.await();
            betPassed = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
        Hand playerHand = new Hand();
        playerHand.clear();
        synchronized (shoe) {
            playerHand.retrieveCardFromDeck(shoe, 2);
        }

        sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Players in game: " + queue.remainingCapacity());
        sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Dealer's cards: " + dealerHand);
        sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Player's cards: " + playerHand);
        sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Your money:     " + playerMoney);
        GameResult gameResult;
        sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Please wait your turn...");

        // synchronization between players - only one can take cards at one time
        try {
            queueSemaphore.acquire();
        } catch (InterruptedException e) {
            System.out.println("Interrupted on semaphore");
        }
        sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Now is your turn.");
        if (playerHand.isBlackjack()) {
            gameResult = GameResult.PLAYER_BLACKJACK;
            awaitAllBarriers();
        } else {
            boolean boxReady = false;

            // HIT/DOUBLE/STAND loop, synchronized with queueSemaphore
            while (!boxReady) {
                sendMessage(socketWriter, ClientMode.CLIENT_SEND_TO_SERVER, chooseActionInstruction);
                incomingBuffer = receiveIncomingData(bufferedInputStream);
                if (incomingBuffer == null) {
                    disconnect(socketWriter);
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
            // end of HIT/STAND/DOUBLE loop
            sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Waiting for other players...");
            queueSemaphore.release();
            queuePassed = true;
            try {
                dealerChoiceBarrier.await();
                dealerChoicePassed = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
            sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Other players are done with their cards.",
                    "Dealer retrieves cards...");
            try {
                dealerDoneBarrier.await();
                dealerDonePassed = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
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
        int net = 0;

        // obtaining game result
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

        // print results and disconnect player
        playerMoney += net;
        sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Your money: " + playerMoney);
        playerHand.clear();
        sendMessage(socketWriter, ClientMode.CLIENT_DISCONNECT, "Your results: "
                + (playerMoney - initialPlayerMoney), "Goodbye.");

        // session logic ends
        try {
            queue.put(playerIndex);
        } catch (InterruptedException ie) {
            System.out.println("Client queue interrupted.");
        }
        try {
            socketOutputStream.close();
        } catch (IOException e) {
            System.err.println("Unsuccessful closing of client's socket.");
        }
    }

    /**
     * Tries to disconnect player and await for all barriers in sequence,
     * calling {@link MultiplePlayerHandler#awaitAllBarriers()}.
     */
    private void disconnect(PrintWriter socketWriter) {
        try {
            sendMessage(socketWriter, ClientMode.CLIENT_DISCONNECT);
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Socket closing terminated.");
        }
        awaitAllBarriers();
    }

    /**
     * Must be used for synchronization purposes when client accidentally disconnected.
     */
    private void awaitAllBarriers() {
        try {
            if (!betPassed) {
                betBarrier.await();
            }
            if (!queuePassed) {
                queueSemaphore.release();
            }
            if (!dealerChoicePassed) {
                dealerChoiceBarrier.await();
            }
            if (!dealerDonePassed) {
                dealerDoneBarrier.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

}

