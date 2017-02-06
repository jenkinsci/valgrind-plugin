package org.jenkinsci.plugins.valgrind.util;

import hudson.model.TaskListener;

import java.util.logging.Logger;

public abstract class ValgrindLogger
{
    private static final Logger LOGGER = Logger.getLogger(ValgrindLogger.class.getSimpleName());

    public static void log(TaskListener listener, final String message)
	{
		listener.getLogger().println("[Valgrind] " + message);
	}

    public static void logFine(final String message)
    {
        LOGGER.fine(message);
    }

    public static void logWarn(final String message)
    {
        LOGGER.warning(message);
    }
}
