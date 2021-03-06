package org.jetlang.web;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public interface HttpResponse {


    default SendResult sendResponse(int statusCode, String statusTxt, String contentType, Path resource, Charset charset) {
        try {
            final byte[] b = Files.readAllBytes(resource);
            return sendResponse(statusCode, statusTxt, contentType, b, charset);
        } catch (IOException failed) {
            throw new RuntimeException(failed);
        }
    }

    default SendResult sendResponse(int statusCode, String statusTxt, String contentType, String content, Charset ascii) {
        byte[] b = content.getBytes(ascii);
        return sendResponse(statusCode, statusTxt, contentType, b, ascii);
    }

    default SendResult sendResponse(int statusCode, String statusTxt, String contentType, byte[] content, Charset charset) {
        return sendResponse(statusCode, statusTxt, contentType, null, content, charset);
    }

    default SendResult sendResponse(int statusCode, String statusTxt, String contentType, KeyValueList headers, byte[] content, Charset charset) {
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(statusCode).append(' ').append(statusTxt).append("\r\n");
        response.append("Content-Type: ").append(contentType);
        if (charset != null) {
            response.append("; charset=").append(charset.name());
        }
        response.append("\r\n");

        if (headers != null) {
            headers.appendTo(response);
        }
        response.append("Content-Length: ").append(content.length).append("\r\n\r\n");
        byte[] header = response.toString().getBytes(HeaderReader.ascii);
        ByteBuffer bb = ByteBuffer.allocate(header.length + content.length);
        bb.put(header);
        bb.put(content);
        bb.flip();
        return send(bb);
    }

    SocketAddress getRemoteAddress();

    SendResult send(ByteBuffer fullResponse);

    default void sendWebsocketHandshake(String reply, KeyValueList additionalHeaders) {
        StringBuilder handshake = new StringBuilder("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: ");
        handshake.append(reply).append("\r\n");
        if (additionalHeaders != null) {
            additionalHeaders.appendTo(handshake);
        }
        handshake.append("\r\n");
        send(ByteBuffer.wrap(handshake.toString().getBytes(HeaderReader.ascii)));
    }

    class Default implements HttpResponse {
        private final NioWriter writer;

        public Default(NioWriter writer) {
            this.writer = writer;
        }

        public SendResult send(ByteBuffer fullResponse) {
            return writer.send(fullResponse);
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return writer.getRemoteAddress();
        }
    }

    class Decorator implements HttpResponse {

        private final HttpResponse target;

        public Decorator(HttpResponse target) {
            this.target = target;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return target.getRemoteAddress();
        }

        @Override
        public SendResult send(ByteBuffer fullResponse) {
            return target.send(fullResponse);
        }
    }

}
