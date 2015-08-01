package blackjack.server;

import blackjack.cards.Deck;
import blackjack.cards.Hand;
import blackjack.cards.Shoe;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Blackjack server, which supports multiple user connections with players amount restriction.
 * Players have some time to connect to the server, then game round starts.
 *
 * @author yevhen bilous
 */
public class MultiplePlayerServer implements Runnable {

    private int serverPort = 9001;
    private Thread runningThread = null;
    private final int maxBoxes = 5;
    private BlockingQueue<Integer> freeIndexesQueue = null;
    private static final int waitSecondsToRegister = 15;

    /**
     * Constructor, which specifies a port to listen.
     *
     * @param port Listening port.
     */
    public MultiplePlayerServer(int port) {
        this.serverPort = port;
    }

    /**
     * Until terminated, awaits for players to connect. After game round creates new game round.
     * Creates different thread for each player with accompanied {@link MultiplePlayerHandler} instance.
     * Synchronizes actions of each created {@link MultiplePlayerHandler} instance.
     * If no player connects during specified time, starts game round from scratch.
     */
    public void run() {

        System.out.println("New round. Allowed players: " + maxBoxes);

        // fill concurrent queue which holds unique indexes of each player
        freeIndexesQueue = new ArrayBlockingQueue<Integer>(maxBoxes);
        synchronized (this) {
            this.runningThread = Thread.currentThread();
            for (int index = 1; index <= maxBoxes; index++) {
                freeIndexesQueue.add(index);
            }
        }

        // starts waiting for connected players
        List<Socket> acceptedSockets = new ArrayList<Socket>();
        PlayersCounter playersCounter = new PlayersCounter(acceptedSockets, freeIndexesQueue.size(), serverPort);
        Thread registerThread = new Thread(playersCounter);
        registerThread.start();
        try {
            runningThread.sleep(waitSecondsToRegister * 1000);
        } catch (InterruptedException e) {
            System.out.println("Main sleep interrupted.");
        }

        // determine, if there were any connected players
        System.out.println("Players joined: " + acceptedSockets.size());
        playersCounter.endRegistration();
        if (acceptedSockets.size() == 0) {
            System.out.println("Nobody connected.");
            return;
        }

        // create synchronization variables
        CyclicBarrier betBarrier = new CyclicBarrier(acceptedSockets.size());
        CyclicBarrier dealerChoiceBarrier = new CyclicBarrier(acceptedSockets.size() + 1);
        CyclicBarrier dealerDoneBarrier = new CyclicBarrier(acceptedSockets.size() + 1);
        Semaphore queueSemaphore = new Semaphore(1);
        List<MultiplePlayerHandler> playerHandlers = new ArrayList<MultiplePlayerHandler>();

        // create shared show and dealer's hand objects
        Deck shoe = new Shoe(6, true);
        Hand dealerHand = new Hand();
        dealerHand.retrieveCardFromDeck(shoe);

        // creating separate threads for each player
        for (Socket clientSocket : acceptedSockets) {
            Integer index = null;
            try {
                index = freeIndexesQueue.take();
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception in getting indexes for threads: " + e);
            }
            MultiplePlayerHandler newPlayerHandler = new MultiplePlayerHandler(clientSocket, index, freeIndexesQueue,
                    shoe, dealerHand, betBarrier, queueSemaphore, dealerChoiceBarrier, dealerDoneBarrier);
            playerHandlers.add(newPlayerHandler);
            new Thread(newPlayerHandler).start();
        }

        // synchronization
        try {
            dealerChoiceBarrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }

        // on this phase, every player is one with his cards, so it's dealer turn to take cards
        // need synchronization because dealer hand is shared object
        while (!dealerHand.reachesDealerHit()) {
            dealerHand.retrieveCardFromDeck(shoe);
        }

        // final synchronization
        try {
            dealerDoneBarrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
        System.out.println("Round ends.");
    }

    public static void main(String... args) {
        if (args.length != 1) {
            System.err.println("Wrong arguments.");
        } else {
            int port = Integer.valueOf(args[0]);
            while (true) {
                MultiplePlayerServer server = new MultiplePlayerServer(9001);
                Thread gameRoundThread = new Thread(server);
                gameRoundThread.start();
                try {
                    gameRoundThread.join();
                } catch (InterruptedException e) {
                    System.err.println("Waiting for round end interrupted.");
                    return;
                }
            }
        }
    }
}

