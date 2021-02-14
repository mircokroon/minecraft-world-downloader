package gui;

import config.Config;
import game.data.coordinates.CoordinateDim2D;
import game.data.WorldManager;
import game.data.chunk.ChunkBinary;
import game.data.dimension.Dimension;
import game.data.region.McaFile;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        menu.add(construct("Delete all downloaded chunks", e -> WorldManager.getInstance().deleteAllExisting()));
        menu.add(new SeparatorMenuItem());

        menu.add(construct("Save overview to file", e -> handler.export()));
        menu.add(construct("Draw all existing chunks", e -> WorldManager.getInstance().drawExistingChunks()));

        menu.add(construct("Settings", e -> GuiManager.loadWindowSettings()));

        menu.add(construct("Save & Exit", e -> {
            WorldManager.getInstance().save();
            Platform.exit();
        }));

        if (Config.isInDevMode()) {
            addDevOptions(menu);
        }
    }

    private void addDevOptions(List<MenuItem> menu) {
        menu.add(new SeparatorMenuItem());

        menu.add(construct("Write chunk 0, 0", e -> {
            Path p = Paths.get(Config.getWorldOutputDir(), "", "region", "r.0.0.mca");
            ChunkBinary cb = new McaFile(p.toFile()).getChunkBinary(new CoordinateDim2D(0, 0, Dimension.OVERWORLD));

            String filename = "chunkdata.bin";
            FileOutputStream f = new FileOutputStream(filename);
            ObjectOutputStream o = new ObjectOutputStream(f);
            o.writeObject(cb);

            System.out.println("Write chunk (0, 0) to " + filename);
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