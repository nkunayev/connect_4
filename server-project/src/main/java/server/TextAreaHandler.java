package server;

import java.util.logging.*;

/**
 * A java.util.logging.Handler that writes formatted log records into a ServerGUI.
 */
public class TextAreaHandler extends Handler {
    private final ServerGUI gui;
    private final Formatter formatter = new SimpleFormatter();

    public TextAreaHandler(ServerGUI gui) {
        this.gui = gui;
        setLevel(Level.ALL);
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) return;
        String msg = formatter.format(record);
        gui.appendLog(msg);
    }

    @Override
    public void flush() { /* no buffering */ }

    @Override
    public void close() throws SecurityException { /* nothing to close */ }
}
