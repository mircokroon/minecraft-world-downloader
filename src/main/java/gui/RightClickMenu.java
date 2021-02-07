package gui;

import game.Config;
import game.data.CoordinateDim2D;
import game.data.WorldManager;
import game.data.chunk.ChunkBinary;
import game.data.dimension.Dimension;
import game.data.region.McaFile;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                if (WorldManager.getInstance().isPaused()) {
                    System.out.println("Resuming");
                    WorldManager.getInstance().resumeSaving();
                    togglePause.setText(PROMPT_PAUSE);
                } else {
                    System.out.println("Pausing");
                    WorldManager.getInstance().pauseSaving();
                    togglePause.setText(PROMPT_RESUME);
                }
            }
        });
        add(togglePause);

        add(new JMenuItem(new AbstractAction("Delete all downloaded chunks") {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorldManager.getInstance().deleteAllExisting();
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
                    WorldManager.getInstance().drawExistingChunks();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }));

        add(new JMenuItem(new AbstractAction("Save & Exit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorldManager.getInstance().save();
                System.exit(0);
            }
        }));

        if (Config.isInDevMode()) {
            addDevOptions();
        }
    }

    private void addDevOptions() {
        add(new Separator());

        // write chunk 0 0 to a file so that it can be used to run tests.
        add(new JMenuItem(new AbstractAction("Write chunk 0 0") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Path p = Paths.get(Config.getExportDirectory(), "", "region", "r.0.0.mca");
                    ChunkBinary cb = new McaFile(p.toFile()).getChunkBinary(new CoordinateDim2D(0, 0, Dimension.OVERWORLD));

                    String filename = "chunkdata.bin";
                    FileOutputStream f = new FileOutputStream(filename);
                    ObjectOutputStream o = new ObjectOutputStream(f);
                    o.writeObject(cb);

                    System.out.println("Write chunk (0, 0) to " + filename);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }));
    }
}
