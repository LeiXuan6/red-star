
package com.inyourcode.transport.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.inyourcode.common.util.AjaxResult;
import com.inyourcode.common.util.JConstants;
import com.inyourcode.transport.api.HttpRequestHandler;
import com.inyourcode.transport.api.JConfig;
import com.inyourcode.transport.api.JConfigGroup;
import com.inyourcode.transport.api.JOption;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyHttpAcceptor extends NettyAcceptor {

    private static final Logger logger = LoggerFactory.getLogger(NettyTcpAcceptor.class);
    private final boolean nativeEt; // Use native epoll ET
    private final NettyConfig.NettyTcpConfigGroup configGroup = new NettyConfig.NettyTcpConfigGroup();
    private HttpRequestHandler httpRequestHandler;

    public NettyHttpAcceptor(int port, HttpRequestHandler httpRequestHandler) {
        super(Protocol.HTTP, new InetSocketAddress(port));
        nativeEt = true;
        this.httpRequestHandler = httpRequestHandler;
        init();
    }

    @Override
    protected void init() {
        super.init();

        // parent options
        JConfig parent = configGroup().parent();
        parent.setOption(JOption.SO_BACKLOG, 32768);
        parent.setOption(JOption.SO_REUSEADDR, true);

        // child options
        JConfig child = configGroup().child();
        child.setOption(JOption.SO_REUSEADDR, true);
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        ServerBootstrap boot = bootstrap();

        if (isNativeEt()) {
            boot.channelFactory(TcpChannelProvider.NATIVE_ACCEPTOR);
        } else {
            boot.channelFactory(TcpChannelProvider.NIO_ACCEPTOR);
        }

        HttpServerInitializer channelInitializer = new HttpServerInitializer(httpRequestHandler);
        boot.childHandler(channelInitializer);
        setOptions();

        return boot.bind(localAddress);
    }

    @Override
    protected void setOptions() {
        super.setOptions();

        ServerBootstrap boot = bootstrap();

        // parent options
        NettyConfig.NettyTcpConfigGroup.ParentConfig parent = configGroup.parent();
        boot.option(ChannelOption.SO_BACKLOG, parent.getBacklog());
        boot.option(ChannelOption.SO_REUSEADDR, parent.isReuseAddress());
        if (parent.getRcvBuf() > 0) {
            boot.option(ChannelOption.SO_RCVBUF, parent.getRcvBuf());
        }

        // child options
        NettyConfig.NettyTcpConfigGroup.ChildConfig child = configGroup.child();
        boot.childOption(ChannelOption.SO_REUSEADDR, child.isReuseAddress())
                .childOption(ChannelOption.SO_KEEPALIVE, child.isKeepAlive())
                .childOption(ChannelOption.TCP_NODELAY, child.isTcpNoDelay())
                .childOption(ChannelOption.ALLOW_HALF_CLOSURE, child.isAllowHalfClosure());
        if (child.getRcvBuf() > 0) {
            boot.childOption(ChannelOption.SO_RCVBUF, child.getRcvBuf());
        }
        if (child.getSndBuf() > 0) {
            boot.childOption(ChannelOption.SO_SNDBUF, child.getSndBuf());
        }
        if (child.getLinger() > 0) {
            boot.childOption(ChannelOption.SO_LINGER, child.getLinger());
        }
        if (child.getIpTos() > 0) {
            boot.childOption(ChannelOption.IP_TOS, child.getIpTos());
        }
        int bufLowWaterMark = child.getWriteBufferLowWaterMark();
        int bufHighWaterMark = child.getWriteBufferHighWaterMark();
        if (bufLowWaterMark >= 0 && bufHighWaterMark > 0) {
            WriteBufferWaterMark waterMark = new WriteBufferWaterMark(bufLowWaterMark, bufHighWaterMark);
            boot.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark);
        }
    }

    @Override
    public JConfigGroup configGroup() {
        return configGroup;
    }

    @Override
    public void start() throws InterruptedException {
        start(true);
    }

    @Override
    public void start(boolean sync) throws InterruptedException {
        // wait until the server socket is bind succeed.
        ChannelFuture future = bind(localAddress).sync();

        logger.info("Jupiter TCP server start" + (sync ? ", and waits until the server socket closed." : ".")
                + JConstants.NEWLINE + " {}.", toString());

        if (sync) {
            // wait until the server socket is closed.
            future.channel().closeFuture().sync();
        }
    }

    @Override
    public void setIoRatio(int bossIoRatio, int workerIoRatio) {
        EventLoopGroup boss = boss();
        if (boss instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) boss).setIoRatio(bossIoRatio);
        } else if (boss instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) boss).setIoRatio(bossIoRatio);
        }

        EventLoopGroup worker = worker();
        if (worker instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) worker).setIoRatio(workerIoRatio);
        } else if (worker instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) worker).setIoRatio(workerIoRatio);
        }
    }

    @Override
    protected EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory) {
        return isNativeEt() ? new EpollEventLoopGroup(nThreads, tFactory) : new NioEventLoopGroup(nThreads, tFactory);
    }

    /**
     * Netty provides the native socket transport for Linux using JNI based on Epoll Edge Triggered(ET).
     */
    public boolean isNativeEt() {
        return nativeEt && NativeSupport.isSupportNativeET();
    }

    @Override
    public String toString() {
        return "Socket address:[" + localAddress + ']' + ", nativeET: " + isNativeEt()
                + JConstants.NEWLINE + bootstrap();
    }

    static class HttpServerInitializer extends ChannelInitializer<SocketChannel> {
        private HttpRequestHandler httpRequestHandler;

        public HttpServerInitializer(HttpRequestHandler httpRequestHandler) {
            this.httpRequestHandler = httpRequestHandler;
        }

        @Override
        public void initChannel(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();
            p.addLast(new HttpServerCodec());
            p.addLast(new HttpObjectAggregator(65536));
            p.addLast(new HttpServerHandler(httpRequestHandler));
        }
    }

    static class HttpServerHandler extends ChannelInboundHandlerAdapter {
        private static final Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);
        private static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");
        private static final AsciiString CONTENT_LENGTH = new AsciiString("Content-Length");
        private static final AsciiString CONNECTION = new AsciiString("Connection");
        private static final AsciiString KEEP_ALIVE = new AsciiString("keep-alive");
        private HttpRequestHandler httpRequestHandler;

        public HttpServerHandler(HttpRequestHandler httpRequestHandler) {
            this.httpRequestHandler = httpRequestHandler;
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
            if (msg instanceof FullHttpRequest) {
                FullHttpRequest req = (FullHttpRequest) msg;
                if (HttpUtil.is100ContinueExpected(req)) {
                    ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
                }

                HttpMethod requestMethod = req.method();
                String uri = req.uri();
                if (!httpRequestHandler.intercept(uri)) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("the request was not intercepted, uri = {}", uri);
                    }
                    return;
                }

                Map<String, String> paramMap = parseRequestParam(req);
                if (logger.isDebugEnabled()) {
                    logger.debug("recive http request, uri = {}, method = {}, content = {}", uri, requestMethod, JSONObject.toJSON(paramMap));
                }

                String action = uri.split("\\?")[0].substring(1);
                AjaxResult ajaxResult = httpRequestHandler.handle(action, requestMethod, paramMap);

                boolean keepAlive = HttpUtil.isKeepAlive(req);
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(JSONObject.toJSONString(ajaxResult).getBytes(Charset.forName("UTF-8"))));
                response.headers().set(CONTENT_TYPE, "application/json");
                response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());

                if (!keepAlive) {
                    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                } else {
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                    ctx.write(response);
                }
            } else {
                logger.error("the data type cannot be processed, type:{}", msg.getClass());
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }

        private Map<String, String> parseRequestParam(FullHttpRequest fullReq) {
            HttpMethod method = fullReq.method();
            Map<String, String> parmMap = new HashMap<>();
            if (HttpMethod.GET.equals(method)) {
                QueryStringDecoder decoder = new QueryStringDecoder(fullReq.uri());
                Set<Map.Entry<String, List<String>>> entries = decoder.parameters().entrySet();
                for (Map.Entry<String, List<String>> entry : entries) {
                    parmMap.put(entry.getKey(), entry.getValue().get(0));
                }
            } else if (HttpMethod.POST.equals(method)) {
                String contentType = getContentType(fullReq.headers());
                if (contentType.equals("application/json") || contentType.equals("text/plain")) {
                    String jsonStr = fullReq.content().toString(Charset.forName("utf-8"));
                    parmMap = JSON.parseObject(jsonStr, Map.class);
                } else {
                    logger.error("the content type cannot parsed, type = {}", contentType);
                }
            } else {
                logger.error("the method type cannot parsed, type = {}", method);
            }

            return parmMap;
        }

        private String getContentType(HttpHeaders headers) {
            String contentType = headers.get("Content-Type").toString();
            String[] list = contentType.split(";");
            return list[0];
        }
    }

}