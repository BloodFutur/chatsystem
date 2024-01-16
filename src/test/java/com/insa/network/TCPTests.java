package com.insa.network;

import com.insa.network.tcp.TCPClient;
import com.insa.network.tcp.TCPMessage;
import com.insa.network.tcp.TCPServer;
import com.insa.users.User;
import com.insa.utils.MyLogger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TCPTests {

    private static final MyLogger LOGGER = new MyLogger(TCPTests.class.getName());


    private static final int TEST_PORT = 1234;

    /**
     * Test that TCPClients can send messages to TCPServers
     */
    @Test
    void testTCPClient() throws IOException, InterruptedException {
        // Create test messages
        TCPMessage testMessage = new TCPMessage(UUID.randomUUID(), "Hello", new User("sender"), new User("receiver"), new Timestamp(new Date().getTime()));
        TCPMessage testMessage2 = new TCPMessage(UUID.randomUUID(),"Hello \n new line", new User("sender2"), new User("receiver2"), new Timestamp(new Date().getTime()));
        TCPMessage testMessage3 = new TCPMessage(UUID.randomUUID(),"Hello \n new line\t",  new User("sender2"), new User("receiver2"), new Timestamp(new Date().getTime()));
        List<TCPMessage> testMessages = Arrays.asList(testMessage, testMessage2, testMessage3);

        // Create TCP Server
        List<TCPMessage> receivedMessages = new ArrayList<>();
        TCPServer server = new TCPServer(TEST_PORT);
        server.addObserver(receivedMessages::add);
        server.start();

        // Create TCP Client
        TCPClient client = new TCPClient();
        client.startConnection("127.0.0.1", TEST_PORT);

        // Send messages
        for (TCPMessage message : testMessages) {
            client.sendMessage(message);
        }

        // Check that the messages have been received
        Thread.sleep(100);

        LOGGER.info("[TEST] - Received messages list:\n" +  receivedMessages);
        LOGGER.severe("HEEEY");
        assertEquals(testMessages.size(), receivedMessages.size());
        assertEquals(testMessages, receivedMessages);

        client.stopConnection();
        server.interrupt();
    }
}
