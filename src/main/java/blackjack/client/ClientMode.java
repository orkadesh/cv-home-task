package blackjack.client;

/**
 * Provides values used in simple communication protocol, which is used in client-server
 * communication. Each value of enum represents action which server wants a client to do.
 * In {@link ClientMode#CLIENT_RECEIVE} mode client must await for server responce;
 * in {@link ClientMode#CLIENT_SEND_TO_SERVER} mode client must prompt user to type some text
 * and then transfer it to server;
 * in {@link ClientMode#CLIENT_DISCONNECT} mode client must close it's open socket and do other
 * finalization actions if necessary.
 * @author yevhen bilous
 */

public enum ClientMode {
    CLIENT_RECEIVE, CLIENT_SEND_TO_SERVER, CLIENT_DISCONNECT
}
