package com.zerodha.jpdfsigner;

import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

// SigningRequest handles the request from the http server to sign.
public class SigningRequest {
    PrivateKey key;
    Certificate[] chain;
    String reason;
    String contact;
    String location;
    Rectangle rect;
    OpenPdfSigner app;
    Font font;
    int page;
    // Thread pool executor.
    ExecutorService executor;

    public SigningRequest(PrivateKey key, Certificate[] chain, String reason, String contact, String location,
            Rectangle rect, OpenPdfSigner app, Font font, int page, ExecutorService executor) {
        this.key = key;
        this.chain = chain;
        this.reason = reason;
        this.contact = contact;
        this.location = location;
        this.rect = rect;
        this.app = app;
        this.font = font;
        this.page = page;
        this.executor = executor;
    }

    // sendResponse sends the response with the given status code.
    public static void sendResponse(String response, int code, HttpServerExchange exchange) {
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/plain; charset=UTF-8");
        exchange.setStatusCode(code);
        exchange.getResponseSender().send(response);
    }

    public void handleRequestWithMeta(HttpServerExchange httpExchange) throws Exception {
        if (httpExchange.getRequestMethod().equals(Methods.POST)) {
            // If this is in a NIO thread, we dispatch this to the executor,
            // since we have to do blocking io.
            if (httpExchange.isInIoThread()) {
                httpExchange.dispatch(executor, this::handleRequestWithMeta);
                return;
            }
            httpExchange.startBlocking();
            StringBuilder sb = new StringBuilder();
            InputStream ios = httpExchange.getInputStream();
            int i;
            while ((i = ios.read()) != -1) {
                sb.append((char) i);
            }

            try {
                System.out.println(sb);
                Gson gson = new Gson();
                Request req = gson.fromJson(sb.toString(), Request.class);
                System.out.println(req.getInputFile());
                String inFile = req.getInputFile();
                String outFile = req.getOutputFile();
                String pwd = req.getPassword();
                String loc = StringUtils.defaultIfEmpty(req.getLocation(), location);
                String cont = StringUtils.defaultIfEmpty(req.getContact(), contact);
                String rsn = StringUtils.defaultIfEmpty(req.getReason(), reason);

                this.app.sign(inFile, outFile, pwd, key, chain, rsn,
                        cont,
                        loc, rect, font, page);
                System.out.println("signing file at src: " + inFile + "to output: " + outFile);
                sendResponse("", 200, httpExchange);
                return;
            } catch (Exception e) {
                sendResponse(e.toString(), 500, httpExchange);
                return;
            }
        } else {
            sendResponse("Method not allowed", StatusCodes.INTERNAL_SERVER_ERROR, httpExchange);
            return;
        }
    }
}
