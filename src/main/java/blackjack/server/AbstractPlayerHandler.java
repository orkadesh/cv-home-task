package blackjack.server;

import blackjack.client.ClientMode;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Abstract parent for separate threads, which handle server-client communication. Has several basic methods
 * which serve to communicate between server and player. Also describes several common variables, such as
 * values of message terminator, minimal players' bet etc.
 * Subclasses are supposed to implement {@link Runnable#run()}, where will be specific logic for each subclass.
 * @author yevhen bilous
 */
public abstract class AbstractPlayerHandler implements Runnable{

    /**
     * Designed for internal purpose, represents game round result.
     */
    enum GameResult {
        PLAYER_BLACKJACK, PLAYER_WINS, DEALER_WINS, STAY
    }

    public static final String greetings = "Hello. You are going to play blackjack. \n" +
            "Our minimal bet is ";
    public static final String chooseActionInstruction = "Type h for HIT, s for STAND and d for DOUBLE.";
    public static final String betProposal = "Please, type your bet: ";

    public static final char TERMINATOR = '#';
    public static final char DIVIDER = '%';
    protected int playerMoney = 100;
    protected int minimalBet = 1;
    protected Socket clientSocket = null;

    /**
     * Sends message to player.
     *
     * @param writer   PrintWriter object, used to send message
     * @param mode     Mode, which client will switched do when after receiving message
     * @param messages Text information, passed from server to player
     */
    public void sendMessage(PrintWriter writer, ClientMode mode, String... messages) {
        StringBuilder message = new StringBuilder(mode.toString() + '$');
        for (String messageLine : messages) {
            message.append(messageLine);
            message.append('\n');
        }
        writer.write(message.substring(0, message.length() - 1) + '#');
        writer.flush();
    }

    /**
     * Receive message from player. Terminator {@link SinglePlayerHandler#TERMINATOR} is used to
     * recognize end of message.
     *
     * @param socketInBufStream BufferedInputStream object, used to receive message from player
     * @return raw message, received from player
     */
    public String receiveIncomingData(BufferedInputStream socketInBufStream) {
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
            System.out.println("Impossible to get data from client's socket.");
            return null;
        }
    }

    /**
     * Returns actual text message received from player
     *
     * @param inputData raw message, received from player
     * @return actual text message received from player
     */
    public static String getMessage(String inputData) {
        return inputData.substring(inputData.indexOf(DIVIDER) + 1, inputData.length());
    }

    /**
     * Prompts player to make bet and continue asking for bet if received values are inappropriate.
     * @param bufferedInputStream {@link BufferedInputStream} object, used to receive data from player
     * @return bet value if it was successfully received, null otherwise
     */
    protected Integer makeBet(BufferedInputStream bufferedInputStream, PrintWriter socketWriter) {
        Integer bet = null;
        String incomingBuffer;
        boolean betMade = false;
        while (!betMade) {
            sendMessage(socketWriter, ClientMode.CLIENT_SEND_TO_SERVER, betProposal +
                    "; Minimal bet is " + minimalBet + "$.");
            incomingBuffer = receiveIncomingData(bufferedInputStream);
            if (incomingBuffer == null) {
                System.err.println("Client disconnected.");
                return null;
            }
            try {
                bet = Integer.valueOf(getMessage(incomingBuffer));
                betMade = bet >= minimalBet;
            } catch (NumberFormatException e) {
                betMade = false;
            }
            if (!betMade)
                sendMessage(socketWriter, ClientMode.CLIENT_RECEIVE,
                        "Please, type one positive integer number, equal or bigger than minimal bet");
        }
        return bet;
    }
}
