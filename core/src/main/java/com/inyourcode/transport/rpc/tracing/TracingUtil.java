/*
 * Copyright (c) 2016 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inyourcode.transport.rpc.tracing;


import com.inyourcode.common.util.JServiceLoader;
import com.inyourcode.common.util.JUnsafe;
import com.inyourcode.common.util.NetUtil;
import com.inyourcode.common.util.SystemClock;
import com.inyourcode.common.util.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.inyourcode.common.util.StackTraceUtil.stackTrace;

/**
 * 链路追踪ID生成的工具类.
 *
 * 一个 {@link TraceId} 包含以下内容(30位):
 * 1  ~ 8  位: 本机IP地址(16进制), 可能是网卡中第一个有效的IP地址
 * 9  ~ 21 位: 当前时间, 毫秒数
 * 22 ~ 25 位: 本地自增ID(1000 ~ 9191)
 * 26      位: d (进程flag)
 * 27 ~ 30 位: 当前进程ID(16进制)
 *
 * jupiter
 * tracing
 *
 * @author jiachun.fjc
 */
public class TracingUtil {

    private static final Logger logger = LoggerFactory.getLogger(TracingUtil.class);

    private static final boolean TRACING_NEEDED = SystemPropertyUtil.getBoolean("jupiter.tracing.needed", true);

    private static final TracingRecorder tracingRecorder = JServiceLoader.loadFirst(TracingRecorder.class);

    private static final ThreadLocal<TraceId> traceThreadLocal = new ThreadLocal<>();

    // maximal value for 64bit systems is 2^22.  See man 5 proc.
    private static final int MAX_PROCESS_ID = 4194304;
    private static final char PID_FLAG = 'd';
    private static final String IP_16;
    private static final String PID;
    private static final int ID_BASE = 1000;
    private static final int ID_MASK = (1 << 13) - 1;
    private static final AtomicInteger id = new AtomicInteger(0);

    static {
        String _ip_16;
        try {
            _ip_16 = getIP_16(SystemPropertyUtil.get("jupiter.local.address", NetUtil.getLocalAddress()));
        } catch (Exception e) {
            _ip_16 = "ffffffff";
        }
        IP_16 = _ip_16;

        String _pid;
        try {
            _pid = getHexProcessId(getProcessId());
        } catch (Exception e) {
            _pid = "0000";
        }
        PID = _pid;
    }

    public static boolean isTracingNeeded() {
        return TRACING_NEEDED;
    }

    public static String generateTraceId() {
        return getTraceId(IP_16, SystemClock.millisClock().now(), getNextId());
    }

    public static TraceId getCurrent() {
        TraceId traceId = null;
        if (TRACING_NEEDED) {
            traceId = traceThreadLocal.get();
        }
        return traceId != null ? traceId : TraceId.NULL_TRACE_ID;
    }

    public static void setCurrent(TraceId traceId) {
        if (traceId == null) {
            traceThreadLocal.remove();
        } else {
            traceThreadLocal.set(traceId);
        }
    }

    public static void clearCurrent() {
        traceThreadLocal.remove();
    }

    public static TracingRecorder getRecorder() {
        return tracingRecorder;
    }

    private static String getHexProcessId(int pid) {
        // unsigned short 0 to 65535
        if (pid < 0) {
            pid = 0;
        }
        if (pid > 65535) {
            String strPid = Integer.toString(pid);
            strPid = strPid.substring(strPid.length() - 4, strPid.length());
            pid = Integer.parseInt(strPid);
        }
        String str = Integer.toHexString(pid);
        while (str.length() < 4) {
            str = "0" + str;
        }
        return str;
    }

    /**
     * Gets current pid, max pid 32 bit systems 32768, for 64 bit 4194304
     * http://unix.stackexchange.com/questions/16883/what-is-the-maximum-value-of-the-pid-of-a-process
     * http://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id
     */
    private static int getProcessId() {
        String value;
        try {
            ClassLoader loader = JUnsafe.getSystemClassLoader();
            // invoke java.lang.management.ManagementFactory.getRuntimeMXBean().getName()
            Class<?> managementFactoryType = Class.forName("java.lang.management.ManagementFactory", true, loader);
            Class<?> runtimeMxBeanType = Class.forName("java.lang.management.RuntimeMXBean", true, loader);

            Method getRuntimeMXBean = managementFactoryType.getMethod("getRuntimeMXBean");
            Object bean = getRuntimeMXBean.invoke(null);
            Method getName = runtimeMxBeanType.getDeclaredMethod("getName");

            value = (String) getName.invoke(bean);
        } catch (Exception e) {
            logger.debug("Could not invoke ManagementFactory.getRuntimeMXBean().getName(), {}.", stackTrace(e));

            value = "";
        }
        int atIndex = value.indexOf('@');
        if (atIndex >= 0) {
            value = value.substring(0, atIndex);
        }

        int pid;
        try {
            pid = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // value did not contain an integer.
            pid = -1;
        }

        if (pid < 0 || pid > MAX_PROCESS_ID) {
            pid = ThreadLocalRandom.current().nextInt(MAX_PROCESS_ID + 1);

            logger.warn("Failed to find the current process ID from '{}'; using a random value: {}.",  value, pid);
        }

        return pid;
    }

    private static String getIP_16(String ip) {
        String[] segments = ip.split("\\.");
        StringBuilder buf = new StringBuilder();
        for (String s : segments) {
            String hex = Integer.toHexString(Integer.parseInt(s));
            if (hex.length() == 1) {
                buf.append('0');
            }
            buf.append(hex);
        }
        return buf.toString();
    }

    private static String getTraceId(String ip_16, long timestamp, int nextId) {
        StringBuilder buf = new StringBuilder()
                .append(ip_16)
                .append(timestamp)
                .append(nextId)
                .append(PID_FLAG)
                .append(PID);
        return buf.toString();
    }

    private static int getNextId() {
        return (id.getAndIncrement() & Integer.MAX_VALUE & ID_MASK) + ID_BASE;
    }
}
