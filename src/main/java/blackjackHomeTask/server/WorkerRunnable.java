package blackjackHomeTask.server;

import blackjackHomeTask.client.ClientMode;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;

/**
 * Created by Gene on 7/30/2015.
 */
public class WorkerRunnable implements Runnable {

    enum GameResult {
        PLAYER_BLACKJACK, PLAYER_WINS, DEALER_WINS, STAY
    }

    static final String greetings = "Hello. You are going to play blackjack. \n" +
            "Our minimal bet is 1$.";
    static final String chooseActionInstruction = "Type h for HIT, s for STAND and d for DOUBLE.";
    static final String betProposal = "Please, type your bet: ";
    public static final String DIVIDER = "#";

    protected Socket clientSocket = null;
    protected BlockingQueue<Integer> queue;
    protected Phaser betPhaser;
    protected int playerIndex;
    protected Deck shoe;
    protected Hand dealerHand;
    protected boolean betTaken = false;
    protected boolean betMissed = false;
    protected PrintWriter socketWriter;
    protected Semaphore queueSemaphore;
    protected Phaser dealerChoisePhaser;
    protected Phaser dealerDone;

    public WorkerRunnable(Socket clientSocket, int playerIndex, BlockingQueue<Integer> queue,
                          Phaser betPhaser, Deck shoe, Hand dealerHand, Semaphore queueSemaphore,
                          Phaser dealerChoicePhaser, Phaser dealerDone) {
        System.out.println("New server thread. Index: " + playerIndex);
        this.queue = queue;
        this.betPhaser = betPhaser;
        this.clientSocket = clientSocket;
        this.playerIndex = playerIndex;
        this.shoe = shoe;
        this.dealerHand = dealerHand;
        this.queueSemaphore = queueSemaphore;
        this.dealerChoisePhaser = dealerChoicePhaser;
        this.dealerDone = dealerDone;
    }

    public void run() {
        // todo: write implementation of server thread logic
        try {
            int playerMoney = 100;
            int initialPlayerMoney = playerMoney;
//            System.out.println("Server thread started. Player connected.");
            OutputStream socketOutputStream = clientSocket.getOutputStream();
            InputStream socketInputStream = clientSocket.getInputStream();
            socketWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socketOutputStream)));
            BufferedInputStream bufferedInputStream = new BufferedInputStream(socketInputStream);
            BufferedReader socketReader = new BufferedReader(new InputStreamReader(socketInputStream));
            // session login starts

//            socketWriter.write(greetings + "Yor money: " + playerMoney + '\n');
//            socketWriter.flush();

            sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, greetings, "Your money: "
                    + playerMoney + "\n");



            boolean playerWantAnotherGame = true;
            int net = 0;
            while (playerWantAnotherGame) {
                sendMessage(socketWriter, ClientMode.CLIENT_TRANSFER, betProposal);
                String incomingBuffer = receiveIncomingData(bufferedInputStream);
                if(incomingBuffer==null || betMissed) {
                    try {
                        queue.put(playerIndex);
                    }
                    catch(InterruptedException e) {
                        System.out.println("Exception in closing missed bet thread");
                    }
                    return;
                }
                Integer bet = Integer.valueOf(getMessage(incomingBuffer));
                sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Player's bet: " + bet);
                sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Waiting for bets from other players...");
                betTaken = true;
                betPhaser.arriveAndAwaitAdvance();

                // todo: implement internal shoe shuffling mechanism
//                Deck shoe = new Shoe(6, true);
//                shoe.shuffle();
//                Hand dealerHand = new Hand();
                Hand playerHand = new Hand();
//                dealerHand.clear();
//                dealerHand.retrieveCardFromDeck(shoe);
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
                try {
                    queueSemaphore.acquire();
                }
                catch(InterruptedException e) {
                    System.out.println("Interrupted on semaphore");
                }
                sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Now is your turn.");
                if (playerHand.isBlackjack()) {
                    gameResult = GameResult.PLAYER_BLACKJACK;
                } else {
                    boolean boxReady = false;
                    while (!boxReady) {
                        sendMessage(socketWriter, ClientMode.CLIENT_TRANSFER, chooseActionInstruction);
                        incomingBuffer = receiveIncomingData(bufferedInputStream);
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
                    // end of cycle
                    queueSemaphore.release();
                    dealerChoisePhaser.arriveAndAwaitAdvance();
                    sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Dealer retrieves cards...");
                    dealerDone.arriveAndAwaitAdvance();
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
                playerMoney += net;
                sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE, "Your money: " + playerMoney);
                sendMessage(socketWriter, ClientMode.CLIENT_TRANSFER, "You have 15 seconds to enjoy next game.",
                        "if you want to enjoy, type y");
                incomingBuffer = receiveIncomingData(bufferedInputStream);
                char playerDecision = getMessage(incomingBuffer).charAt(0);
                if (playerDecision != 'y')
                    playerWantAnotherGame = false;
            }


            sendMessage(socketWriter, ClientMode.CLIENT_DISCONNECT, "Your results: "
                    + (playerMoney - initialPlayerMoney), "Goodbye.");

            // session logic ends
            try {
                queue.put(playerIndex);
            }
            catch(InterruptedException ie) {
                System.out.println("Client queue interrupted.");
            }
            socketOutputStream.close();
//            System.out.println("Server thread finished. Player disconnected.");
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
    }

    public void sendMessage(PrintWriter writer, ClientMode mode, String... messages) {
        StringBuilder message = new StringBuilder(mode.toString() + '$');
        for (String messageLine : messages) {
            message.append(messageLine + '\n');
        }
        writer.write(message.substring(0, message.length() - 1) + '#');
        writer.flush();
    }

    public String receiveIncomingData(BufferedInputStream socketInBufStream) {
        byte[] return_data = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int byte_read = socketInBufStream.read();
            while ((char) byte_read != '#') {
                baos.write(byte_read);
                byte_read = socketInBufStream.read();
            }
            return_data = baos.toByteArray();
            baos.close();
            return new String(return_data);
        } catch (IOException exception) {
            // todo: do something with exception
            System.out.println("Exception in method receiveIncomingData: " + exception);
            betMissed = true;
        }
        return null;
    }

    public static ClientMode getMode(String inputData) {
        return ClientMode.valueOf(inputData.substring(0, inputData.indexOf('$')));
    }

    public static String getMessage(String inputData) {
        return inputData.substring(inputData.indexOf('$') + 1, inputData.length());
    }

    public boolean wasBetTaken() {
        return betTaken;
    }

    public void disconnect() {
        try {
            sendMessage(socketWriter, ClientMode.CLIENT_DISCONNECT, "Time out.");
            clientSocket.close();
            betMissed = true;
//            clientSocket.shutdownInput();
        }
        catch(IOException e) {
            System.out.println("Socket connection terminated.");
        }
    }

}

