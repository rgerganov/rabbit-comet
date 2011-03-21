package com.xakcop;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.eclipse.jetty.continuation.Continuation;

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
            String message = new String(body, 0, body.length, "UTF-8");
            Pattern pattern = Pattern.compile("type=\"(.+?)\"");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                messages.add(matcher.group(1));
            } else {
                messages.add(message);
            }
            if (continuation != null && continuation.isSuspended()) {
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