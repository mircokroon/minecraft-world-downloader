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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Class to the handle the GUI.
 */
public class GuiManager {
    public static int width = 400;
    public static int height = 400;

    private static GraphicsHandler chunkGraphicsHandler;

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

        chunkGraphicsHandler = new GraphicsHandler();
        f.add(chunkGraphicsHandler);

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
        if (chunkGraphicsHandler != null) {
            chunkGraphicsHandler.setChunkLoaded(coord, chunk);
        }
    }

    public static void drawExistingChunks(List<Coordinate2D> existing) {
        if (chunkGraphicsHandler != null) {
            existing.forEach(chunkGraphicsHandler::setChunkExists);
        }
    }
}

/**
 * The panel with the canvas we can draw to.
 */
class GraphicsHandler extends JPanel implements ActionListener {
    private static final Image NONE = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
    private final Color BACKGROUND_COLOR = Color.decode("#292929");
    private int renderDistanceX;
    private int renderDistanceZ;
    private int minX;
    private int minZ;
    private int gridSize = 0;
    private Map<Coordinate2D, Image> chunkMap = new ConcurrentHashMap<>();
    private Collection<Coordinate2D> drawableChunks = new ConcurrentLinkedQueue<>();
    private Image chunkImage;

    private boolean hasChanged = false;

    GraphicsHandler() {
        replaceChunkImage();
        computeRenderDistance();

        // timer to redraw the canvas
        new Timer(150, this).start();

        // timer to recompute bounds periodically if needed
        new Timer(2000, (e) -> computeBounds(false)).start();
    }

    private void replaceChunkImage() {
        this.chunkImage = new BufferedImage(GuiManager.width, GuiManager.height, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Compute the render distance on both axis -- we have two to keep them separate as non-square windows will look
     * bad otherwise.
     */
    private void computeRenderDistance() {
        int avgDistance = Game.getRenderDistance();
        double ratio = (1d * GuiManager.height / GuiManager.width);

        renderDistanceX =  (int) Math.ceil(avgDistance / ratio);
        renderDistanceZ = (int) Math.ceil(avgDistance * ratio);
    }

    void setChunkExists(Coordinate2D coord) {
        chunkMap.put(coord, NONE);

        hasChanged = true;
    }

    void setChunkLoaded(Coordinate2D coord, Chunk chunk) {
        Image image = chunk.getImage();

        if (image == null) {
            image = NONE;
        }

        chunkMap.put(coord, image);
        drawChunk(chunkImage.getGraphics(), coord);

        hasChanged = true;
    }

    /**
     * Compute the bounds of the canvas based on the existing chunk data. Will also delete chunks that are out of the
     * set render distance. The computed bounds will be used to determine the scale and positions to draw the chunks to.
     */
    void computeBounds(boolean dimensionsChanged) {
        if (dimensionsChanged) {
            computeRenderDistance();
        } else if (!hasChanged) {
            return;
        }

        hasChanged = false;

        Collection<Coordinate2D> inRangeChunks = getChunksInRange();

        int[] xCoords = inRangeChunks.stream().mapToInt(Coordinate2D::getX).toArray();
        int maxX = Arrays.stream(xCoords).max().orElse(0) + 1;
        minX = Arrays.stream(xCoords).min().orElse(0) - 1;

        int[] zCoords = inRangeChunks.stream().mapToInt(Coordinate2D::getZ).toArray();
        int maxZ = Arrays.stream(zCoords).max().orElse(0) + 1;
        minZ = Arrays.stream(zCoords).min().orElse(0) - 1;

        int gridWidth = GuiManager.width / (Math.abs(maxX - minX) + 1);
        int gridHeight = GuiManager.height / (Math.abs(maxZ - minZ) + 1);
        gridSize = Math.min(gridWidth, gridHeight);

        replaceChunkImage();
        drawChunksToImage();
    }

    /**
     * Computes the set of chunks in range, as well as building the set of all chunks we should draw (up to twice the
     * range due to pixels).
     * @return the set of chunks actually in range.
     */
    private Collection<Coordinate2D> getChunksInRange() {
        if (Game.getPlayerPosition() == null) {
            drawableChunks = chunkMap.keySet();
            return drawableChunks;
        }

        Collection<Coordinate2D> inRangeChunks = new ConcurrentLinkedQueue<>();
        drawableChunks = new ConcurrentLinkedQueue<>();
        Coordinate2D playerChunk = Game.getPlayerPosition().chunkPos();

        for (Coordinate2D coord : chunkMap.keySet()) {
            if (playerChunk.isInRange(coord, renderDistanceX * 2, renderDistanceZ * 2)) {
                drawableChunks.add(coord);

                if (playerChunk.isInRange(coord, renderDistanceX, renderDistanceZ)) {
                    inRangeChunks.add(coord);
                }
            }
        }
        return inRangeChunks;
    }

    /**
     * Called when the canvas needs to be re-painted. Will draw the player position, all of the chunks and potentially
     * the text 'saving' if the world is being saved.
     */
    @Override
    protected synchronized void paintComponent(Graphics g) {
        g.clearRect(0, 0, GuiManager.width, GuiManager.height);
        if (chunkImage != null) {
            g.drawImage(chunkImage, 0, 0, GuiManager.width, GuiManager.height,
                        (im, infoflags, x, y, width, height) -> false);
        }

        if (Game.getPlayerPosition() != null) {
            Coordinate3D playerPosition = Game.getPlayerPosition();

            double playerX = ((playerPosition.getX() / 16.0 - minX) * gridSize);
            double playerZ = ((playerPosition.getZ() / 16.0 - minZ) * gridSize);

            g.setColor(Color.BLACK);
            g.fillOval((int) playerX - 6, (int) playerZ - 6, 12, 12);
            g.setColor(Color.WHITE);
            g.fillOval((int) playerX - 4, (int) playerZ - 4, 8, 8);

            g.setColor(Color.RED);
            g.drawOval((int) playerX - 16, (int) playerZ - 16, 32, 32);
        }
    }

    private void drawChunksToImage() {
        Graphics g = chunkImage.getGraphics();
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, GuiManager.width, GuiManager.height);
        g.setColor(Color.WHITE);
        for (Coordinate2D pos : drawableChunks) {
            drawChunk(g, pos);
        }
    }

    private void drawChunk(Graphics g, Coordinate2D pos) {
        Image img =  chunkMap.get(pos);

        int drawX = (pos.getX() - minX) * gridSize;
        int drawY = (pos.getZ() - minZ) * gridSize;
        if (img == NONE) {
            g.drawRect(drawX, drawY, gridSize, gridSize);
        } else {
            g.drawImage(
                img, drawX, drawY, gridSize, gridSize,
                (im, infoflags, x, y, width, height) -> false
            );
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(GuiManager.width, GuiManager.height);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }
}