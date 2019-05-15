package gui;


import game.Game;
import game.data.Coordinate2D;
import game.data.Coordinate3D;
import game.data.WorldManager;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Class to the handle the GUI.
 */
public class GuiManager {
    public static int WIDTH = 400;
    public static int HEIGHT = 400;

    private static GraphicsHandler graphicsHandler;

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
        f.setSize(WIDTH, HEIGHT);
        f.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent evt) {
                Component c = (Component) evt.getSource();
                HEIGHT = c.getHeight();
                WIDTH = c.getWidth();
                graphicsHandler.computeBounds();
            }
        });

        graphicsHandler = new GraphicsHandler();
        f.add(graphicsHandler);
        f.pack();
        f.setVisible(true);

        try {
            WorldManager.loadExistingChunks();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set a chunk to being loaded.
     * @param coord the chunk coordinates
     */
    public static void setChunkLoaded(Coordinate2D coord) {
        if (graphicsHandler != null) {
            graphicsHandler.setChunkLoaded(coord);
        }
    }

    /**
     * Set a list of chunks that have been saved.
     * @param saved the list of saved chunk coordinates
     */
    public static void setChunksSaved(List<Coordinate2D> saved) {
        if (graphicsHandler != null) {
            graphicsHandler.setChunksSaved(saved);
        }
    }

    /**
     * Indicate if the world is currently being saved.
     * @param b true if the world is being saved
     */
    public static void setSaving(boolean b) {
        graphicsHandler.setSaving(b);
    }
}

/**
 * The panel with the canvas we can draw to.
 */
class GraphicsHandler extends JPanel implements ActionListener {
    private final int RENDER_RANGE = 200;
    Graphics g;
    Timer timer;
    private int minX, maxX, minZ, maxZ, gridSize = 0;
    private HashMap<Coordinate2D, Boolean> chunkMap = new HashMap<>();
    private boolean isSaving = false;

    GraphicsHandler() {
        timer = new Timer(1000, this);
        timer.start();
    }

    void setSaving(boolean b) {
        isSaving = b;
    }

    synchronized void setChunkLoaded(Coordinate2D coord) {
        if (!chunkMap.containsKey(coord)) {
            chunkMap.put(coord, false);
            computeBounds();
        }
    }

    /**
     * Compute the bounds of the canvas based on the existing chunk data. Will also delete chunks that are out of the
     * set render distance. The computed bounds will be used to determine the scale and positions to draw the chunks to.
     */
    void computeBounds() {
        Set<Coordinate2D> inRangeChunks = chunkMap.keySet();
        if (Game.getPlayerPosition() != null) {
            Coordinate2D playerChunk = Game.getPlayerPosition().chunkPos();
            inRangeChunks = chunkMap.keySet().stream()
                .filter(el -> playerChunk.isInRange(el, RENDER_RANGE))
                .collect(Collectors.toSet());
        }

        int[] xCoords = inRangeChunks.stream().mapToInt(Coordinate2D::getX).toArray();
        maxX = Arrays.stream(xCoords).max().orElse(0) + 1;
        minX = Arrays.stream(xCoords).min().orElse(0) - 1;

        int[] zCoords = inRangeChunks.stream().mapToInt(Coordinate2D::getZ).toArray();
        maxZ = Arrays.stream(zCoords).max().orElse(0) + 1;
        minZ = Arrays.stream(zCoords).min().orElse(0) - 1;

        int gridWidth = GuiManager.WIDTH / (Math.abs(maxX - minX) + 1);
        int gridHeight = GuiManager.HEIGHT / (Math.abs(maxZ - minZ) + 1);
        gridSize = Math.min(gridWidth, gridHeight);
    }

    /**
     * Called when the canvas needs to be re-painted. Will draw the player position, all of the chunks and potentially
     * the text 'saving' if the world is being saved.
     */
    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (this.g == null) {
            this.g = g;
            g.setFont(g.getFont().deriveFont(16f));
        }

        g.clearRect(0, 0, GuiManager.WIDTH, GuiManager.HEIGHT);
        for (Map.Entry<Coordinate2D, Boolean> e : chunkMap.entrySet()) {
            g.setColor(e.getValue() ? Color.green : Color.BLUE);
            g.fillRect(
                (e.getKey().getX() - minX) * gridSize,
                (e.getKey().getZ() - minZ) * gridSize,
                gridSize,
                gridSize
            );
        }

        if (Game.getPlayerPosition() != null) {
            g.setColor(Color.RED);
            Coordinate3D playerPosition = Game.getPlayerPosition();

            double playerX = ((playerPosition.getX() / 16.0 - minX) * gridSize);
            double playerZ = ((playerPosition.getZ() / 16.0 - minZ) * gridSize);

            g.fillOval((int) playerX, (int) playerZ, 8, 8);
        }

        if (isSaving) {
            g.setColor(Color.black);
            g.drawString("Saving...", 10, 10);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(GuiManager.WIDTH, GuiManager.HEIGHT);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    synchronized void setChunksSaved(List<Coordinate2D> saved) {
        for (Coordinate2D coordinate : saved) {
            chunkMap.put(coordinate, true);
        }
        computeBounds();
    }
}