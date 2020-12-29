package acat.services.logger;

import java.util.Date;

public class LogRecord {

    private Date timestamp;
    private Level level;
    private String message;

    public LogRecord( Level level, String message ) {
        this.timestamp = new Date();
        this.level     = level;
        this.message   = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

}