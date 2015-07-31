package blackjackHomeTask.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.*;

public class BlackjackClient {

    protected static int serverPort = 9001;

    public static void main(String... args) {
        Socket clientSocket = null;
        boolean connectionComplete = false;
        try {
            InetAddress address = InetAddress.getLocalHost();
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(address, serverPort), 5000);
            connectionComplete = true;
            System.out.println("Socket opened. Waiting for game round start...");
        } catch (UnknownHostException unkHosExc) {
            // todo: do something with unknownHost exception

        } catch (IOException ioExc) {
            // todo: do something with io exception
            System.out.println("Server is unreachable.");
        }
        if (connectionComplete && clientSocket.isConnected()) {
            try {
                BufferedReader playerResponseReader = new BufferedReader(new InputStreamReader(System.in));
                InputStream socketInputStream = clientSocket.getInputStream();
                OutputStream socketOutputStream = clientSocket.getOutputStream();
                BufferedReader socketReader = new BufferedReader(new InputStreamReader(socketInputStream));
                BufferedInputStream bufferedInputStream = new BufferedInputStream(socketInputStream);
                PrintWriter socketWriter = new PrintWriter(new OutputStreamWriter(socketOutputStream));
                // client logic starts

                String incomingString = receiveIncomingData(bufferedInputStream);
                System.out.println(getMessage(incomingString));

                ClientMode clientMode = getMode(incomingString);
                while(clientMode!=ClientMode.CLIENT_DISCONNECT) {
                    switch(clientMode) {
                        case CLIENT_RECEIVE: {
                            incomingString = receiveIncomingData(bufferedInputStream);
                            String message = getMessage(incomingString);
                            if(message.length()>1) System.out.println(getMessage(incomingString));
                            clientMode = getMode(incomingString);
                            break;
                        }
                        case CLIENT_TRANSFER: {
                            String outputData = playerResponseReader.readLine();
                            sendMessage(socketWriter, clientMode, outputData);
                            clientMode = ClientMode.CLIENT_RECEIVE;
                            break;
                        }
                    }
                }
                clientSocket.close();
                System.out.println("Connection closed.");
            } catch (IOException ioExc) {
                System.out.println("Exception in client");
            }
        }
    }

    public static void sendMessage(PrintWriter writer, ClientMode mode, String ... messages) {
        StringBuilder message = new StringBuilder(mode.toString() + '$');
        for(String messageLine: messages) {
            message.append(messageLine + '\n');
        }
        writer.write(message.substring(0, message.length() - 1) + '#');
        writer.flush();
    }

    public static String receiveIncomingData(BufferedInputStream socketInBufStream) throws IOException{
        byte[] return_data = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int byte_read = socketInBufStream.read();
            while((char)byte_read!= '#')
            {
                baos.write(byte_read);
                byte_read = socketInBufStream.read();
            }
            return_data = baos.toByteArray();
            baos.close();
            return new String(return_data);
        }
        catch(IOException exception) {
            // todo: do something with exception
            System.out.println("IOException while getting incoming string in client");
        }
        return null;
    }

    public static ClientMode getMode(String inputData) {
        return ClientMode.valueOf(inputData.substring(0, inputData.indexOf('$')));
    }

    public static String getMessage(String inputData) {
        return inputData.substring(inputData.indexOf('$') + 1, inputData.length());
    }

}
