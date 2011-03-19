package com.xakcop;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

public class ContTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final Continuation continuation = ContinuationSupport.getContinuation(req);
        if (continuation.isInitial()) {
            continuation.setTimeout(4000);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(6000);
                        continuation.resume();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            continuation.suspend();
            return;
        } else {
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();
            out.println("continuation timed out");
            out.flush();
        }
    }

}
