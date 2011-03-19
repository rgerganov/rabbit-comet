package com.xakcop;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

public class RabbitPublisher {

    static final String HOST = System.getProperty("amqp.host", "localhost");
    static final int PORT = Integer.getInteger("amqp.port", 5672);
    static final String EXCHANGE = System.getProperty("amqp.exchange", "systemExchange");
    static final String ENCODING = "UTF-8";

    Connection connection;

    void connect() throws IOException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        connection = factory.newConnection();
    }

    void publish() throws IOException {
        Channel channel = connection.createChannel();
        try {
            BasicProperties props = MessageProperties.PERSISTENT_TEXT_PLAIN;
            props.setContentEncoding(ENCODING);
            String message = "Current time: " + SimpleDateFormat.getInstance().format(new Date());
            channel.basicPublish(EXCHANGE, "", props, message.getBytes(ENCODING));
        } finally {
            channel.close();
        }
    }

    void disconnect() throws IOException {
        connection.close();
    }

    public static void main(String[] args) throws IOException {
        RabbitPublisher publisher = new RabbitPublisher();
        publisher.connect();
        publisher.publish();
        System.out.println("Message published.");
        publisher.disconnect();
    }

}
