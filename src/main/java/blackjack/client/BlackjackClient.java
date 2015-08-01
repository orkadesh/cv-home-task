package blackjack.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.*;

/**
 * Blackjack client, that connects to server and serves as thin client, i.e. it doesn't implement
 * any real game mechanics, just client-server (more specifically, blackjack-server - blackjack-client)
 * communication mechanism.
 * It uses {@link ClientMode} enum to determine client behavior. See {@link ClientMode} description for
 * more specific information.
 * @author yevhen bilous
 */
public class BlackjackClient implements Runnable {

    protected int serverPort = 9001;
    protected InetAddress address;
    public static final char TERMINATOR = '#';
    public static final char DIVIDER = '$';

    /**
     * Creates blackjack client, which connects to specified address and port.
     * @param address address of blackjack server
     * @param port port, which blackjack server is listening
     */
    public BlackjackClient(InetAddress address, int port) {
        this.address = address;
        this.serverPort = port;
    }

    /**
     * Sends message to server.
     * @param writer {@link PrintWriter} object, used to send message thr
     * @param messages messages, which client want to send to server
     */
    public static void sendMessage(PrintWriter writer, String... messages) {
        StringBuilder message = new StringBuilder(DIVIDER);
        for (String messageLine : messages) {
            message.append(messageLine);
            message.append('\n');
        }
        writer.write(message.substring(0, message.length() - 1) + TERMINATOR);
        writer.flush();
    }

    /**
     * Receive message from server. Terminator {@link BlackjackClient#TERMINATOR} is used to recognize
     * end of bytes stream.
     * @param socketInBufStream {@link BufferedInputStream} object, used to receive messages from server
     * @return message, successfully received from server, null otherwise
     */
    public static String receiveIncomingData(BufferedInputStream socketInBufStream) {
        byte[] return_data;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            int byte_read = socketInBufStream.read();
            while ((char) byte_read != TERMINATOR) {
                byteArrayOutputStream.write(byte_read);
                byte_read = socketInBufStream.read();
            }
            return_data = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            return new String(return_data);
        } catch (IOException exception) {
            System.out.println("IOException while getting incoming string in client");
            return null;
        }
    }

    /**
     * Returns client mode, encoded in message received from server. Mode is actually an enum instance
     * of {@link ClientMode}. Mode is used to determine client behavior.
     * @param inputData raw message, received from server
     * @return client mode, which is used by client to determine it's next behavior
     */
    public static ClientMode getMode(String inputData) {
        return ClientMode.valueOf(inputData.substring(0, inputData.indexOf('$')));
    }

    /**
     * Returns informational message, encoded in whole message, received from server.
     * Normally this message must be represented to human player by printing it in cli.
     * @param inputData raw message, received from server
     * @return informational message, received from server
     */
    public static String getMessage(String inputData) {
        return inputData.substring(inputData.indexOf('$') + 1, inputData.length());
    }

    /**
     * Implements client-server communication from client side. Basically, client's task is to:
     * 1) establish connection 2) loop through read-write cycle (read from server and write to console
     * and vice versa) 3) close socket if communication with server is done.
     */
    public void run() {
        // establish connection with server
        // and terminates execution if address is bad, or server os unreachable
        Socket clientSocket = null;
        boolean connectionComplete = false;
        try {
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(address, serverPort), 5000);
            connectionComplete = true;
            System.out.println("Socket opened. Waiting for game round start...");
        } catch (UnknownHostException unkHosExc) {
            System.err.println("Server address is inappropriate.");
            return;
        } catch (IOException ioExc) {
            System.out.println("Server is unreachable.");
        }

        // assume that connection was successful
        if (connectionComplete && clientSocket.isConnected()) {

            // creating and initializing service variable such as streams and buffers
            BufferedReader playerResponseReader = new BufferedReader(new InputStreamReader(System.in));
            InputStream socketInputStream = null;
            OutputStream socketOutputStream = null;
            try {
                socketInputStream = clientSocket.getInputStream();
                socketOutputStream = clientSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(socketInputStream==null || socketOutputStream==null) {
                System.err.println("Unable to properly work with socket.");
                return;
            }
            BufferedInputStream bufferedInputStream = new BufferedInputStream(socketInputStream);
            PrintWriter socketWriter = new PrintWriter(new OutputStreamWriter(socketOutputStream));

            // getting initial "greeting" message from server
            String incomingString = receiveIncomingData(bufferedInputStream);
            System.out.println(getMessage(incomingString));

            // determining next behavior
            ClientMode clientMode = getMode(incomingString);

            // read-write-wait client loop. Behavior is determined by clientMode enum instance
            while (clientMode != ClientMode.CLIENT_DISCONNECT) {
                switch (clientMode) {
                    case CLIENT_RECEIVE: {
                        incomingString = receiveIncomingData(bufferedInputStream);
                        if(incomingString==null) {
                            System.out.println("Server disconnected.");
                            return;
                        }
                        String message = getMessage(incomingString);
                        if (message.length() > 1) System.out.println(getMessage(incomingString));
                        clientMode = getMode(incomingString);
                        break;
                    }
                    case CLIENT_SEND_TO_SERVER: {
                        String outputData = null;
                        try {
                            outputData = playerResponseReader.readLine();
                        } catch (IOException e) {
                            System.err.println("Internal error.");
                            e.printStackTrace();
                        }
                        sendMessage(socketWriter, outputData);
                        clientMode = ClientMode.CLIENT_RECEIVE;
                        break;
                    }
                }
            }

            // CLIENT_DISCONNECT mode received
            // trying to close socket and end execution
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Unable to close connection properly.");
                e.printStackTrace();
            }
            System.out.println("Connection closed.");
        }
    }

    /**
     * Executable method, which parses cli arguments as follows:
     * ip-address port
     * then creates new Thread with blackjack client, which receive specified address and port
     * on it's creation
     * @param args must be in format: ip-address port
     */
    public static void main(String... args) {
        InetAddress address = null;
        int port;
        if (args.length < 2) {
            System.err.println("Need appropriate number of arguments: address and port");
            return;
        }
        else {
            try {
                address = InetAddress.getByName(args[0]);
            } catch (UnknownHostException e) {
                System.err.println("Wrong address");
            }
            port = Integer.valueOf(args[1]);
        }
        new Thread(new BlackjackClient(address, port)).start();
    }
}
