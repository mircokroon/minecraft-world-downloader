package game.data.chunk;

import config.Config;
import game.data.coordinates.CoordinateDim2D;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ChunkEvents {
    private static final Map<CoordinateDim2D, List<ChunkEvent>> events = new HashMap<>();

    public ChunkEvents() { }

    public static void raiseEvent(CoordinateDim2D pos, String event) {
        if (!Config.trackEvents()) { return; }

        events.computeIfAbsent(pos, k -> new ArrayList<>()).add(new ChunkEvent(null, event));
    }

    public void raiseEvent(String event) {
        if (!Config.trackEvents()) { return; }

        events.computeIfAbsent(getLocation(), k -> new ArrayList<>()).add(new ChunkEvent(this, event));
    }

    protected void printEventLog() {
        System.out.println("Events for " + getLocation() + ": " + System.identityHashCode(this));
        if (events.get(getLocation()) == null) { return; }
        for (ChunkEvent event : events.get(getLocation())) {
            System.out.println("\t" + event);
        }
    }

    public static void printEventLog(CoordinateDim2D pos) {
        System.out.println("Events for " + pos + ": ");
        if (events.get(pos) == null) { return; }
        for (ChunkEvent event : events.get(pos)) {
            System.out.println("\t" + event);
        }
    }

    public abstract CoordinateDim2D getLocation();
}

class ChunkEvent {
    int hash;
    String time;
    String msg;

    public ChunkEvent(ChunkEvents obj, String msg) {
        this.hash = System.identityHashCode(obj);
        this.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        this.msg = msg;
    }

    @Override
    public String toString() {
        return hash + "@" + time + ": " + msg;
    }
}
