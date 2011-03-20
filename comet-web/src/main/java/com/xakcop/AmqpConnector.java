package com.xakcop;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class AmqpConnector implements ServletContextListener {

    public static final String AMQP_CONNECTION = "amqpConnection";

    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        String host = context.getInitParameter("amqpHost");
        int port = Integer.parseInt(context.getInitParameter("amqpPort"));
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        Connection connection;
        try {
            connection = factory.newConnection();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create AMQP connection", e);
        }
        context.setAttribute(AMQP_CONNECTION, connection);
        System.out.println("Successfully connected to AMQP broker at host: " + host);
    }

    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        Connection connection = (Connection) context.getAttribute(AMQP_CONNECTION);
        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
