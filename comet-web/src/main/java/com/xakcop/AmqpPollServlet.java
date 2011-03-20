package com.xakcop;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

public class AmqpPollServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String ENCODING = "UTF-8";

    Map<String, NotifyingConsumer> queues = new HashMap<String, NotifyingConsumer>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getParameter("action") != null) {
            doPost(request, response);
        } else {
            getServletContext().getNamedDispatcher("default").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        String queue = request.getParameter("queue");

        if (action.equals("start")) {
            startConsumer(request, response, queue);
        } else if (action.equals("stop")) {
            stopConsumer(request, response, queue);
        } else if (action.equals("poll")) {
            poll(request, response, queue);
        }
    }

    synchronized void startConsumer(HttpServletRequest request, HttpServletResponse response, final String queue) throws IOException {
        if (queues.containsKey(queue)) {
            System.out.println("Consumer already started for queue: " + queue);
            response.sendError(503);
            return;
        }
        ServletContext context = getServletConfig().getServletContext();
        Connection connection = (Connection) context.getAttribute(AmqpConnector.AMQP_CONNECTION);
        Channel channel = connection.createChannel();
        NotifyingConsumer consumer = new NotifyingConsumer(channel, queue);
        System.out.println("Starting consumer for queue: " + queue);
        consumer.start();
        queues.put(queue, consumer);
        response.setContentType("text/json;charset=utf-8");
        PrintWriter out = response.getWriter();
        out.print("{status:\"success\"}");
    }

    synchronized void stopConsumer(HttpServletRequest request, HttpServletResponse response, String queue) throws IOException {
        if (!queues.containsKey(queue)) {
            System.out.println("There is no consumer for queue: " + queue);
            response.sendError(503);
            return;
        }
        NotifyingConsumer consumer = queues.remove(queue);
        System.out.println("Stopping consumer for queue: " + queue);
        consumer.cancel();
        response.setContentType("text/json;charset=utf-8");
        PrintWriter out = response.getWriter();
        out.print("{status:\"success\"}");
    }

    synchronized void poll(HttpServletRequest request, HttpServletResponse response, String queue) throws IOException {
        if (!queues.containsKey(queue)) {
            System.out.println("[poll] There is consumer for queue: " + queue);
            response.sendError(503);
            return;
        }
        NotifyingConsumer consumer = queues.get(queue);
        String message = consumer.poll();
        if (message != null) {
            response.setContentType("text/json;charset=utf-8");
            PrintWriter out = response.getWriter();
            String body = String.format("{action:\"poll\", msg:\"%s\"}", message);
            out.print(body);
        } else {
            Continuation continuation = ContinuationSupport.getContinuation(request);
            if (continuation.isInitial()) {
                System.out.println("[poll] No messages in queue: " + queue);
                continuation.setTimeout(20000);
                continuation.suspend();
                consumer.setContinuation(continuation);
            } else {
                System.out.println("[poll] Timeout for queue: " + queue);
                consumer.setContinuation(null);
                response.setContentType("text/json;charset=utf-8");
                PrintWriter out = response.getWriter();
                out.print("{action:\"poll\"}");
            }
        }
    }
}
