package com.xakcop;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jetty.continuation.Continuation;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class NotifyingConsumer extends DefaultConsumer {

    String queue;
    List<String> messages;
    Continuation continuation;

    public NotifyingConsumer(Channel channel, String queue) {
        super(channel);
        this.messages = new LinkedList<String>();
        this.queue = queue;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
            throws IOException {
        synchronized (this) {
            messages.add(new String(body, 0, body.length, "UTF-8"));
            if (continuation != null) {
                continuation.resume();
            }
        }
    }

    synchronized public String poll() {
        if (messages.size() > 0) {
            return messages.remove(0);
        } else {
            return null;
        }
    }

    public void start() throws IOException {
        getChannel().basicConsume(queue, true, this);
    }

    public void cancel() throws IOException {
        Channel channel = getChannel();
        channel.basicCancel(getConsumerTag());
        channel.close();
    }

    synchronized public void setContinuation(Continuation continuation) {
        this.continuation = continuation;
    }
}