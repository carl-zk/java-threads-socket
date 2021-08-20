package com.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author carl
 */
public class Log4jTest {

    @Test
    void givenStdoutLogger_whenLogTrace_thenOK() {
        Logger logger = LoggerFactory.getLogger("Stdout");
        logger.trace("trace");
        assertTrue(logger.isTraceEnabled());
    }

    @Test
    void givenRootLogger_whenLogTrace_thenNotOK() {
        Logger logger = LoggerFactory.getLogger(Log4jTest.class);
        logger.trace("trace");
        logger.info("info");
        logger.warn("warn");
        logger.error("error");
        assertFalse(logger.isTraceEnabled());
    }

    @Test
    public void givenAsyncLogger_whenLogMany_thenNotFlushDirectly() throws Exception {
        Files.deleteIfExists(Paths.get("target/async.log"));
        Files.createFile(Paths.get("target/async.log"));
        Logger logger = LoggerFactory.getLogger("asyncLogger");

        final int count = 88;
        for (int i = 0; i < count; i++) {
            logger.info("This is async JSON message #{}", i);
        }

        //        AsyncLoggerContext.getContext().stop(5, TimeUnit.SECONDS);
        //        AsyncLoggerContext.getContext().terminate();
        TimeUnit.SECONDS.sleep(1);

        long logEventsCount = Files.lines(Paths.get("target/logfile.json")).count();
        assertTrue(logEventsCount > 0 && logEventsCount <= count);
    }

    @Test
    void givenLoggerRolling_whenLog_thenFlushDirectly() throws IOException {
        Files.deleteIfExists(Paths.get("target/rolling.log"));
        Logger logger = LoggerFactory.getLogger("rollingFileLogger");
        logger.debug("debug");
        logger.info("info");
        logger.error("error");
        long logEventsCount = Files.lines(Paths.get("target/rolling.log")).count();
        assertTrue(logEventsCount == 2);
    }

    @Test
    void givenRunningContext_whenChangeLogger_thenRefresh() {
        Logger logger = LoggerFactory.getLogger("file2Logger");
        logger.info("this message won't be logged in file");
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        // @formatter:off
        FileAppender appender = FileAppender.newBuilder().withFileName("target/dynamic.log").withAppend(false)
                .withLocking(false).withImmediateFlush(true)
                .withBufferedIo(false).withBufferSize(4000).withAdvertise(false)
                .setName("File2")
                .setIgnoreExceptions(false)
                .setConfiguration(config)
                .setLayout(PatternLayout.createDefaultLayout(config))
                .build();
        // @formatter:on
        appender.start();
        config.addAppender(appender);
        AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);
        AppenderRef[] refs = new AppenderRef[]{ref};
        LoggerConfig loggerConfig = LoggerConfig.createLogger(false, Level.INFO, "file2Logger", "true", refs, null, config, null);
        loggerConfig.addAppender(appender, null, null);
        config.addLogger("file2Logger", loggerConfig);
        ctx.updateLoggers();

        logger.info("this message will be logged");
        LoggerFactory.getLogger("fileLogger").info("other loggers still exist");
    }
}
