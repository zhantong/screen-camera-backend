import ch.qos.logback.core.ConsoleAppender;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

/**
 * Created by zhantong on 2016/11/3.
 */

public class ConfigureLogback {
    public static void configureLogbackDirectly(String logFilePath) {
        // reset the default context (which may already have been initialized)
        // since we want to reconfigure it
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.reset();

        // setup FileAppender
        PatternLayoutEncoder encoder1 = new PatternLayoutEncoder();
        encoder1.setContext(lc);
        encoder1.setPattern("%d [%thread] %level %logger [%marker] - %msg%n");
        encoder1.start();

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setContext(lc);
        fileAppender.setFile(logFilePath);
        fileAppender.setEncoder(encoder1);
        fileAppender.start();


        // add the newly created appenders to the root logger;
        // qualify Logger to disambiguate from org.slf4j.Logger
        ch.qos.logback.classic.Logger FILE_LOG = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(FILE_LOG.class);
        FILE_LOG.addAppender(fileAppender);

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<ILoggingEvent>();
        consoleAppender.setContext(lc);
        consoleAppender.setEncoder(encoder1);
        consoleAppender.start();

        ch.qos.logback.classic.Logger LOG = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LOG.class);
        LOG.addAppender(consoleAppender);
    }
}
