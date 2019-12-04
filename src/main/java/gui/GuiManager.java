package gui;


import game.Game;
import game.data.Coordinate2D;
import game.data.Coordinate3D;
import game.data.WorldManager;
import game.data.chunk.Chunk;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
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
     * @param chunk the chunk object
     */
    public static void setChunkLoaded(Coordinate2D coord, Chunk chunk) {
        if (graphicsHandler != null) {
            graphicsHandler.setChunkLoaded(coord, chunk);
        }
    }
}

/**
 * The panel with the canvas we can draw to.
 */
class GraphicsHandler extends JPanel implements ActionListener {
    private final int render_distance;
    private Timer timer;
    private int minX;
    private int minZ;
    private int gridSize = 0;
    private HashMap<Coordinate2D, Image> chunkMap = new HashMap<>();

    GraphicsHandler() {
        this.render_distance = Game.getRenderDistance();
        timer = new Timer(1000, this);
        timer.start();
    }

    synchronized void setChunkLoaded(Coordinate2D coord, Chunk chunk) {
        Image image = chunk.getImage();
        if (image == null) { new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY); }

        chunkMap.put(coord, image);
        computeBounds();
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
                .filter(el -> playerChunk.isInRange(el, render_distance))
                .collect(Collectors.toSet());
        }

        int[] xCoords = inRangeChunks.stream().mapToInt(Coordinate2D::getX).toArray();
        int maxX = Arrays.stream(xCoords).max().orElse(0) + 1;
        minX = Arrays.stream(xCoords).min().orElse(0) - 1;

        int[] zCoords = inRangeChunks.stream().mapToInt(Coordinate2D::getZ).toArray();
        int maxZ = Arrays.stream(zCoords).max().orElse(0) + 1;
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
        g.clearRect(0, 0, GuiManager.WIDTH, GuiManager.HEIGHT);
        for (Map.Entry<Coordinate2D, Image> e : chunkMap.entrySet()) {

            g.drawImage(
                e.getValue(),
                (e.getKey().getX() - minX) * gridSize,
                (e.getKey().getZ() - minZ) * gridSize,
                gridSize,
                gridSize,
                (img, infoflags, x, y, width, height) -> false
            );

        }

        if (Game.getPlayerPosition() != null) {
            g.setColor(Color.RED);
            Coordinate3D playerPosition = Game.getPlayerPosition();

            double playerX = ((playerPosition.getX() / 16.0 - minX) * gridSize);
            double playerZ = ((playerPosition.getZ() / 16.0 - minZ) * gridSize);

            g.fillOval((int) playerX, (int) playerZ, 8, 8);
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
}