package org.quangdao;

import org.quangdao.ui.ConfigWindow;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {
    public static void main(String[] args) throws Exception {
        ConfigWindow configWindow = new ConfigWindow();
        JFrame jFrame = new JFrame();
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.add(configWindow.getRoot());
        jFrame.pack();
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                configWindow.saveToFile();
            }
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        jFrame.setVisible(true);
    }
}
