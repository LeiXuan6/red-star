package com.inyourcode.transport.session;

import com.inyourcode.common.util.StackTraceUtil;
import com.inyourcode.serialization.api.Serializer;
import com.inyourcode.serialization.api.SerializerFactory;
import com.inyourcode.transport.api.Status;
import com.inyourcode.transport.api.channel.JChannel;
import com.inyourcode.transport.api.payload.JRequestBytes;
import com.inyourcode.transport.api.processor.ProviderProcessor;
import com.inyourcode.transport.netty.channel.NettyChannel;
import com.inyourcode.transport.session.api.AsyncRequest;
import com.inyourcode.transport.session.api.FixedRequest;
import com.inyourcode.transport.session.api.ISessionFactory;
import com.inyourcode.transport.session.api.Session;
import com.inyourcode.transport.session.threads.AsyncExecutorPool;
import com.inyourcode.transport.session.threads.FixedExecutor;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * <ul>
 *     消费者抽象的实现
 *     <li>将字节消息转成java对象，然后分发到模块</li>
 *     <li>默认是在netty io线程处理</li>
 *     <li>通过使用注解{@link AsyncRequest} 与 {@link FixedRequest}，可以让将消息投递到指定的线程池</li>
 * </ul>
 *
 * @author JackLei
 **/
public class BasicSessionProcessor implements ProviderProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(BasicSessionProcessor.class);
    private ISessionFactory sessionFactory;

    public BasicSessionProcessor(ISessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void handleRequest(JChannel channel, JRequestBytes requestPayload) throws Exception {
        byte s_code = requestPayload.serializerCode();
        long invokeId = requestPayload.invokeId();
        Serializer serializer = SerializerFactory.getSerializer(s_code);
        byte[] bytes = requestPayload.bytes();

        if (serializer == null) {
            logger.error("The serializer could not be found when type = {}", s_code);
            return;
        }
        SessionHandlerScanner.ProcesserWrapper processerWapper = SessionHandlerScanner.getProcesserWapper(invokeId);
        if (processerWapper == null) {
            logger.error("The ProcesserWrapper could not be found when invokeId = {}", invokeId);
            return;
        }

        Session session = sessionFactory.get((NettyChannel) channel);
        Object message = serializer.readObject(bytes, processerWapper.getMessageClazz());
        invoke(processerWapper, message, session);
    }

    @Override
    public void handleException(JChannel channel, JRequestBytes request, Status status, Throwable cause) {
        logger.error("handle exception,channel = {},exception = {}", channel, StackTraceUtil.stackTrace(cause));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        sessionFactory.create(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessionFactory.remove(ctx.channel());
    }

    protected void invoke(SessionHandlerScanner.ProcesserWrapper processerWrapper, Object message, Session session) {
        try {
            if (processerWrapper.isNoAuthReq()) {
                FixedExecutor.execute(session.hashCode(), new Message(processerWrapper, message, session));
                return;
            }

            if (!session.auth()) {
                //TODO close连接
                logger.error("session is forbid, channel = {}", session.channel());
                return;
            }

            if (processerWrapper.isAsyncReq()) {
                AsyncExecutorPool.execute(new Message(processerWrapper, message, session));
                return;
            }

            if (processerWrapper.isFixedReq()) {
                FixedExecutor.execute(session.hashCode(), new Message(processerWrapper, message, session));
                return;
            }

            FixedExecutor.execute(session.hashCode(), new Message(processerWrapper, message, session));
        } catch (Exception ex) {
            logger.error("network reqeust processing failed,{}", StackTraceUtil.stackTrace(ex));
        }
    }

    class Message implements Runnable {
        private SessionHandlerScanner.ProcesserWrapper processerWrapper;
        private Object message;
        private Session session;

        public Message(SessionHandlerScanner.ProcesserWrapper processerWrapper, Object message, Session session) {
            this.processerWrapper = processerWrapper;
            this.message = message;
            this.session = session;
        }

        @Override
        public void run() {
            try {
                processerWrapper.invoke(session, message);
            } catch (InvocationTargetException e) {
                logger.error("session messge invoke failed, {}", StackTraceUtil.stackTrace(e));
            } catch (IllegalAccessException e) {
                logger.error("session messge invoke failed, {}", StackTraceUtil.stackTrace(e));
            }
        }
    }

}
