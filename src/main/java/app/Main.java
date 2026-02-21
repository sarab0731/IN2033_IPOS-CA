package app;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("IPOS-CA");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);

            JLabel label = new JLabel("IPOS-CA starting...", SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.BOLD, 24));
            frame.add(label);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
