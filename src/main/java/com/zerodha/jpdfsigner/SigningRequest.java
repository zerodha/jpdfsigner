package com.zerodha.jpdfsigner;

import com.google.gson.Gson;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.concurrent.ExecutorService;

public class SigningRequest {

    private static final String CONTENT_TYPE = "text/plain; charset=UTF-8";
    private static final String METHOD_NOT_ALLOWED = "Method not allowed";

    private final PrivateKey key;
    private final Certificate[] chain;
    private final String reason;
    private final String contact;
    private final String location;
    private final Rectangle rect;
    private final OpenPdfSigner app;
    private final Font font;
    private final int page;
    private final ExecutorService executor;

    public SigningRequest(
        PrivateKey key,
        Certificate[] chain,
        String reason,
        String contact,
        String location,
        Rectangle rect,
        OpenPdfSigner app,
        Font font,
        int page,
        ExecutorService executor
    ) {
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

    public static void sendResponse(
        String response,
        int code,
        HttpServerExchange exchange
    ) {
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, CONTENT_TYPE);
        exchange.setStatusCode(code);
        exchange.getResponseSender().send(response);
    }

    public void handleRequestWithMeta(HttpServerExchange httpExchange) {
        if (!httpExchange.getRequestMethod().equals(Methods.POST)) {
            sendResponse(
                METHOD_NOT_ALLOWED,
                StatusCodes.METHOD_NOT_ALLOWED,
                httpExchange
            );
            return;
        }

        if (httpExchange.isInIoThread()) {
            httpExchange.dispatch(executor, () ->
                handleRequestWithMeta(httpExchange)
            );
            return;
        }

        httpExchange.startBlocking();
        try {
            String requestBody = readInputStream(httpExchange.getInputStream());
            processRequest(requestBody, httpExchange);
        } catch (IOException e) {
            System.err.println("Error reading request body: " + e.getMessage());
            sendResponse(
                "Error reading request",
                StatusCodes.INTERNAL_SERVER_ERROR,
                httpExchange
            );
        }
    }

    private String readInputStream(InputStream inputStream) throws IOException {
        try (inputStream) {
            return new String(
                inputStream.readAllBytes(),
                StandardCharsets.UTF_8
            );
        }
    }

    private void processRequest(
        String requestBody,
        HttpServerExchange httpExchange
    ) {
        try {
            Request req = new Gson().fromJson(requestBody, Request.class);
            SignParams params = createSignParams(req);
            app.sign(params);
            System.out.println(
                "Signing file at src: " +
                req.getInputFile() +
                " to output: " +
                req.getOutputFile()
            );
            sendResponse("", StatusCodes.OK, httpExchange);
        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
            sendResponse(
                e.toString(),
                StatusCodes.INTERNAL_SERVER_ERROR,
                httpExchange
            );
        }
    }

    private SignParams createSignParams(Request req) {
        SignParams params = new SignParams();
        params.setSrc(req.getInputFile());
        params.setDest(req.getOutputFile());
        params.setPassword(req.getPassword());
        params.setContact(
            (req.getContact() != null && !req.getContact().isBlank())
                ? req.getContact()
                : contact
        );
        params.setLocation(
            (req.getLocation() != null && !req.getLocation().isBlank())
                ? req.getLocation()
                : location
        );
        params.setReason(
            (req.getReason() != null && !req.getReason().isBlank())
                ? req.getReason()
                : reason
        );
        params.setChain(chain);
        params.setKey(key);

        // Use custom coordinates from request if provided and valid
        if (req.getCoordinates() != null && req.getCoordinates().isValid()) {
            params.setRect(new Rectangle(
                req.getCoordinates().getX1(),
                req.getCoordinates().getY1(),
                req.getCoordinates().getX2(),
                req.getCoordinates().getY2()
            ));
        } else {
            // Use default coordinates from config
            params.setRect(rect);
        }

        params.setFont(font);

        // Use custom page from request if provided, otherwise use default from config
        params.setPage(req.getPage() != null ? req.getPage() : page);

        return params;
    }
}
