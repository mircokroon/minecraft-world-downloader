package gui;

import game.Config;
import game.data.Coordinate2D;
import game.data.Coordinate3D;
import game.data.CoordinateDim2D;
import game.data.WorldManager;
import game.data.chunk.Chunk;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The panel with the canvas we can draw to.
 */
public class CanvasHandler extends JPanel implements ActionListener {
    private final Supplier<Coordinate3D> playerPosition;
    private final Supplier<Double> playerRotation;
    private static final Image NONE = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
    private static final ChunkImage NO_IMG = new ChunkImage(NONE, true);
    private final Color BACKGROUND_COLOR = Color.decode("#292929");
    private final Color UNSAVED_COLOR = new Color(255, 0, 0, 100);
    private int renderDistance = Config.getOverviewZoomDistance();
    private boolean markUnsaved = Config.markUnsavedChunks();
    private int renderDistanceX;
    private int renderDistanceZ;
    private Bounds bounds;
    private int gridSize = 0;
    private Map<CoordinateDim2D, ChunkImage> chunkMap = new ConcurrentHashMap<>();
    private Collection<CoordinateDim2D> drawableChunks = new ConcurrentLinkedQueue<>();
    private Image chunkImage;

    private boolean hasChanged = false;

    CanvasHandler() {
        this.bounds = new Bounds();
        this.playerPosition = WorldManager.getInstance()::getPlayerPosition;
        this.playerRotation = WorldManager.getInstance()::getPlayerRotation;

        replaceChunkImage();
        computeRenderDistance();

        // timer to redraw the canvas
        new Timer(150, this).start();

        // timer to recompute bounds periodically if needed
        new Timer(2000, (e) -> computeBounds(false)).start();

        bindScroll();
    }

    public void clearChunks() {
        chunkMap.clear();
        drawableChunks.clear();

        hasChanged = true;
    }

    private void bindScroll() {
        this.addMouseWheelListener(mouseWheelEvent -> {
            this.renderDistance = this.renderDistance + (mouseWheelEvent.getWheelRotation() * 2);
            if (this.renderDistance < 2) { renderDistance = 2; }
            if (this.renderDistance > 1000) { renderDistance = 1000; }

            this.computeBounds(true);
        });
    }

    private void replaceChunkImage() {
        this.chunkImage = new BufferedImage(GuiManager.width, GuiManager.height, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Compute the render distance on both axis -- we have two to keep them separate as non-square windows will look
     * bad otherwise.
     */
    private void computeRenderDistance() {
        double ratio = (1d * GuiManager.height / GuiManager.width);

        renderDistanceX =  (int) Math.ceil(renderDistance / ratio);
        renderDistanceZ = (int) Math.ceil(renderDistance * ratio);
    }

    void setChunkExists(CoordinateDim2D coord) {
        chunkMap.put(coord, NO_IMG);

        hasChanged = true;
    }

    void markChunkSaved(CoordinateDim2D coord) {
        if (chunkMap.containsKey(coord)) {
            chunkMap.get(coord).setSaved(true);
        }

        hasChanged = true;
    }

    void setChunkLoaded(CoordinateDim2D coord, Chunk chunk) {
        Image image = chunk.getImage();

        if (image == null) {
            image = NONE;
        }

        chunkMap.put(coord, new ChunkImage(image, chunk.isSaved()));
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

        this.drawableChunks = getChunksInRange(chunkMap.keySet(),renderDistanceX * 2, renderDistanceZ * 2);
        Collection<CoordinateDim2D> inRangeChunks = getChunksInRange(drawableChunks, renderDistanceX, renderDistanceZ);

        this.bounds = getOverviewBounds(inRangeChunks);

        int gridWidth = GuiManager.width / bounds.getWidth();
        int gridHeight = GuiManager.height / bounds.getHeight();

        gridSize = Math.min(gridWidth, gridHeight);

        replaceChunkImage();
        drawChunksToImage();
    }

    private Bounds getOverviewBounds(Collection<CoordinateDim2D> coordinates) {
        Bounds bounds = new Bounds();
        for (Coordinate2D coordinate : coordinates) {
            bounds.update(coordinate);
        }
        return bounds;
    }

    /**
     * Computes the set of chunks in range, as well as building the set of all chunks we should draw (up to twice the
     * range due to pixels).
     * @return the set of chunks actually in range.
     */
    private Collection<CoordinateDim2D> getChunksInRange(Collection<CoordinateDim2D> coords, int rangeX, int rangeZ) {
        game.data.dimension.Dimension dimension = Config.getDimension();
        if (playerPosition.get() == null) {
            drawableChunks = chunkMap.keySet();
            return drawableChunks;
        }
        Coordinate2D player = playerPosition.get().globalToChunk();

        return coords.parallelStream()
            .filter(coordinateDim2D -> coordinateDim2D.getDimension().equals(dimension))
            .filter(coordinate2D -> coordinate2D.isInRange(player, rangeX, rangeZ))
            .collect(Collectors.toSet());
    }


    /**
     * Called when the canvas needs to be re-painted. Will draw the player position and all of the chunks.
     */
    @Override
    protected synchronized void paintComponent(Graphics g) {
        g.clearRect(0, 0, GuiManager.width, GuiManager.height);
        if (chunkImage != null) {
            g.drawImage(chunkImage, 0, 0, GuiManager.width, GuiManager.height,
                    (im, infoflags, x, y, width, height) -> false);
        }

        Coordinate3D playerPos = playerPosition.get();

        double playerX = ((playerPos.getX() / 16.0 - bounds.getMinX()) * gridSize);
        double playerZ = ((playerPos.getZ() / 16.0 - bounds.getMinZ()) * gridSize);

        // direction pointer
        double yaw = Math.toRadians(this.playerRotation.get() + 45);
        double pointerX = (playerX + (3)*Math.cos(yaw) - (3)*Math.sin(yaw));
        double pointerZ = (playerZ + (3)*Math.sin(yaw) + (3)*Math.cos(yaw));

        g.setColor(Color.BLACK);
        g.fillOval((int) playerX - 6, (int) playerZ - 6, 12, 12);
        g.fillOval((int) pointerX - 4, (int) pointerZ - 4, 8, 8);

        g.setColor(Color.WHITE);
        g.fillOval((int) playerX - 4, (int) playerZ - 4, 8, 8);
        g.fillOval((int) pointerX - 2, (int) pointerZ - 2, 4, 4);

        // indicator circle
        g.setColor(Color.RED);
        g.drawOval((int) playerX - 16, (int) playerZ - 16, 32, 32);
    }

    private void drawChunksToImage() {
        Graphics g = chunkImage.getGraphics();
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, GuiManager.width, GuiManager.height);
        for (Coordinate2D pos : drawableChunks) {
            drawChunk(g, pos);
        }
    }

    private void drawChunk(Graphics g, Coordinate2D pos, Bounds bounds, int gridSize) {
        ChunkImage chunkImage = chunkMap.get(pos);

        int drawX = (pos.getX() - bounds.getMinX()) * gridSize;
        int drawY = (pos.getZ() - bounds.getMinZ()) * gridSize;
        if (chunkImage.getImage() == NONE) {
            g.setColor(Color.WHITE);
            g.drawRect(drawX, drawY, gridSize, gridSize);
        } else {
            g.drawImage(
                    chunkImage.getImage(), drawX, drawY, gridSize, gridSize,
                    (im, infoFlags, x, y, width, height) -> false
            );

            // if the chunk wasn't saved yet, mark it as such
            if (markUnsaved && !chunkImage.isSaved) {
                g.setColor(UNSAVED_COLOR);
                g.fillRect(drawX, drawY, gridSize, gridSize);
            }
        }
    }

    private void drawChunk(Graphics g, Coordinate2D pos) {
        drawChunk(g, pos, this.bounds, this.gridSize);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(GuiManager.width, GuiManager.height);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    public void export() {
        int MAX_RADIUS = 300;

        Bounds b = getOverviewBounds(getChunksInRange(chunkMap.keySet(), MAX_RADIUS, MAX_RADIUS));

        int imgWidth = b.getWidth() * Chunk.SECTION_WIDTH;
        int imgHeight = b.getHeight() * Chunk.SECTION_WIDTH;

        int gridSize = Chunk.SECTION_WIDTH;

        Image img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_3BYTE_BGR);
        chunkMap.keySet().forEach(coord -> drawChunk(img.getGraphics(), coord, b, gridSize));


        try {
            ImageIO.write((RenderedImage) img, "png", new File(Config.getExportDirectory() + "/rendered.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ChunkImage {
    Image image;
    boolean isSaved;

    public ChunkImage(Image image, boolean isSaved) {
        this.image = image;
        this.isSaved = isSaved;
    }

    public Image getImage() {
        return image;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
    }
}
