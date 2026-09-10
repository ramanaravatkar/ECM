package com.esun.fn.constants;

import org.apache.logging.log4j.Logger;
public class LoggerUtil {

    public static final Logger migrationLogger =
            Logger.getLogger(LoggerUtil.class);

    public static final Logger successLogger =
            Logger.getLogger("SUCCESS");

    public static final Logger errorLogger =
            Logger.getLogger("ERRORLOGGER");

}