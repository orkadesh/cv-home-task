package blackjackHomeTask.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Gene on 7/31/2015.
 */
public class PlayersCounter implements Runnable {

    protected boolean registrationAllowed = true;
    protected List<Socket> sockets;
    protected ServerSocket serverSocket;
    protected int allowedAmount;

    public PlayersCounter(List<Socket> sockets, int availablePlayers) {
        this.sockets = sockets;
        this.allowedAmount = availablePlayers;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(9001, allowedAmount);
        }
        catch(IOException e) {
            System.err.println("Exception in creating server socket");
        }
        while (registrationAllowed && allowedAmount>0) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
                sockets.add(clientSocket);
                allowedAmount--;
                System.out.println("Allowed amount: " + allowedAmount);
            }
            catch (IOException e) {
                if (!registrationAllowed) {
                    System.out.println("Registration of players ended.");
                    break;
                }
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }
        }
        try {
            serverSocket.close();
        }
        catch(IOException e) {
            System.out.println(e);
        }
        System.out.println("Registration ends.");
    }

    public void endRegistration() {
        this.registrationAllowed = false;
        try {
            serverSocket.close();
        }
        catch(IOException e) {
            System.out.println("EndReg method: exception: " + e);
        }
    }

    public List<Socket> getSockets() {
        return sockets;
    }

}
