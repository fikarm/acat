package acat.services.logger;

public class Logger {

    private static final Logger logger = new Logger( ) ;
    private static final Log log = new Log();

    private Logger() {
    }

    public static Logger getInstance() {
        return logger;
    }

    public static void log( LogRecord record ) {
        log.offer( record );
    }

    public static void debug( String msg ) {
        log( new LogRecord( Level.DEBUG, msg ) );
    }

    public static void info( String msg ) {
        log( new LogRecord( Level.INFO, msg ) );
    }

    public static void warn( String msg ) {
        log( new LogRecord( Level.WARN, msg ) );
    }

    public static void error( String msg ) {
        log( new LogRecord( Level.ERROR, msg ) );
    }

    public static Log getLog() {
        return log;
    }

}