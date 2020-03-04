package gui;

import game.data.WorldManager;

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class RightClickMenu extends JPopupMenu {
    public RightClickMenu(CanvasHandler handler) {

        add(new JMenuItem(new AbstractAction("Save overview to file") {
            @Override
            public void actionPerformed(ActionEvent e) {
                handler.export();
            }
        }));


        add(new JMenuItem(new AbstractAction("Draw all existing chunks") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    WorldManager.drawExistingChunks();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }));

        add(new JMenuItem(new AbstractAction("Exit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        }));
    }
}
