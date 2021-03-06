package org.springframework.roo.felix;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Provides levels for the Felix "log" command.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class LogLevel implements Comparable<LogLevel> {

    private final String key;
    private final String felixCode;

    public static final LogLevel ERROR = new LogLevel("ERROR", "error");
    public static final LogLevel WARNING = new LogLevel("WARNING", "warn");
    public static final LogLevel INFORMATION = new LogLevel("INFORMATION",
            "info");
    public static final LogLevel DEBUG = new LogLevel("DEBUG", "debug");

    public LogLevel(final String key, final String felixCode) {
        Assert.hasText(key, "Key required");
        Assert.hasText(felixCode, "Felix code required");
        this.key = key;
        this.felixCode = felixCode;
    }

    public String getFelixCode() {
        return felixCode;
    }

    public String getKey() {
        return key;
    }

    @Override
    public final int hashCode() {
        return this.key.hashCode() * this.felixCode.hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
        return obj instanceof LogLevel && this.compareTo((LogLevel) obj) == 0;
    }

    public final int compareTo(final LogLevel o) {
        if (o == null)
            return -1;
        int result = this.key.compareTo(o.key);
        if (result == 0) {
            return this.felixCode.compareTo(o.felixCode);
        }
        return result;
    }

    @Override
    public String toString() {
        ToStringCreator tsc = new ToStringCreator(this);
        tsc.append("key", key);
        tsc.append("felixCode", felixCode);
        return tsc.toString();
    }
}
