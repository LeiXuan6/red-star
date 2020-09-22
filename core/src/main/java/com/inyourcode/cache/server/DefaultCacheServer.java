package com.inyourcode.cache.server;

import com.inyourcode.cache.provider.RemoteCacheProviderIml;
import com.inyourcode.common.util.StackTraceUtil;
import com.inyourcode.transport.netty.JNettyTcpAcceptor;
import com.inyourcode.transport.rpc.DefaultServer;
import com.inyourcode.transport.rpc.JServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultCacheServer {
    private static final Logger logger = LoggerFactory.getLogger(DefaultCacheServer.class);
    private int port;
    private JServer server;

    public DefaultCacheServer(int port) {
        this.port = port;
        this.server = new DefaultServer().withAcceptor(new JNettyTcpAcceptor(port));
    }

    public void start() {
        server.serviceRegistry()
                .provider(new RemoteCacheProviderIml())
                .register();
        try {
            server.start();
        } catch (InterruptedException e) {
            logger.error("cache server start error", StackTraceUtil.stackTrace(e));
        }
    }
}
