package gui;


import game.data.Coordinate2D;
import game.data.CoordinateDim2D;
import game.data.WorldManager;
import game.data.chunk.Chunk;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Class to the handle the GUI.
 */
public class GuiManager {
    public static int width = 400;
    public static int height = 400;

    private static CanvasHandler chunkGraphicsHandler;

    public static void showGui() {
        SwingUtilities.invokeLater(GuiManager::createAndShowGUI);
    }

    /**
     * Initialised the GUI. Asks the world manager to provide provide the list of chunks that already exist so that we
     * can draw those to the UI.
     */
    private static void createAndShowGUI() {
        JFrame f = new JFrame("World Downloader");
        f.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        f.setSize(width, height);
        f.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent evt) {
                Component c = (Component) evt.getSource();
                height = c.getHeight();
                width = c.getWidth();
                chunkGraphicsHandler.computeBounds(true);
            }
        });

        chunkGraphicsHandler = new CanvasHandler();
        f.add(chunkGraphicsHandler);

        f.pack();
        f.setVisible(true);

        chunkGraphicsHandler.setComponentPopupMenu(new RightClickMenu(chunkGraphicsHandler));

        try {
            WorldManager.outlineExistingChunks();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set a chunk to being loaded.
     * @param coord the chunk coordinates
     * @param chunk the chunk object
     */
    public static void setChunkLoaded(CoordinateDim2D coord, Chunk chunk) {
        if (chunkGraphicsHandler != null) {
            chunkGraphicsHandler.setChunkLoaded(coord, chunk);
        }
    }

    public static void drawExistingChunks(List<CoordinateDim2D> existing) {
        if (chunkGraphicsHandler != null) {
            existing.forEach(chunkGraphicsHandler::setChunkExists);
        }
    }

    public static void clearChunks() {
        if (chunkGraphicsHandler != null) {
            chunkGraphicsHandler.clearChunks();
        }
    }
}

