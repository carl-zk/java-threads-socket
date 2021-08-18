package com.config;

import org.apache.logging.log4j.core.async.AsyncLogger;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.async.AsyncLoggerContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void givenLoggerWithAsyncConfig_whenLogToJsonFile_thanOK() throws Exception {
        Files.deleteIfExists(Paths.get("target/logfile.json"));
        Files.createFile(Paths.get("target/logfile.json"));
        Logger logger = LoggerFactory.getLogger("jsonLogger");

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
}
