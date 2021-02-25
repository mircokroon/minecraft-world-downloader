package gui;

import config.Config;
import game.data.chunk.ChunkImageFactory;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.CoordinateDim2D;
import game.data.coordinates.CoordinateDouble3D;
import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.dimension.Dimension;
import game.data.entity.PlayerEntity;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import util.PathUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Controller for the map scene. Contains a canvas for chunks which is redrawn only when required, and one for entities
 * which can be redrawn any moment.
 */
public class GuiMap {
    private static final Image NONE = new WritableImage(1, 1);
    private static final ChunkImage NO_IMG = new ChunkImage(NONE, true);
    private static final Color BACKGROUND_COLOR = new Color(.2, .2, .2, 1);
    private static final Color EXISTING_COLOR = new Color(.8, .8, .8, .2);
    private static final Color UNSAVED_COLOR = new Color(1, 0, 0, .3);
    private static final Color PLAYER_COLOR = new Color(.6, .95, 1, .7);
    private static final WritableImage BLACK = new WritableImage(1, 1);
    static {
        BLACK.getPixelWriter().setColor(0, 0, Color.BLACK);
    }

    public Canvas chunkCanvas;
    public Canvas entityCanvas;
    public Label helpLabel;
    public Label coordsLabel;
    public Button playerLockButton;

    private CoordinateDouble3D playerPos;
    private double playerRotation;

    private Bounds bounds;
    private int renderDistanceX;
    private int renderDistanceZ;
    private int gridSize = 0;
    private final Map<Dimension, Map<Coordinate2D, ChunkImage>> chunkDimensions = new ConcurrentHashMap<>();
    private Map<Coordinate2D, ChunkImage> chunkMap;
    private Collection<Coordinate2D> drawableChunks = new ConcurrentLinkedQueue<>();
    private Collection<PlayerEntity> otherPlayers;

    ReadOnlyDoubleProperty width;
    ReadOnlyDoubleProperty height;

    private boolean hasChanged = false;
    private boolean mouseOver = false;
    private boolean enableModernImageHandling = true;
    private boolean playerHasConnected = false;
    private boolean showErrorPrompt = false;
    private boolean isDragging = false;
    private boolean draggingHasMoved = false;
    private boolean lockedToPlayer = true;

    private double mouseX = -1;
    private double mouseY = -1;

    private Coordinate2D center = new Coordinate2D(0, 0);
    WritableImage chunkCanvasCopy;

    @FXML
    void initialize() {
        WorldManager manager = WorldManager.getInstance();
        this.otherPlayers = manager.getEntityRegistry().getPlayerSet();

        Platform.runLater(manager::outlineExistingChunks);

        setDimension(manager.getDimension());
        this.playerPos = manager.getPlayerPosition().toDouble();
        playerLockButton.setVisible(false);

        setupCanvasProperties();

        GuiManager.setGraphicsHandler(this);
        manager.setPlayerPosListener(this::updatePlayerPos);

        AnimationTimer animationTimer = new AnimationTimer() {
            @Override
            public void handle(long time) {
                redrawEntities();
            }
        };
        animationTimer.start();

        setupContextMenu();
        bindScroll();

        setupHelpLabel();
    }

    private void setupHelpLabel() {
        if (!playerHasConnected) {
            helpLabel.setText(Config.getConnectionDetails().getConnectionHint());
        }
        entityCanvas.setOnMouseEntered(e -> {
            mouseOver = true;
            if (playerHasConnected && !showErrorPrompt) {
                helpLabel.setText("Right-click to open context menu. Scroll to zoom.");
            }
        });
        entityCanvas.setOnMouseMoved(e -> {
            mouseX = e.getSceneX();
            mouseY = e.getSceneY();

            int worldX = (int) Math.round((bounds.getMinX() + (mouseX / gridSize)) * 16);
            int worldZ = (int) Math.round((bounds.getMinZ() + (mouseY / gridSize)) * 16);

            coordsLabel.setText("(" + worldX + ", " + worldZ + ")");
        });
        entityCanvas.setOnMouseExited(e -> {
            mouseOver = false;
            coordsLabel.setText("");
            if (playerHasConnected && !showErrorPrompt) {
                helpLabel.setText("");
            }
        });

        handleDrag();
    }

    /**
     * Handle dragging on the canvas. When dragging, a copy of the current canvas is made to provide a lightweight
     * visualisation of the dragging state. When dragging ends, the chunks are fully re-drawn.
     */
    private void handleDrag() {
        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);

        entityCanvas.setOnMousePressed((e) -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }

            // hide entity canvas while dragging
            entityCanvas.setOpacity(0);
            isDragging = true;
            draggingHasMoved = false;
            chunkCanvasCopy = chunkCanvas.snapshot(snapshotParameters, null);
        });

        entityCanvas.setOnMouseReleased((e) -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            isDragging = false;
            entityCanvas.setOpacity(1);
            if (!draggingHasMoved) {
                return;
            }

            lockedToPlayer = false;
            playerLockButton.setVisible(true);

            double diffX = mouseX - e.getX();
            double diffY = mouseY - e.getY();

            Coordinate2D difference = new Coordinate2D(Math.round(diffX / gridSize), Math.round(diffY / gridSize));
            this.center = this.bounds.center().add(difference);
            this.redrawAll(true);

            if (difference.getX() == 0 && difference.getZ() == 0) {
                followPlayer();
                chunkCanvas.getGraphicsContext2D().drawImage(chunkCanvasCopy, 0, 0);
            }
        });

        entityCanvas.setOnMouseDragged((e) -> {
            if (!isDragging) { return; }
            draggingHasMoved = true;

            double diffX = mouseX - e.getX();
            double diffY = mouseY - e.getY();

            chunkCanvas.getGraphicsContext2D().setFill(BACKGROUND_COLOR);
            chunkCanvas.getGraphicsContext2D().fillRect(0, 0, width.get(), height.get());
            chunkCanvas.getGraphicsContext2D().drawImage(chunkCanvasCopy, -diffX, -diffY);
        });

        // button to reset the center back to the player
        playerLockButton.setOnMouseClicked(e -> followPlayer());
    }

    private void followPlayer() {
        lockedToPlayer = true;
        playerLockButton.setVisible(false);
        redrawAll(true);
    }

    private void setupCanvasProperties() {
        setSmoothingState(chunkCanvas.getGraphicsContext2D(), false);
        setSmoothingState(entityCanvas.getGraphicsContext2D(), true);

        entityCanvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
        entityCanvas.getGraphicsContext2D().setFont(Font.font(null, FontWeight.BOLD, 14));

        Pane p = (Pane) chunkCanvas.getParent();
        width = p.widthProperty();
        height = p.heightProperty();

        chunkCanvas.widthProperty().bind(width);
        chunkCanvas.heightProperty().bind(height);

        entityCanvas.widthProperty().bind(width);
        entityCanvas.heightProperty().bind(height);

        // periodically recompute the canvas bounds
        Timeline reload = new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            computeBounds(false);
        }));

        Timeline redraw = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            this.bounds = null;
            redrawAll(true);
        }));

        height.addListener((ChangeListener<? super Number>) (a, b, c) -> redraw.play());
        height.addListener((ChangeListener<? super Number>) (a, b, c) -> redraw.play());

        // periodically clean up old images
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor((r) -> new Thread(r, "Chunk Image Cleaner"));
        executor.scheduleWithFixedDelay(this::cleanFarImages, 120, 60, TimeUnit.SECONDS);

        reload.setCycleCount(Animation.INDEFINITE);
        reload.play();

        redrawAll(true);
    }

    /**
     * Periodically we will try to remove older images so we don't use needless memory to store them all. It only
     * removes them if they are over 8000 blocks away so it's fairly conservative.
     */
    private void cleanFarImages() {
        Coordinate2D playerRegion = playerPos.discretize().globalToChunk().chunkToRegion();
        for (Coordinate2D c : chunkMap.keySet()) {
            Coordinate2D chunkRegion = c.chunkToRegion();

            if (playerRegion.blockDistance(chunkRegion) > 16) {
                chunkMap.remove(c);
            }
        }
    }

    /**
     * Draw an image to the given canvas. In Java 9+, this just calls drawImage. In Java 8 drawImage causes super
     * ugly artifacts due to forced interpolation, so to avoid this we manually draw the image and do nearest neighbour
     * interpolation.
     */
    private void drawImage(GraphicsContext ctx, int drawX, int drawY, int gridSize, Image img, boolean drawBlack) {
        if (enableModernImageHandling) {
            if (drawBlack) {
                ctx.drawImage(BLACK, drawX, drawY, gridSize, gridSize);
            }
            ctx.drawImage(img, drawX, drawY, gridSize, gridSize);
            return;
        }

        // since this drawing method does not support out of bounds drawing, check for bounds first
        if (drawX < 0 || drawY < 0 || gridSize < 1) {
            return;
        }
        if (drawX + gridSize > ctx.getCanvas().getWidth() || drawY + gridSize > ctx.getCanvas().getHeight()) {
            return;
        }

        // if drawBlack is enabled, we remove transparency by doing a bitwise or with this mask.
        int colMask = 0;
        if (drawBlack) {
            colMask = 0xFF000000;
        }

        double imgSize = img.getWidth();

        // for performance reasons, we read all pixels and write pixels through arrays. We only touch the pixel
        // reader/writer at the start and end.
        int imgWidth = (int) imgSize;
        int[] input = new int[imgWidth * imgWidth];
        int[] output = new int[gridSize * gridSize];

        WritablePixelFormat<IntBuffer> format = WritablePixelFormat.getIntArgbInstance();
        img.getPixelReader().getPixels(0, 0, imgWidth, imgWidth, format, input, 0, imgWidth);


        // in the loop we use the ratio to calculate where a pixel fom the input image ends up in the output
        double ratio = imgSize / gridSize;
        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                int imgX = (int) (x * ratio);
                int imgY = (int) (y * ratio);

                output[x + y * gridSize] = input[imgX + imgY * imgWidth] | colMask;
            }
        }

        ctx.getPixelWriter().setPixels(drawX, drawY, gridSize, gridSize, format, output, 0, gridSize);
    }

    private void setSmoothingState(GraphicsContext ctx, boolean value) {
        try {
            Method m = ctx.getClass().getMethod("setImageSmoothing", boolean.class);
            m.invoke(ctx, value);
        } catch (NoSuchMethodError | NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            enableModernImageHandling = false;
            // if we can't set the image smoothing, we're likely on an older Java version. We will draw it manually
            // so that we can use Nearest Neighbour interpolation.
        }
    }

    private void setupContextMenu() {
        ContextMenu menu = new RightClickMenu(this);
        entityCanvas.setOnContextMenuRequested(e -> menu.show(entityCanvas, e.getScreenX(), e.getScreenY()));

        entityCanvas.setOnMouseClicked(e -> menu.hide());
    }

    public void clearChunks() {
        chunkMap.clear();
        drawableChunks.clear();

        hasChanged = true;
    }

    private void bindScroll() {
        entityCanvas.setOnScroll(scrollEvent -> {
            int zoom = Config.getZoomLevel();
            if (scrollEvent.getDeltaY() > 0) {
                zoom /= 2;
            } else {
                zoom *= 2;
            }
            if (zoom < 1) { zoom = 1; }
            if (zoom > 1000) { zoom = 1000; }

            if (Config.getZoomLevel() != zoom) {
                Config.setZoomLevel(zoom);
                redrawAll(true);
            }
        });
    }

    /**
     * Compute the render distance on both axis -- we have two to keep them separate as non-square windows will look
     * bad otherwise.
     */
    private void computeRenderDistance() {
        double ratio = (height.get() / width.get());

        int zoom = Config.getZoomLevel();

        renderDistanceZ = zoom;
        renderDistanceX = zoom;

        // height is bigger - so reduce width
        if (ratio > 1) {
            renderDistanceZ = (int) Math.ceil(zoom / ratio);
            // width is bigger - reduce height
        } else {
            renderDistanceX = (int) Math.ceil(zoom / ratio);
        }
    }

    void setChunkExists(CoordinateDim2D coord) {
        chunkMap.put(coord.stripDimension(), NO_IMG);

        hasChanged = true;
    }

    void markChunkSaved(CoordinateDim2D coord) {
        if (chunkMap.containsKey(coord.stripDimension())) {
            chunkMap.get(coord.stripDimension()).setSaved(true);
        }

        hasChanged = true;
    }

    void setChunkLoaded(CoordinateDim2D coord, Chunk chunk) {
        if (!playerHasConnected) {
            playerHasConnected = true;

            if (!showErrorPrompt) {
                Platform.runLater(() -> helpLabel.setText(""));
            }
        }

        ChunkImageFactory imageFactory = chunk.getChunkImageFactory();
        imageFactory.onComplete(image -> {
            chunkMap.put(coord.stripDimension(), new ChunkImage(image, chunk.isSaved()));
            drawChunkAsync(coord.stripDimension());

            hasChanged = true;
        });
        imageFactory.createImage();
    }

    void redrawAll(boolean force) {
        computeRenderDistance();
        this.computeBounds(force);
        hasChanged = false;
    }

    /**
     * Compute the bounds of the canvas based on the existing chunk data. Will also delete chunks that are out of the
     * set render distance. The computed bounds will be used to determine the scale and positions to draw the chunks to.
     */
    void computeBounds(boolean force) {
        if (!force && !hasChanged) {
            return;
        }

        Coordinate2D center;
        if (lockedToPlayer && this.playerPos != null) {
            center = this.playerPos.discretize().globalToChunk();
        } else {
            // if no player position is known, calculate the average coordinate
            center = this.center;
        }

        //this.drawableChunks = getChunksInRange(chunkMap.keySet(),renderDistanceX * 2, renderDistanceZ * 2);
        this.drawableChunks = getChunksInRange(center, chunkMap.keySet(), renderDistanceX, renderDistanceZ);

        Bounds newBounds;
        if (lockedToPlayer) {
            newBounds = getOverviewBounds(drawableChunks, this.playerPos.discretize().globalToChunk());
        } else {
            newBounds = new Bounds(center, renderDistanceX, renderDistanceZ);
        }

        if (!newBounds.equals(bounds)) {
            bounds = newBounds;

            double gridWidth = width.get() / bounds.getWidth();
            double gridHeight = height.get() / bounds.getHeight();

            gridSize = (int) Math.max(2, Math.round(Math.min(gridWidth, gridHeight)));

            redrawCanvas();
        }
    }

    private void redrawCanvas() {
        GraphicsContext graphics = this.chunkCanvas.getGraphicsContext2D();

        graphics.setStroke(Color.TRANSPARENT);
        graphics.setFill(BACKGROUND_COLOR);
        graphics.fillRect(0, 0, width.get(), height.get());

        for (Coordinate2D drawableChunk : drawableChunks) {
            drawChunk(drawableChunk);
        }
    }

    private Bounds getOverviewBounds(Collection<Coordinate2D> coordinates, Coordinate2D... others) {
        Bounds bounds = new Bounds();
        for (Coordinate2D coordinate : coordinates) {
            bounds.update(coordinate);
        }
        for (Coordinate2D coordinate : others) {
            bounds.update(coordinate);
        }
        return bounds;
    }

    /**
     * Computes the set of chunks in range, as well as building the set of all chunks we should draw (up to twice the
     * range due to pixels).
     * @return the set of chunks actually in range.
     */
    private Collection<Coordinate2D> getChunksInRange(Coordinate2D center, Collection<Coordinate2D> coords, int rangeX, int rangeZ) {
        return coords.parallelStream()
                .filter(coordinate2D -> coordinate2D.isInRange(center, rangeX, rangeZ))
                .collect(Collectors.toSet());
    }


    public void updatePlayerPos(CoordinateDouble3D playerPos, double rot) {
        this.playerPos = playerPos;
        this.playerRotation = rot;
    }


    private void redrawEntities() {
        GraphicsContext graphics = entityCanvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, width.get(), height.get());

        if (Config.renderOtherPlayers()) {
            for (PlayerEntity player : otherPlayers) {
                drawOtherPlayer(graphics, player);
            }
        }
        redrawPlayer(graphics);
    }

    /**
     * Draw another player on the map. If the cursor is nearby (max 10 pixels away), draw the name of the player.
     * If the name of the player is not known it's first requested from the Mojang API.
     */
    private void drawOtherPlayer(GraphicsContext graphics, PlayerEntity player) {
        double playerX = ((player.getPosition().getX() / 16.0 - bounds.getMinX()) * gridSize);
        double playerZ = ((player.getPosition().getZ() / 16.0 - bounds.getMinZ()) * gridSize);
        if (mouseOver && isNear(playerX, playerZ)) {
            graphics.setFill(Color.WHITE);

            if (player.getName() != null) {
                graphics.strokeText(player.getName(), playerX, playerZ - 5);
                graphics.fillText(player.getName(), playerX, playerZ - 5);
            }
        } else {
            graphics.setFill(PLAYER_COLOR);
        }
        graphics.setStroke(Color.BLACK);

        graphics.strokeOval(playerX - 3, playerZ - 3, 6, 6);
        graphics.fillOval(playerX - 3, playerZ - 3, 6, 6);
    }

    private boolean isNear(double x, double y) {
        return Math.abs(x - mouseX) < 10 && Math.abs(y - mouseY) < 10;
    }

    private void redrawPlayer(GraphicsContext graphics) {
        if (playerPos == null) { return; }
        double playerX = ((playerPos.getX() / 16.0 - bounds.getMinX()) * gridSize);
        double playerZ = ((playerPos.getZ() / 16.0 - bounds.getMinZ()) * gridSize);

        // direction pointer
        double yaw = Math.toRadians(this.playerRotation + 45);
        double pointerX = (playerX + (3)*Math.cos(yaw) - (3)*Math.sin(yaw));
        double pointerZ = (playerZ + (3)*Math.sin(yaw) + (3)*Math.cos(yaw));

        graphics.setFill(Color.WHITE);
        graphics.setStroke(Color.BLACK);
        graphics.strokeOval((int) playerX - 4, (int) playerZ - 4, 8, 8);
        graphics.strokeOval((int) pointerX - 2, (int) pointerZ - 2, 4, 4);
        graphics.fillOval((int) playerX - 4, (int) playerZ - 4, 8, 8);
        graphics.fillOval((int) pointerX - 2, (int) pointerZ - 2, 4, 4);

        // indicator circle
        graphics.setFill(Color.TRANSPARENT);
        graphics.setStroke(Color.RED);

        graphics.strokeOval((int) playerX - 16, (int) playerZ - 16, 32, 32);
    }

    private void drawChunk(Coordinate2D pos) {
        drawChunk(chunkCanvas.getGraphicsContext2D(), pos, this.bounds, this.gridSize, true);
    }

    private void drawChunk(GraphicsContext graphics, Coordinate2D pos, Bounds bounds, int gridSize, boolean drawBlack) {
        ChunkImage chunkImage = chunkMap.get(pos);

        int drawX = (pos.getX() - bounds.getMinX()) * gridSize;
        int drawY = (pos.getZ() - bounds.getMinZ()) * gridSize;

        if (chunkImage.getImage() == NONE) {
            graphics.setLineWidth(1);
            graphics.setFill(EXISTING_COLOR);
            graphics.setStroke(Color.WHITE);

            // offset by 1 since the stroke is centered on the border, not inside the shape
            graphics.strokeRect(drawX + 1, drawY + 1, gridSize - 1, gridSize - 1);
            graphics.fillRect(drawX, drawY,gridSize - 1, gridSize - 1);
        } else {
            // draw black before drawing chunk so that we can tell void from missing chunks
            drawImage(graphics, drawX, drawY, gridSize, chunkImage.getImage(), drawBlack);

            // if the chunk wasn't saved yet, mark it as such
            if (Config.markUnsavedChunks() && !chunkImage.isSaved) {
                graphics.setFill(UNSAVED_COLOR);
                graphics.setStroke(Color.TRANSPARENT);
                graphics.fillRect(drawX, drawY, gridSize, gridSize);
            }
        }
    }

    private void drawChunkAsync(Coordinate2D pos) {
        Platform.runLater(() -> drawChunk(pos));
    }

    public void export() {
        List<Coordinate2D> drawables = chunkMap.entrySet().stream()
                .filter((coord) -> coord.getValue() != NO_IMG)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Bounds fullBounds = getOverviewBounds(drawables);

        // set size limit so that we don't have memory issues with the output image
        int MAX_SIZE = 2 << 12;

        int width = fullBounds.getWidth() * 16;
        int height = fullBounds.getHeight() * 16;

        // half the size of the output so we keep nice round numbers, until the grid size is down to 1px/chunk
        int gridSize = 16;
        while (gridSize > 1 && (width > MAX_SIZE || height > MAX_SIZE)) {
            width /= 2;
            height /= 2;
            gridSize /= 2;
        }

        Canvas temp = new Canvas(width, height);
        GraphicsContext graphics = temp.getGraphicsContext2D();
        setSmoothingState(graphics, false);

        // draw each chunk
        for (Map.Entry<Coordinate2D, ChunkImage> entry : chunkMap.entrySet()) {
            if (entry.getValue() == NO_IMG) { continue; }

            drawChunk(graphics, entry.getKey(), fullBounds, gridSize, false);
        }

        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);
        WritableImage img = temp.snapshot(snapshotParameters, new WritableImage(width, height));

        try {
            File dest = PathUtils.toPath(Config.getWorldOutputDir(), "rendered.png").toFile();
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", dest);
            System.out.println("Saved overview to " + dest.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setDimension(Dimension dimension) {
        this.chunkMap = chunkDimensions.computeIfAbsent(dimension, (k) -> new ConcurrentHashMap<>());
    }

    public void showErrorMessage() {
        this.helpLabel.setText("An error has occured. 'Right click' -> 'Settings' to view.");
        this.helpLabel.setStyle("-fx-text-fill: red;");
        this.showErrorPrompt = true;
    }

    public void hideErrorMessage() {
        this.helpLabel.setText("");
        this.helpLabel.setStyle("-fx-text-fill: white;");
        this.showErrorPrompt = false;
    }

    public int countImages() {
        int total = 0;
        for (Map<Coordinate2D, ChunkImage> x : chunkDimensions.values()) {
            total += x.values().size();
        }
        return total;
    }
}

