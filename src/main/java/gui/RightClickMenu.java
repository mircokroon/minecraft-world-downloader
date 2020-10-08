package gui;

import game.data.WorldManager;

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class RightClickMenu extends JPopupMenu {
    final static String PROMPT_PAUSE = "Pause chunk saving";
    final static String PROMPT_RESUME = "Resume chunk saving";

    public RightClickMenu(CanvasHandler handler) {

        JMenuItem togglePause = new JMenuItem(PROMPT_PAUSE);

        togglePause.addActionListener(new AbstractAction("Pause chunk saving") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (WorldManager.isPaused()) {
                    System.out.println("Resuming");
                    WorldManager.resumeSaving();
                    togglePause.setText(PROMPT_PAUSE);
                } else {
                    System.out.println("Pausing");
                    WorldManager.pauseSaving();
                    togglePause.setText(PROMPT_RESUME);
                }
            }
        });
        add(togglePause);

        add(new JMenuItem(new AbstractAction("Delete all downloaded chunks") {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorldManager.deleteAllExisting();
            }
        }));


        add(new Separator());

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

        add(new JMenuItem(new AbstractAction("Save & Exit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorldManager.save();
                System.exit(0);
            }
        }));
    }
}
