package blackjackHomeTask.server;

import blackjackHomeTask.client.ClientMode;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.net.ServerSocket;

/**
 * Created by Gene on 7/30/2015.
 */

public class BlackjackServer implements Runnable {

    protected int serverPort = 9001;
    protected ServerSocket serverSocket = null;
    protected Thread runningThread = null;
    protected final int maxBoxes = 4;
    protected ExecutorService threadPool = Executors.newFixedThreadPool(maxBoxes);
    protected final BlockingQueue<Integer> freeIndexesQueue = new ArrayBlockingQueue<Integer>(maxBoxes);

    public BlackjackServer(int port) {
        this.serverPort = port;
    }

    public void run() {

        synchronized (this) {
            this.runningThread = Thread.currentThread();
            for(int index = 1; index<=maxBoxes; index++) {
                freeIndexesQueue.add(index);
            }
        }

        List<Socket> acceptedSockets = new ArrayList<Socket>();
        PlayersCounter playersCounter = new PlayersCounter(acceptedSockets, maxBoxes);
        Thread registerThread = new Thread(playersCounter);
        registerThread.start();
        try {
            Thread.currentThread().sleep(15000);
        }
        catch(InterruptedException e) {
            System.out.println("Main sleep interrupted.");
        }
        System.out.println("Accepted sockets: " + acceptedSockets.size());
        playersCounter.endRegistration();
        Phaser betPhaser = null;
        Phaser dealerChoisePhaser = null;
        Phaser dealerDone = null;
        Semaphore queueSemaphore = new Semaphore(1);
        if(!acceptedSockets.isEmpty()) {
            betPhaser = new Phaser(acceptedSockets.size());
            dealerChoisePhaser = new Phaser(acceptedSockets.size() + 1);
            dealerDone = new Phaser(acceptedSockets.size() + 1);
        }
        List<WorkerRunnable> playerHandlers = new ArrayList<WorkerRunnable>();
        Deck shoe = new Shoe(6, true);
        Hand dealerHand = new Hand();
        dealerHand.retrieveCardFromDeck(shoe);
        for(Socket clientSocket: acceptedSockets) {
            Integer index = null;
            try {
                index = freeIndexesQueue.take();
            }
            catch(InterruptedException e) {
                System.out.println("Interrupted exception in getting indexes for threads: " + e);
            }
            WorkerRunnable newPlayerHandler = new WorkerRunnable(clientSocket, index, freeIndexesQueue,
                    betPhaser, shoe, dealerHand, queueSemaphore, dealerChoisePhaser, dealerDone);
            playerHandlers.add(newPlayerHandler);
            new Thread(newPlayerHandler).start();
        }
        dealerChoisePhaser.arriveAndAwaitAdvance();
        while (!dealerHand.reachesDealerHit()) {
            dealerHand.retrieveCardFromDeck(shoe);
        }
        dealerDone.arriveAndAwaitAdvance();

    }

    public static void main(String... args) {
        BlackjackServer server = new BlackjackServer(9001);
        new Thread(server).start();
    }

}

