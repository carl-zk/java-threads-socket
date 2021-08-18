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
@Plugin(name = "CustomConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(Integer.MAX_VALUE)
public class CustomConfigurationFactory extends ConfigurationFactory {

    static Configuration createConfiguration(final String name, ConfigurationBuilder<BuiltConfiguration> builder) {
        builder.setConfigurationName(name);
        builder.setStatusLevel(Level.WARN);
        //        builder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL).addAttribute("level", Level.INFO));
        // appenders
        AppenderComponentBuilder stdoutAppenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
        stdoutAppenderBuilder.add(builder.newLayout("PatternLayout").addAttribute("pattern", "%style{%date{DEFAULT}}{yellow} " +
                "%highlight{%-5level}{FATAL=bg_red, ERROR=red, WARN=yellow, INFO=green} " +
                "%d [%t] %-5level %logger{36}: %msg%n%throwable"));
        //        appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL).addAttribute("marker", "FLOW"));
        builder.add(stdoutAppenderBuilder);

        AppenderComponentBuilder JSONLogfileAppenderBuilder = builder.newAppender("JSONLogfileAppender", "RandomAccessFile")
                .addAttribute("fileName", "target/logfile.json")
                .addAttribute("immediateFlush", false)
                .addAttribute("append", false);
        JSONLogfileAppenderBuilder.add(builder.newLayout("PatternLayout").addAttribute("pattern", "%d [%t] %-5level %logger{36}: %msg%n%throwable"));
        builder.add(JSONLogfileAppenderBuilder);
        // loggers
        //        builder.add(builder.newLogger("org.apache.logging.log4j", Level.INFO).add(builder.newAppenderRef("Stdout")).addAttribute("additivity", false));
        builder.add(builder.newLogger("Stdout", Level.TRACE).add(builder.newAppenderRef("Stdout")).addAttribute("additivity", false));
        builder.add(builder.newAsyncLogger("jsonLogger", Level.INFO).add(builder.newAppenderRef("JSONLogfileAppender")).addAttribute("additivity", false));
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
