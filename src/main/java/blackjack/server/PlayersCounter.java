package blackjack.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Serves for accepting players' connections, denying connections that are over
 * specified limit. Should be used in tandem with {@link MultiplePlayerServer}.
 * @author yevhen bilous
 */
public class PlayersCounter implements Runnable {

    protected boolean registrationAllowed = true;
    protected List<Socket> sockets;
    protected ServerSocket serverSocket;
    protected int allowedAmount;
    protected int serverPort;

    public PlayersCounter(List<Socket> sockets, int availablePlayers, int serverPort) {
        this.sockets = sockets;
        this.allowedAmount = availablePlayers;
        this.serverPort = serverPort;
    }

    /**
     * Logic of establishing connections between server and clients. Accept for specified amount
     * of connections, then terminates.
     */
    public void run() {
        try {
            serverSocket = new ServerSocket(serverPort, allowedAmount);
        }
        catch(IOException e) {
            System.err.println("Exception while creating server socket");
            return;
        }
        while (registrationAllowed && allowedAmount>0) {
            Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
                sockets.add(clientSocket);
                allowedAmount--;
                System.out.println("Free boxes remained: " + allowedAmount);
            }
            catch (IOException e) {
                if (!registrationAllowed) {
                    System.out.println("Registration of players ended: time out.");
                    break;
                }
            }
        }
        try {
            serverSocket.close();
        }
        catch(IOException e) {
            System.err.println("Unable to properly close server socket");
            return;
        }
        if(allowedAmount==0) {
            System.out.println("All boxes are occupied");
        }
    }

    /**
     * Ends socket accepting loop in run() and server socket.
     */
    public void endRegistration() {
        this.registrationAllowed = false;
        try {
            serverSocket.close();
        }
        catch(IOException e) {
            System.out.println("EndReg method: exception: " + e);
        }
    }

    /**
     * Returns connected players sockets.
     * @return sockets, associated with clients who were successfully connected to server
     */
    public List<Socket> getSockets() {
        return sockets;
    }

}
