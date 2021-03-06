package gui;

import config.Config;
import game.data.WorldManager;
import game.data.chunk.Chunk;
import game.data.chunk.ChunkBinary;
import game.data.chunk.ChunkImageFactory;
import game.data.coordinates.CoordinateDim2D;
import game.data.dimension.Dimension;
import game.data.region.McaFile;
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

        menu.add(construct("Prune overview images", e -> handler.unloadImages()));
        menu.add(construct("Draw nearby existing chunks", e -> {
            new Thread(() -> WorldManager.getInstance().drawExistingChunks(handler.getCenter())).start();
        }));
        menu.add(construct("Save overview to file", e -> handler.export()));

        menu.add(new SeparatorMenuItem());

        menu.add(construct("Settings", e -> GuiManager.loadWindowSettings()));

        menu.add(construct("Save & Exit", e -> {
            WorldManager.getInstance().save();
            System.exit(0);
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
            int chunks = WorldManager.getInstance().countActiveChunks();
            int entities = WorldManager.getInstance().getEntityRegistry().countActiveEntities();
            int players = WorldManager.getInstance().getEntityRegistry().countActivePlayers();
            int maps = WorldManager.getInstance().getMapRegistry().countActiveMaps();
            int images = handler.countImages();

            System.out.printf("Statistics:" +
                            "\n\tActive regions: %d" +
                            "\n\tActive binary chunks: %d" +
                            "\n\tActive chunks: %d" +
                            "\n\tActive entities: %d" +
                            "\n\tActive players: %d" +
                            "\n\tActive maps: %d" +
                            "\n\tActive chunk images:%d" +
                            "\n",
                    regions, binaryChunks, chunks, entities, players, maps, images);
        }));

        menu.add(construct("Print 0, 0 events", e -> {
            Chunk.printEventLog(new CoordinateDim2D(0, 0, Dimension.OVERWORLD));
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