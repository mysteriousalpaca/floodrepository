import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;

// Main window setup for the game
public class FloodBuilder extends JFrame {

    private static final int GRID_SIZE = 20; // How many tiles wide/long the map is
    private static final int TILE_WIDTH = 64; // Visual width of one tile
    private static final int TILE_HEIGHT = 32; // Visual height of one tile

    private int budget = 5000; // Starting money
    private String gamePhase = "PREPARATION"; // Tracks if we are building or flooding
    private Grid grid;
    private FloodEngine floodEngine;
    private GameLogic logic;

    // Variables to track what structure the user is currently clicking and dragging
    private StructureType draggingType = null;
    private Point dragPoint = null;

    enum StructureType { WALL, SANDBAG } //Choose to use an enumeration, which behaves as a safer storage than a string or int
    //I found online that enumerations behave similar to classes, guaranteeing that only walls or sandbags exist.   

    public FloodBuilder() {
        grid = new Grid(GRID_SIZE);
        floodEngine = new FloodEngine();
        logic = new GameLogic(grid);

        // Sets up the specific spots on the map that the player must protect
        logic.placeDangerTiles(new int[][]{ //Change or remove these if you want to remove the floodtiles to view the algorithm for flooding better 
            {5, 5}, {5, 6}, {6, 5}, {6, 6}, {10, 10}, {11, 10}
        });

        // Basic window settings
        setTitle("FLOODBUILDER - DYLAN's CULMINATING");
        setSize(1100, 850);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        GamePanel viewport = new GamePanel();
        add(viewport, BorderLayout.CENTER);

        // This timer runs every 300ms to update the flood and game state
        Timer gameTimer = new Timer(300, e -> {
            if (gamePhase.equals("FLOODING")) {
                floodEngine.update(grid); // Spread the water

                logic.decaySandbags(); // Make sandbags weaker over time

                // Check if the water has reached a protected tile
                if (logic.checkDangerFlooded()) {
                    JOptionPane.showMessageDialog(this, "Danger tile flooded! Game Over!");
                    gamePhase = "GAMEOVER";
                    return;
                }

                repaint(); // Redraw the screen
            }
        });
        gameTimer.start();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Base template for things we can build
    abstract class Structure {
        int cost; 
        abstract void render(Graphics2D g, int px, int py, boolean isGhost);
    }

    class Wall extends Structure {
        /**
         * Structure Cost: Walls are permanent, high-durability barriers.
         * The cost is set high to force strategic placement rather than spamming. (DYLAN)
         */
        public Wall() { this.cost = 1000; } 
        
        @Override
        void render(Graphics2D g, int px, int py, boolean isGhost) {
            // Draw a 3D-looking block using shapes (AI)
            if (isGhost) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            int h = 35, w = TILE_WIDTH / 2, hh = TILE_HEIGHT / 2;

            Path2D.Double left = new Path2D.Double();
            left.moveTo(px, py + hh); left.lineTo(px - w, py); left.lineTo(px - w, py - h); left.lineTo(px, py + hh - h); left.closePath();
            Path2D.Double right = new Path2D.Double();
            right.moveTo(px, py + hh); right.lineTo(px + w, py); right.lineTo(px + w, py - h); right.lineTo(px, py + hh - h); right.closePath();
            Path2D.Double top = new Path2D.Double();
            top.moveTo(px, py + hh - h); top.lineTo(px + w, py - h); top.lineTo(px, py - hh - h); top.lineTo(px - w, py - h); top.closePath();

            g.setColor(new Color(110, 110, 110)); g.fill(left);
            g.setColor(new Color(80, 80, 80)); g.fill(right);
            g.setColor(new Color(150, 150, 150)); g.fill(top);
            g.setColor(Color.BLACK); g.draw(left); g.draw(right); g.draw(top);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }

    class Sandbag extends Structure {
        int life = 10; // Sandbags disappear after 10 "ticks" of water contact
        
        /**
         * Structure Cost: Sandbags are cheap, temporary defenses.
         * They allow the player to react quickly but require replacement.
         */
        public Sandbag() { this.cost = 300; }
        
        @Override
        void render(Graphics2D g, int px, int py, boolean isGhost) {
            // Draw a simple oval to look like a bag (AI)
            if (isGhost) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g.setColor(new Color(194, 178, 128));
            g.fillOval(px - 15, py - 5, 30, 15);
            g.setColor(Color.BLACK); g.drawOval(px - 15, py - 5, 30, 15);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        public void decay() { life--; }
        public boolean isDead() { return life <= 0; }
    }

   
    class GameLogic {
        private Grid grid;
        public GameLogic(Grid grid) { this.grid = grid; }

        /**
         * Accepts a 2D array of coordinates and flags specific grid tiles as "Danger" zones.
         * @param positions An array of [x, y] integer pairs representing grid indices.
         */
        public void placeDangerTiles(int[][] positions) {
            // Iterate through the list of coordinate pairs provided
            for (int[] pos : positions) {
                int x = pos[0], y = pos[1];
                // Verification step: Ensure coordinates are within the allocated memory bounds of the grid
                // This prevents ArrayIndexOutOfBounds exceptions if the user inputs a point like {99, 99}
                if (x >= 0 && x < grid.tiles.length && y >= 0 && y < grid.tiles[0].length) {
                    // Set the boolean flag for this specific tile to true, triggering a unique render color
                    grid.tiles[x][y].isDanger = true;
                }
            }
        }

        /**
         * Iterates through the entire grid to manage the lifecycle of temporary structures.
         * Specifically targets objects of the Sandbag class to simulate erosion.
         */
        public void decaySandbags() {
            // Double-nested loop to traverse every cell in the 2D Tile array
            for (int i = 0; i < grid.tiles.length; i++) {
                for (int j = 0; j < grid.tiles[i].length; j++) {
                    // Check if the current tile contains a structure AND if that structure is an instance of Sandbag
                    // This uses polymorphism to ensure we don't accidentally try to 'decay' a Wall
                    if (grid.tiles[i][j].structure instanceof Sandbag) {
                        Sandbag s = (Sandbag) grid.tiles[i][j].structure;
                        s.decay(); // Decrements the internal 'life' counter of the bag
                        
                        // Lifecycle termination: if durability reaches 0, nullify the reference
                        // This removes the structure from the grid, making the tile traversable by water again
                        if (s.isDead()) grid.tiles[i][j].structure = null;
                    }
                }
            }
        }

        /**
         * Evaluates the win/loss state by checking for an intersection between "Danger" tiles and "Wet" tiles.
         * @return true if any tile marked as Danger has its isWet flag set to true.
         */
        public boolean checkDangerFlooded() {
            for (int i = 0; i < grid.tiles.length; i++) {
                for (int j = 0; j < grid.tiles[i].length; j++) {
                    // Logical AND operation: Tile must be both a protection target AND currently submerged
                    if (grid.tiles[i][j].isDanger && grid.tiles[i][j].isWet) {
                        return true; // Immediate exit upon finding a single failure point
                    }
                }
            }
            return false; // Grid is safe
        }

        /**
         * Scans the grid for tiles that do not currently have a structure assigned to them.
         * Useful for AI placement logic or finding valid build locations.
         * @return A List of Point objects representing the (x, y) indices of vacant tiles.
         */
        public List<Point> getEmptyTiles() {
            List<Point> empty = new ArrayList<>();
            for (int i = 0; i < grid.tiles.length; i++) {
                for (int j = 0; j < grid.tiles[i].length; j++) {
                    // Reference check: if the structure pointer is null, the tile is unoccupied
                    if (grid.tiles[i][j].structure == null) {
                        // Create a new spatial Point object and add it to the dynamic list
                        empty.add(new Point(i, j));
                    }
                }
            }
            return empty; // Returns a collection of all available construction coordinates
        }
    }

    
    // The panel where the actual drawing happens
    class GamePanel extends JPanel {
        private Rectangle wallSource, bagSource, rotateBtn, startBtn;

        public GamePanel() {
            setBackground(new Color(30, 30, 30));
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // Check if we clicked on a UI button or a buildable item (DYLAN) !!!!!
                    if (wallSource.contains(e.getPoint())){
                         draggingType = StructureType.WALL;  //
                    }
                    else if (bagSource.contains(e.getPoint())){
                         draggingType = StructureType.SANDBAG; 
                    }
                    else if (rotateBtn.contains(e.getPoint())) {
                        rotateWorld90();
                    }
                    else if (startBtn.contains(e.getPoint())) {
                        gamePhase = "FLOODING";
                        floodEngine.startFlood(0, 0, grid);
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    // Follow the mouse while dragging a wall/sandbag
                    if (draggingType != null) {
                        dragPoint = e.getPoint();
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    // When the mouse is let go, try to place the structure
                    if (draggingType != null) {
                        int ox = getWidth() / 2, oy = 100;
                        double dx = e.getX() - ox, dy = e.getY() - oy;
                        
                        // Math to convert 2D screen pixels back into Isometric grid coordinates
                        int ix = (int) Math.floor((dx / (TILE_WIDTH / 2.0) + dy / (TILE_HEIGHT / 2.0)) / 2.0);
                        int iy = (int) Math.floor((dy / (TILE_HEIGHT / 2.0) - dx / (TILE_WIDTH / 2.0)) / 2.0);

                        // If the drop is inside the grid and we are still in preparation phase
                        if (ix >= 0 && ix < GRID_SIZE && iy >= 0 && iy < GRID_SIZE && gamePhase.equals("PREPARATION")) {
                            if (grid.tiles[ix][iy].structure == null) {
                                Structure s = (draggingType == StructureType.WALL) ? new Wall() : new Sandbag();
                                // Check if player has enough money
                                if (budget >= s.cost) {
                                    grid.tiles[ix][iy].structure = s;
                                    budget -= s.cost;
                                }
                            }
                        }
                        draggingType = null; dragPoint = null;
                        repaint();
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        // Handles the math for rotating the grid tiles by 90 degrees
        private void rotateWorld90() {
            Tile[][] newTiles = new Tile[GRID_SIZE][GRID_SIZE];
            for (int i = 0; i < GRID_SIZE; i++)
                for (int j = 0; j < GRID_SIZE; j++)
                    newTiles[j][GRID_SIZE - 1 - i] = grid.tiles[i][j];
            grid.tiles = newTiles;
            floodEngine.rebuildQueue(grid); // Update the flood water logic to the new positions
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int ox = getWidth() / 2, oy = 100;

            // Draw every tile in the grid using Isometric Projection
            for (int i = 0; i < GRID_SIZE; i++) //X axis
                for (int j = 0; j < GRID_SIZE; j++) { //Y axis
                    int px = (int) ((i - j) * (TILE_WIDTH / 2.0)) + ox;
                    int py = (int) ((i + j) * (TILE_HEIGHT / 2.0)) + oy;
                    grid.tiles[i][j].render(g2, px, py);
                }

            drawHUD(g2); // Draw the buttons and budget

            // If dragging, draw a "ghost" version of the wall under the mouse
            if (draggingType != null && dragPoint != null) {
                Structure ghost = (draggingType == StructureType.WALL) ? new Wall() : new Sandbag();
                ghost.render(g2, dragPoint.x, dragPoint.y, true);
            }
        }

        // Draw the user interface at the bottom of the screen
        private void drawHUD(Graphics2D g2) {
            int bw = 80, bh = 80;
            int hudWidth = 450;
            int startX = (getWidth() - hudWidth) / 2;
            int startY = getHeight() - 120;

            g2.setColor(new Color(50, 50, 50, 220));
            g2.fillRoundRect(startX - 10, startY - 10, hudWidth + 20, 110, 15, 15);

            wallSource = new Rectangle(startX, startY, bw, bh);
            bagSource = new Rectangle(startX + 90, startY, bw, bh);
            rotateBtn = new Rectangle(startX + 180, startY, bw, bh);
            startBtn = new Rectangle(startX + 270, startY, 160, bh);

            g2.setColor(Color.DARK_GRAY); g2.fill(wallSource);
            new Wall().render(g2, wallSource.x + bw/2, wallSource.y + bh/2 + 10, false);

            g2.setColor(Color.DARK_GRAY); g2.fill(bagSource);
            new Sandbag().render(g2, bagSource.x + bw/2, bagSource.y + bh/2, false);

            g2.setColor(new Color(70, 130, 180)); g2.fill(rotateBtn);
            g2.setColor(Color.WHITE); g2.drawString("ROTATE", rotateBtn.x + 15, rotateBtn.y + bh/2 + 5);

            g2.setColor(new Color(180, 50, 50)); g2.fill(startBtn);
            g2.setColor(Color.WHITE); g2.drawString("RELEASE FLOOD", startBtn.x + 30, startBtn.y + bh/2 + 5);

            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.drawString("BUDGET: $" + budget, startX + 10, startY - 20);
        }
    }

    // Represents a single square on the map
    class Tile {
        Structure structure = null;
        boolean isWet = false;
        boolean isDanger = false;
        public void render(Graphics2D g, int px, int py) {
            Polygon d = new Polygon(); // Shape of an isometric diamond
            d.addPoint(px, py - TILE_HEIGHT / 2);
            d.addPoint(px + TILE_WIDTH / 2, py);
            d.addPoint(px, py + TILE_HEIGHT / 2);
            d.addPoint(px - TILE_WIDTH / 2, py);
            
            // Set colors: Red for danger, Blue for water, Green for grass
            if (isDanger) g.setColor(new Color(200, 0, 0));
            else g.setColor(isWet ? new Color(40, 100, 200) : new Color(70, 130, 50));
            
            g.fill(d); g.setColor(new Color(255, 255, 255, 30)); g.draw(d);
            if (structure != null) structure.render(g, px, py, false);
        }
    }

    class Grid {
        Tile[][] tiles;
        public Grid(int size) {
            tiles = new Tile[size][size];
            for (int i = 0; i < size; i++)
                for (int j = 0; j < size; j++) tiles[i][j] = new Tile();
        }
        public void decaySandbags() {
            // Already handled in GameLogic, but this is a secondary helper if needed
            for (int i = 0; i < tiles.length; i++)
                for (int j = 0; j < tiles[i].length; j++)
                    if (tiles[i][j].structure instanceof Sandbag) {
                        Sandbag s = (Sandbag) tiles[i][j].structure;
                        s.decay();
                        if (s.isDead()) tiles[i][j].structure = null;
                    }
        }
    }
//-------------------------------------------------------------------------------------------------------------------------
    // FLOOD ENGINE BY DYLAN 
    // This uses a "Breadth-First Search" (BFS) logic to spread water
    class FloodEngine {
        private Queue<Point> queue = new LinkedList<>(); //I searched online and found that linkedlist might be inefficient, but it is the easiest way to do a queue search
        
        // Starts the water at a specific point
        public void startFlood(int x, int y, Grid grid) {
            queue.clear();
            queue.add(new Point(x, y)); //Origin point of the BFS 
            grid.tiles[x][y].isWet = true;
        }

        // If we rotate the world, we need to find all current water to keep spreading
        public void rebuildQueue(Grid grid) {
            queue.clear();
            for (int i = 0; i < GRID_SIZE; i++)
                for (int j = 0; j < GRID_SIZE; j++)
                    if (grid.tiles[i][j].isWet) queue.add(new Point(i, j));
        }

        // Logic to move water to adjacent tiles
//----------------------------------------------------------------------------------------------------------
        public void update(Grid grid) { //Each call to this function is one time step 
            int size = queue.size();  //controls the spread of water
            for (int i = 0; i < size; i++) {
                Point p = queue.poll();
                int[][] neighbors = {{p.x+1, p.y}, {p.x-1, p.y}, {p.x, p.y+1}, {p.x, p.y-1}};
                for (int[] n : neighbors) {
                    if (n[0] >= 0 && n[0] < GRID_SIZE && n[1] >= 0 && n[1] < GRID_SIZE) {
                        Tile target = grid.tiles[n[0]][n[1]];
                        // Water spreads IF the tile is dry AND there is no wall/sandbag in the way
                        if (!target.isWet && target.structure == null) {
                            target.isWet = true;
                            queue.add(new Point(n[0], n[1]));
                        }
                    }
                }
            }
        }
    }
//------------------------------------------------------------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(FloodBuilder::new);
    }
}
