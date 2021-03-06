/*
 * Copyright (c) 2015 The Jupiter Project
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

package com.inyourcode.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class StackTraceUtil {

    public static String stackTrace(Throwable t) {
        if (t == null) {
            return "null";
        }

        PrintWriter pw = null;
        try {
            StringWriter sw = new StringWriter();
            pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            return sw.toString();
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    private StackTraceUtil() {}
}
