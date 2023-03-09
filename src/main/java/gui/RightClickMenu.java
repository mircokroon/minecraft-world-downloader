package gui;

import config.Config;
import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkBinary;
import game.data.coordinates.Coordinate2D;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
import game.data.region.McaFile;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.*;
import javafx.stage.Stage;
import util.PathUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.List;

public class RightClickMenu extends ContextMenu {
    final static String PROMPT_PAUSE = "Pause chunk saving";
    final static String PROMPT_RESUME = "Resume chunk saving";


    public RightClickMenu(GuiMap handler) {
        List<MenuItem> menu = this.getItems();

        menu.add(construct(PROMPT_PAUSE, event -> {
            MenuItem item =  ((MenuItem) event.getTarget());
            if (WorldManager.getInstance().isPaused()) {
                WorldManager.getInstance().resumeSaving();
                item.setText(PROMPT_PAUSE);
            } else {
                WorldManager.getInstance().pauseSaving();
                item.setText(PROMPT_RESUME);
            }
            handler.setStatusMessage("");
        }));

        menu.add(construct("Delete all downloaded chunks", e -> {
            Alert alert = new Alert(Alert.AlertType.NONE,
                    "Are you sure you want to delete all downloaded chunks? This cannot be undone.",
                    ButtonType.CANCEL, ButtonType.YES
            );
            GuiManager.addIcon((Stage) alert.getDialogPane().getScene().getWindow());
            alert.setTitle("Confirm delete");
            alert.getDialogPane().getStylesheets().add(getClass().getResource("/ui/dark.css").toExternalForm());
            alert.showAndWait();

            if (alert.getResult() == ButtonType.YES) {
                WorldManager.getInstance().deleteAllExisting();
            }
        }));


        menu.add(new SeparatorMenuItem());

        menu.add(construct("Redraw nearby chunks", e -> {
            Coordinate2D region = handler.getCursorCoordinates().globalToRegion();
            new Thread(() -> WorldManager.getInstance().drawExistingChunks(region)).start();
        }));

        menu.add(construct("Redraw region", e -> {
            Coordinate2D region = handler.getCursorCoordinates().globalToRegion();
            handler.getRegionHandler().resetRegion(region);
            new Thread(() -> WorldManager.getInstance().drawExistingRegion(region)).start();
        }));

        menu.add(construct("Copy coordinates", e -> {
            Coordinate2D coords = handler.getCursorCoordinates();
            String coordsString = String.format("%d ~ %d", coords.getX(), coords.getZ());
            StringSelection selection = new StringSelection(coordsString);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        }));

        menu.add(new SeparatorMenuItem());

        menu.add(construct("Settings", e -> GuiManager.loadWindowSettings()));

        menu.add(construct("Save & Exit", e -> {
            GuiManager.saveAndExit();
        }));

        if (Config.isInDevMode()) {
            addDevOptions(menu, handler);
        }
    }

    private void addDevOptions(List<MenuItem> menu, GuiMap handler) {
        menu.add(new SeparatorMenuItem());

        menu.add(construct("Write chunk 0, 0", e -> {
            Path p = PathUtils.toPath(Config.getWorldOutputDir(), "", "region", "r.0.0.mca");
            ChunkBinary cb = new McaFile(p.toFile()).getChunkBinary(new CoordinateDim2D(0, 0, Dimension.OVERWORLD));

            String filename = "chunkdata.bin";
            FileOutputStream f = new FileOutputStream(filename);
            ObjectOutputStream o = new ObjectOutputStream(f);
            o.writeObject(cb);

            System.out.println("Write chunk (0, 0) to " + filename);
        }));

        menu.add(construct("Write all chunks as text", e -> {
           Config.toggleWriteChunkNbt();
        }));

        menu.add(construct("Print stats", e -> {
            int regions = WorldManager.getInstance().countActiveRegions();
            int binaryChunks = WorldManager.getInstance().countActiveBinaryChunks();
            int unpasedChunks = WorldManager.getInstance().countQueuedChunks();
            int chunks = WorldManager.getInstance().countActiveChunks();
            int entities = WorldManager.getInstance().getEntityRegistry().countActiveEntities();
            int players = WorldManager.getInstance().getEntityRegistry().countActivePlayers();
            int maps = WorldManager.getInstance().getMapRegistry().countActiveMaps();
            int images = handler.imageCount();

            System.out.printf("Statistics:" +
                            "\n\tActive regions: %d" +
                            "\n\tActive binary chunks: %d" +
                            "\n\tActive unparsed chunks: %d" +
                            "\n\tActive chunks: %d" +
                            "\n\tActive entities: %d" +
                            "\n\tActive players: %d" +
                            "\n\tActive maps: %d" +
                            "\n\tActive region images: %d" +
                            "\n",
                    regions, binaryChunks, unpasedChunks, chunks, entities, players, maps, images);
        }));

        menu.add(construct("Print chunk events", e -> {
            Chunk.printEventLog(handler.getCursorCoordinates().globalToChunk().addDimension(Dimension.OVERWORLD));
        }));
    }

    private MenuItem construct(String name, HandleError handler) {
        MenuItem item = new MenuItem(name);
        item.addEventHandler(EventType.ROOT, handler);
        return item;
    }
}

interface HandleError extends EventHandler<Event> {
    @Override
    default void handle(Event event) {
        try {
            handleErr(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void handleErr(Event event) throws IOException;
}