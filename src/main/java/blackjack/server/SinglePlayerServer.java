package blackjack.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Represents single-player blackjack server. In fact, many users can connect, but each wiil have
 * separate game with dealer. Server creates server socket with unlimited possible connections and
 * creates separate thread for handling each separate player.
 *
 * @author bilous yevhen
 */
public class SinglePlayerServer implements Runnable {

    protected int serverPort = 9001;
    protected ServerSocket serverSocket = null;

    public SinglePlayerServer(int port) {
        this.serverPort = port;
    }

    /**
     * Until terminated, awaits for clients' connections and creates separate thread
     * to serve each player separately.
     */
    public void run() {
        System.out.println("Server started.");
        try {
            serverSocket = new ServerSocket(serverPort);
        } catch (IOException e) {
            System.err.println("Exception in creating server socket");
        }
        while (true) {
            boolean connectionSuccess = false;
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
                connectionSuccess = true;
            } catch (IOException e) {
                System.err.println("Fail to work with server socket.");
            }
            if (connectionSuccess)
                new Thread(new SinglePlayerHandler(clientSocket)).start();
        }
    }

    /**
     * Parses command line arguments and, if they are appropriate, starts single player
     * blackjack server.
     *
     * @param args must contain one argument: port for listening
     */
    public static void main(String... args) {
        System.out.println(args[0]);
        if (args.length == 0) {
            System.err.println("You need to specify port as argument to start server.");
        } else {
            int port = Integer.valueOf(args[0]);
            SinglePlayerServer single = new SinglePlayerServer(port);
            new Thread(single).start();
        }
    }

}
