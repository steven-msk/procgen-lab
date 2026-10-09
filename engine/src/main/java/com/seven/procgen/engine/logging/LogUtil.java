package com.seven.procgen.engine.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogUtil {
    public static Logger GetLogger() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        return LoggerFactory.getLogger(walker.getCallerClass());
    }
}
