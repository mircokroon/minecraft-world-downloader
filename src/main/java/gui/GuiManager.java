package gui;


import game.data.Coordinate2D;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class GuiManager {
    public static int WIDTH = 400;
    public static int HEIGHT = 400;

    private static GraphicsHandler graphicsHandler;

    public static void showGui() {
        SwingUtilities.invokeLater(GuiManager::createAndShowGUI);
    }

    public static void setChunkLoaded(Coordinate2D coord) {
        graphicsHandler.setChunkLoaded(coord);
    }

    public static void setChunkSaved(Coordinate2D coord) {
        graphicsHandler.setChunkSaved(coord);
    }




    private static void createAndShowGUI() {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(WIDTH, HEIGHT);

        graphicsHandler = new GraphicsHandler();
        f.add(graphicsHandler);
        f.pack();
        f.setVisible(true);

    }

}

class GraphicsHandler extends JPanel implements ActionListener {
    Graphics g;

    private int minX, maxX, minZ, maxZ, gridSize;
    private HashMap<Coordinate2D, Boolean> chunkMap = new HashMap<>();
    private boolean boundsChanged = false;

    Timer timer;

    public GraphicsHandler() {
        timer = new Timer(1000, this);
        timer.start();
    }

    public synchronized void setChunkLoaded(Coordinate2D coord) {
        if (!chunkMap.containsKey(coord)) {
            chunkMap.put(coord, false);
            computeBounds();
        }
    }

    public synchronized void setChunkSaved(Coordinate2D coord) {
        boolean inMap = chunkMap.containsKey(coord);
        chunkMap.put(coord, true);

        if (!inMap) {
            computeBounds();
        }

    }

    private void computeBounds() {
        boundsChanged = true;

        chunkMap.keySet().forEach(el -> {
        
        });

        maxX = chunkMap.keySet().stream().mapToInt(Coordinate2D::getX).max().orElse(0);
        minX = chunkMap.keySet().stream().mapToInt(Coordinate2D::getX).min().orElse(0);
        maxZ = chunkMap.keySet().stream().mapToInt(Coordinate2D::getZ).max().orElse(0);
        minZ = chunkMap.keySet().stream().mapToInt(Coordinate2D::getZ).min().orElse(0);

        int gridWidth = GuiManager.WIDTH / ((maxX - minX) + 1);
        int gridHeight = GuiManager.HEIGHT / ((maxZ - minZ) + 1);
        gridSize = Math.min(gridWidth, gridHeight);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(GuiManager.WIDTH, GuiManager.HEIGHT);
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        this.g = g;
        super.paintComponent(g);

        g.clearRect(0, 0, GuiManager.WIDTH, GuiManager.HEIGHT);
        chunkMap.forEach((key, value) -> {
            g.setColor(value ? Color.green : Color.BLUE);
            g.fillRect((key.getX() - minX) * gridSize, (key.getZ() - minZ) * gridSize, gridSize, gridSize);
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (boundsChanged) {
            repaint();
            boundsChanged = false;
        }
    }
}