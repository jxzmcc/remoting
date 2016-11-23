package org.jetlang.web;

import org.jetlang.fibers.NioFiber;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class WebServerConfigBuilder<S> {

    private final SessionFactory<S> factory;
    private Charset websocketCharset = Charset.forName("UTF-8");
    private List<Consumer<Map<String, Handler<S>>>> events = new ArrayList<>();
    private int readBufferSizeInBytes = 1024;
    private int maxReadLoops = 50;
    private RequestDecorator<S> decorator = new RequestDecorator<S>() {
        @Override
        public HttpRequestHandler<S> decorate(HttpRequestHandler<S> handler) {
            return handler;
        }
    };
    private Handler<S> defaultHandler = new Handler<S>() {
        @Override
        public NioReader.State start(HttpRequest headers, HeaderReader<S> reader, NioWriter writer, S sessionState) {
            reader.getHttpResponseWriter().sendResponse("404 Not Found", "text/plain", headers.getRequestUri() + " Not Found", HeaderReader.ascii);
            return reader.start();
        }
    };

    public WebServerConfigBuilder(SessionFactory<S> factory) {
        this.factory = factory;
    }

    public RequestDecorator<S> getDecorator() {
        return decorator;
    }

    public void setDecorator(RequestDecorator<S> decorator) {
        this.decorator = decorator;
    }

    public Handler<S> getDefaultHandler() {
        return defaultHandler;
    }

    public void setDefaultHandler(Handler<S> defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    public int getMaxReadLoops() {
        return maxReadLoops;
    }

    public void setMaxReadLoops(int maxReadLoops) {
        this.maxReadLoops = maxReadLoops;
    }

    public int getReadBufferSizeInBytes() {
        return readBufferSizeInBytes;
    }

    public void setReadBufferSizeInBytes(int readBufferSizeInBytes) {
        this.readBufferSizeInBytes = readBufferSizeInBytes;
    }

    public Charset getWebsocketCharset() {
        return websocketCharset;
    }

    public WebServerConfigBuilder setWebsocketCharset(Charset websocketCharset) {
        this.websocketCharset = websocketCharset;
        return this;
    }

    public <T> WebServerConfigBuilder<S> add(String path, WebSocketHandler<S, T> handler) {
        events.add((map) -> {
            map.put(path, new WebSocketRequestHandler<>(websocketCharset, handler));
        });
        return this;
    }

    public WebServerConfigBuilder<S> add(String path, HttpHandler<S> rs) {
        events.add((map) -> {
            map.put(path, rs);
        });
        return this;
    }

    public interface RequestDecorator<S> {

        HttpRequestHandler<S> decorate(HttpRequestHandler<S> handler);
    }

    public WebDispatcher<S> create(NioFiber readFiber) {
        Map<String, Handler<S>> handlerMap = new HashMap<>();
        for (Consumer<Map<String, Handler<S>>> event : events) {
            event.accept(handlerMap);
        }
        HttpRequestHandler<S> handler = decorator.decorate(createHandler(handlerMap));
        return new WebDispatcher<>(readFiber, handler, readBufferSizeInBytes, maxReadLoops, factory);
    }

    protected HttpRequestHandler<S> createHandler(final Map<String, Handler<S>> handlerMap) {
        return new HttpRequestHandler.Default<>(handlerMap, defaultHandler);
    }
}
