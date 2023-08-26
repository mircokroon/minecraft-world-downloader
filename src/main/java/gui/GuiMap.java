package gui;

import config.Config;
import game.data.chunk.ChunkImageFactory;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.CoordinateDim2D;
import game.data.coordinates.CoordinateDouble2D;
import game.data.coordinates.CoordinateDouble3D;
import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.dimension.Dimension;
import game.data.entity.PlayerEntity;
import gui.images.RegionImageHandler;
import gui.markers.PlayerMarker;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
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
import javafx.scene.paint.Paint;
import javafx.scene.shape.FillRule;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Controller for the map scene. Contains a canvas for chunks which is redrawn only when required, and one for entities
 * which can be redrawn any moment.
 */
public class GuiMap {
    private static final Color BACKGROUND_COLOR = new Color(.16, .16, .16, 1);
    private static final Color PLAYER_COLOR = new Color(.6, .95, 1, .7);
    private static final Paint PLAYER_STROKE = Color.color(.1f, .1f, .1f);

    public Canvas chunkCanvas;
    public Canvas entityCanvas;
    public Label helpLabel;
    public Label coordsLabel;
    public Button playerLockButton;
    public Label statusLabel;

    private CoordinateDouble3D playerPos;
    private Coordinate2D cursorPos;
    private double playerRotation;

    private double targetBlocksPerPixel;
    private double blocksPerPixel;
    private int gridSize;

    private RegionImageHandler regionHandler;
    private Collection<PlayerEntity> otherPlayers;

    ReadOnlyDoubleProperty width;
    ReadOnlyDoubleProperty height;

    private boolean mouseOver = false;
    private boolean playerHasConnected = false;
    private boolean showErrorPrompt = false;
    private boolean lockedToPlayer = true;

    private String statusMessage = "";

    // drag parameters
    private double mouseX, mouseY, initialMouseX, initialMouseY;
    private CoordinateDouble2D initialCenter = new CoordinateDouble2D(0, 0);
    private CoordinateDouble2D center = new CoordinateDouble2D(0, 0);
    private Bounds bounds;

    private ZoomBehaviour zoomBehaviour;

    private final PlayerMarker playerMarker = new PlayerMarker();

    @FXML
    void initialize() {
        this.zoomBehaviour = Config.smoothZooming() ? new SmoothZooming() : new SnapZooming();
        this.regionHandler = new RegionImageHandler();
        this.bounds = new Bounds();

        WorldManager manager = WorldManager.getInstance();
        this.otherPlayers = manager.getEntityRegistry().getPlayerSet();

        setDimension(manager.getDimension());
        this.playerPos = manager.getPlayerPosition().toDouble();

        manager.setPlayerPosListener(this::updatePlayerPos);
        GuiManager.setGraphicsHandler(this);

        GuiManager.getStage().setResizable(true);
        GuiManager.getStage().setHeight(500);
        GuiManager.getStage().setWidth(700);

        AnimationTimer animationTimer = new AnimationTimer() {
            @Override
            public void handle(long time) {
                zoomBehaviour.handle(time);
                computeBounds();
                drawWorld();
                drawEntities();
            }
        };
        animationTimer.start();

        zoomBehaviour.bind(entityCanvas);
        zoomBehaviour.onChange(newBlocksPerPixel -> {
            this.blocksPerPixel = newBlocksPerPixel;
            this.gridSize = (int) Math.round((32 * 16) / newBlocksPerPixel);
        });
        zoomBehaviour.onTargetChange(newTarget -> {
           this.targetBlocksPerPixel = newTarget; 
        });

        playerLockButton.setVisible(false);

        setupCanvasProperties();
        setupContextMenu();
        setupHelpLabel();
        setupDragging();
    }

    private void setupHelpLabel() {
        if (!playerHasConnected) {
            helpLabel.setText(Config.getConnectionDetails().getConnectionHint());
        }
        entityCanvas.setOnMouseEntered(e -> {
            mouseOver = true;
            if (playerHasConnected && !showErrorPrompt) {
                helpLabel.setText("Right-click to open context menu. Scroll or +/- to zoom. Drag to pan.");
            }
        });
        entityCanvas.setOnMouseMoved(e -> {
            this.mouseX = e.getSceneX();
            this.mouseY = e.getSceneY();

            int worldX = (int) Math.round((bounds.getMinX() + (mouseX * blocksPerPixel)));
            int worldZ = (int) Math.round((bounds.getMinZ() + (mouseY * blocksPerPixel)));

            cursorPos = new Coordinate2D(worldX, worldZ);

            String label = cursorPos.toString();
            if (Config.isInDevMode()) {
                label += String.format("\t\tchunk: %s", cursorPos.globalToChunk());
                label += String.format("\t\tregion: %s", cursorPos.globalToRegion());
            }
            coordsLabel.setText(label);
        });
        entityCanvas.setOnMouseExited(e -> {
            mouseOver = false;
            coordsLabel.setText("");
            cursorPos = null;
            if (playerHasConnected && !showErrorPrompt) {
                helpLabel.setText("");
            }
        });
    }

    /**
     * Handle dragging on the canvas. When dragging, a copy of the current canvas is made to provide a lightweight
     * visualisation of the dragging state. When dragging ends, the chunks are fully re-drawn.
     */
    private void setupDragging() {
        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);

        entityCanvas.setOnMousePressed((e) -> {
            if (e.getButton() != MouseButton.PRIMARY) { return; }

            if (lockedToPlayer) {
                this.initialCenter = playerPos;
            } else {
                this.initialCenter = center;
            }

            this.initialMouseX = mouseX;
            this.initialMouseY = mouseY;
        });

        entityCanvas.setOnMouseReleased((e) -> {
            if (e.getButton() != MouseButton.PRIMARY) { return; }

            if (!lockedToPlayer) {
                this.playerLockButton.setVisible(true);
            }
        });

        entityCanvas.setOnMouseDragged((e) -> {
            if (e.getButton() != MouseButton.PRIMARY) { return; }

            this.mouseX = e.getSceneX();
            this.mouseY = e.getSceneY();

            lockedToPlayer = false;

            double diffX = initialMouseX - mouseX;
            double diffY = initialMouseY - mouseY;

            CoordinateDouble2D difference = new CoordinateDouble2D(diffX * blocksPerPixel, diffY * blocksPerPixel);
            this.center = this.initialCenter.add(difference);
        });

        // button to reset the center back to the player
        playerLockButton.setOnMouseClicked(e -> followPlayer());
    }

    private void followPlayer() {
        lockedToPlayer = true;
        playerLockButton.setVisible(false);
    }

    private void setupCanvasProperties() {
        chunkCanvas.getGraphicsContext2D().setImageSmoothing(false);
        entityCanvas.getGraphicsContext2D().setImageSmoothing(true);

        entityCanvas.getGraphicsContext2D().setTextAlign(TextAlignment.CENTER);
        entityCanvas.getGraphicsContext2D().setFont(Font.font(null, FontWeight.BOLD, 14));

        Pane p = (Pane) chunkCanvas.getParent();
        width = p.widthProperty();
        height = p.heightProperty();

        chunkCanvas.widthProperty().bind(width);
        chunkCanvas.heightProperty().bind(height);

        entityCanvas.widthProperty().bind(width);
        entityCanvas.heightProperty().bind(height);

        chunkCanvas.setStyle("-fx-background-color: rgb(51, 151, 51)");
    }

    private void setupContextMenu() {
        ContextMenu menu = new RightClickMenu(this);
        entityCanvas.setOnContextMenuRequested(e -> menu.show(entityCanvas, e.getScreenX(), e.getScreenY()));
        entityCanvas.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {
				menu.hide();
			}
		});
    }

    public void clearChunks() {
        regionHandler.clear();
    }

    void setChunkLoaded(CoordinateDim2D coord, Chunk chunk) {
        if (!playerHasConnected) {
            playerHasConnected = true;

            if (!showErrorPrompt) {
                Platform.runLater(() -> helpLabel.setText(""));
            }
        }

        ChunkImageFactory imageFactory = chunk.getChunkImageFactory();
        imageFactory.onComplete((imageMap, isSaved) -> regionHandler.drawChunk(coord, imageMap, isSaved));
        imageFactory.onSaved(() -> regionHandler.markChunkSaved(coord));
        imageFactory.requestImage();
    }

    /**
     * Compute the bounds of the canvas based on the existing chunk data. Will also delete chunks that are out of the
     * set render distance. The computed bounds will be used to determine the scale and positions to draw the chunks to.
     */
    void computeBounds() {
        CoordinateDouble2D center;
        if (lockedToPlayer && this.playerPos != null) {
            center = this.playerPos;
        } else {
            center = this.center;
        }

        double blockWidth = width.intValue() * blocksPerPixel;
        double blockHeight = height.intValue() * blocksPerPixel;
        bounds.set(center, blockWidth, blockHeight);
    }

    private void drawWorld() {
        GraphicsContext graphics = this.chunkCanvas.getGraphicsContext2D();

        graphics.setFill(BACKGROUND_COLOR);
        graphics.fillRect(0, 0, width.get(), height.get());

        regionHandler.drawAll(bounds, targetBlocksPerPixel, this::drawRegion);

        drawDebugHighlight(graphics);
    }

    private void drawDebugHighlight(GraphicsContext graphics) {
        if (!Config.isInDevMode() || cursorPos == null) {
            return;
        }

        if (blocksPerPixel > 1) {
            Coordinate2D activeRegion = cursorPos.globalToRegion();
            int drawX = (int) Math.round(((32 * 16 * activeRegion.getX()) - bounds.getMinX()) / blocksPerPixel);
            int drawY = (int) Math.round(((32 * 16 * activeRegion.getZ()) - bounds.getMinZ()) / blocksPerPixel);

            graphics.setFill(Color.rgb(0, 0, 0, .2));
            graphics.fillRect(drawX, drawY, gridSize, gridSize);
        } else {
            Coordinate2D activeChunk = cursorPos.globalToChunk();
            int drawX = (int) Math.round(((16 * activeChunk.getX()) - bounds.getMinX()) / blocksPerPixel);
            int drawY = (int) Math.round(((16 * activeChunk.getZ()) - bounds.getMinZ()) / blocksPerPixel);

            graphics.setFill(Color.rgb(0, 0, 0, .2));
            graphics.fillRect(drawX, drawY, gridSize / 32, gridSize / 32);
        }
    }

    public void updatePlayerPos(CoordinateDouble3D playerPos, double rot) {
        this.playerPos = playerPos;
        this.playerRotation = rot;
    }

    private void drawEntities() {
        GraphicsContext graphics = entityCanvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, width.get(), height.get());

        if (Config.renderOtherPlayers()) {
            for (PlayerEntity player : otherPlayers) {
                drawOtherPlayer(graphics, player);
            }
        }
        drawPlayer(graphics);
    }

    /**
     * Draw another player on the map. If the cursor is nearby (max 10 pixels away), draw the name of the player.
     * If the name of the player is not known it's first requested from the Mojang API.
     */
    private void drawOtherPlayer(GraphicsContext graphics, PlayerEntity player) {
        double playerX = ((player.getPosition().getX() - bounds.getMinX()) / blocksPerPixel);
        double playerZ = ((player.getPosition().getZ() - bounds.getMinZ()) / blocksPerPixel);
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

    private void drawPlayer(GraphicsContext graphics) {
        if (playerPos == null) { return; }
        if (bounds == null) { return; }

        double playerX = ((playerPos.getX() - bounds.getMinX()) / blocksPerPixel);
        double playerZ = ((playerPos.getZ() - bounds.getMinZ()) / blocksPerPixel);

        playerMarker.transform(playerX, playerZ, this.playerRotation, 0.7);

        // marker
        graphics.setFillRule(FillRule.NON_ZERO);
        graphics.setFill(Color.WHITE);
        graphics.fillPolygon(playerMarker.getPointsX(), playerMarker.getPointsY(), playerMarker.count());
        graphics.setStroke(PLAYER_STROKE);
        graphics.strokePolygon(playerMarker.getPointsX(), playerMarker.getPointsY(), playerMarker.count());

        // indicator circle
        graphics.setStroke(Color.RED);
        graphics.strokeOval((int) playerX - 16, (int) playerZ - 16, 32, 32);
    }

    private void drawRegion(Coordinate2D pos, Image image) {
        if (image == null) {
            return;
        }
        GraphicsContext graphics = chunkCanvas.getGraphicsContext2D();

        Coordinate2D globalPos = pos.regionToGlobal();
        int drawX = (int) Math.round((globalPos.getX() - bounds.getMinX()) / blocksPerPixel);
        int drawY = (int) Math.round((globalPos.getZ() - bounds.getMinZ()) / blocksPerPixel);

        graphics.drawImage(image, drawX, drawY, gridSize, gridSize);
    }

    public void setDimension(Dimension dimension) {
        regionHandler.setDimension(dimension);
    }

    public void showErrorMessage() {
        this.showErrorPrompt = true;
        this.updateStatusPrompt();
    }

    public void hideErrorMessage() {
        this.showErrorPrompt = false;
        this.updateStatusPrompt();
    }

    private void updateStatusPrompt() {
        if (this.showErrorPrompt) {
            this.statusLabel.setText("An error has occured. 'Right click' -> 'Settings' to view.");
            this.statusLabel.setStyle("-fx-text-fill: red;");
        } else {
            this.statusLabel.setText(statusMessage);
            this.statusLabel.setStyle("-fx-text-fill: white;");
        }
    }

    public String imageStats() {
        return regionHandler.stats();
    }

    public Coordinate2D getCenter() {
        if (lockedToPlayer) {
            return playerPos.discretize().globalToChunk();
        } else {
            return center.discretize();
        }
    }

    public void setStatusMessage(String str) {
        if (str.equals("") && WorldManager.getInstance().isPaused()) {
            str = "Paused - right-click to resume";
        }
        this.statusMessage = str;

        Platform.runLater(this::updateStatusPrompt);
    }

    public Coordinate2D getCursorCoordinates() {
        int worldX = (int) Math.round((bounds.getMinX() + (mouseX * blocksPerPixel)));
        int worldZ = (int) Math.round((bounds.getMinZ() + (mouseY * blocksPerPixel)));
        
        return new Coordinate2D(worldX, worldZ);
    }

    public RegionImageHandler getRegionHandler() {
        return regionHandler;
    }

    public void setChunkState(Coordinate2D coords, ChunkImageState state) {
        regionHandler.setChunkState(coords, state);
    }
}
