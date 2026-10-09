package com.seven.procgen.engine.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class LogUtil {
    public static Logger GetLogger() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        String className = walker.getCallerClass().getSimpleName();
        return LogManager.getLogger(className);
    }
}
