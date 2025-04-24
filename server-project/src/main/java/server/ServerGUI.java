package server;

import javax.swing.*;
import java.awt.*;

/**
 * A simple JFrame that displays log messages in a scrolling text area.
 */
public class ServerGUI extends JFrame {
    private final JTextArea logArea;

    public ServerGUI() {
        super("Connect Four Server Log");
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        this.getContentPane().add(scroll, BorderLayout.CENTER);
        this.setSize(600, 400);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /** Append a line of text (from the logging handler) */
    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
