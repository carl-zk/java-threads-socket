package com.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;

import java.net.URI;

/**
 * @author carl
 */
@Plugin(name = "Log4jConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(Integer.MAX_VALUE)
public class Log4jConfigurationFactory extends ConfigurationFactory {

    static Configuration createConfiguration(final String name, ConfigurationBuilder<BuiltConfiguration> builder) {
        builder.setConfigurationName(name);
        builder.setStatusLevel(Level.WARN);
        //        builder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL).addAttribute("level", Level.INFO));
        // appenders
        /**
         * stdout
         */
        AppenderComponentBuilder stdoutAppenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
        stdoutAppenderBuilder.add(builder.newLayout("PatternLayout").addAttribute("pattern", "%style{%date{DEFAULT}}{yellow} " +
                "%highlight{%-5level}{FATAL=bg_red, ERROR=red, WARN=yellow, INFO=green} " +
                "%d [%t] %-5level %logger{36}: %msg%n%throwable"));
        //        appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL).addAttribute("marker", "FLOW"));
        builder.add(stdoutAppenderBuilder);

        /**
         * async
         */
        AppenderComponentBuilder asyncAppenderBuilder = builder.newAppender("asyncAppender", "RandomAccessFile")
                .addAttribute("fileName", "target/async.log")
                .addAttribute("immediateFlush", false)
                .addAttribute("append", false);
        asyncAppenderBuilder.add(builder.newLayout("PatternLayout").addAttribute("pattern", "%d [%t] %-5level %logger{36}: %msg%n%throwable"));
        builder.add(asyncAppenderBuilder);

        /**
         * rolling
         */
        AppenderComponentBuilder rollingFileAppenderBuilder = builder.newAppender("rollingFileAppender", "RollingFile")
                .addAttribute("fileName", "target/rolling.log")
                .addAttribute("filePattern", "target/rolling-%d{MM-dd-yy}.log.gz")
                .add(builder.newLayout("PatternLayout").addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable"));
        ComponentBuilder triggeringPolicies = builder.newComponent("Policies")
                .addComponent(builder.newComponent("CronTriggeringPolicy")
                        .addAttribute("schedule", "0 0 0 * * ?"))
                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy")
                        .addAttribute("size", "100M"));
        rollingFileAppenderBuilder.addComponent(triggeringPolicies);
        builder.add(rollingFileAppenderBuilder);

        /**
         * file
         */
        AppenderComponentBuilder fileAppenderBuilder = builder.newAppender("fileAppender", "File")
                .addAttribute("fileName", "target/file.log")
                .add(builder.newLayout("PatternLayout").addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable"));
        builder.add(fileAppenderBuilder);
        // loggers
        //        builder.add(builder.newLogger("org.apache.logging.log4j", Level.INFO).add(builder.newAppenderRef("Stdout")).addAttribute("additivity", false));
        builder.add(builder.newLogger("Stdout", Level.TRACE).add(builder.newAppenderRef("Stdout")).addAttribute("additivity", false));
        builder.add(builder.newAsyncLogger("asyncLogger", Level.INFO).add(builder.newAppenderRef("asyncAppender")).addAttribute("additivity", false));
        builder.add(builder.newLogger("rollingFileLogger", Level.INFO).add(builder.newAppenderRef("rollingFileAppender")).addAttribute("additivity", false));
        builder.add(builder.newLogger("fileLogger", Level.INFO).add(builder.newAppenderRef("fileAppender")).addAttribute("additivity", false));
        builder.add(builder.newRootLogger(Level.INFO).add(builder.newAppenderRef("Stdout")));
        return builder.build();
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        return getConfiguration(loggerContext, source.toString(), null);
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation) {
        ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();
        return createConfiguration(name, builder);
    }

    @Override
    protected String[] getSupportedTypes() {
        return new String[]{"*"};
    }
}
