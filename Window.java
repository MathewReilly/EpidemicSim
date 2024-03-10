import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class Window
{
    private class GridPanel extends JPanel
    {
        @Override
        public void paint(Graphics g)
        {
            super.paint(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // draw grid
            Dimension dim = this.getSize();

            int gridSize = Window.this.debug ? Window.this.sim.borderedGridSize : Window.this.sim.gridSize;
            double w = (double) dim.width / gridSize;
            double h = (double) dim.height / gridSize;
            double cellSize = Math.min(w, h);

            for(int rows = 0; rows < gridSize; rows++)
            {
                for(int cols = 0; cols < gridSize; cols++)
                {
                    double x = cols * cellSize;
                    double y = rows * cellSize;

                    CellState cs = Window.this.debug ?
                        Window.this.sim.grid[rows][cols].getState() :
                        Window.this.sim.getFromGrid(rows, cols).getState();

                    Rectangle2D.Double r = new Rectangle2D.Double(x, y, cellSize, cellSize);

                    switch (cs)
                    {
                        case CellState.SUSCEPTIBLE -> { g2d.setColor(Color.blue);  } 
                        case CellState.INFECTIOUS  -> { g2d.setColor(Color.red);   }
                        case CellState.REMOVED     -> { g2d.setColor(Color.green); }
                        case CellState.BORDER      -> { g2d.setColor(Color.gray);  }
                    }

                    g2d.fill(r);
                    g2d.setColor(Color.black);
                    g2d.draw(r);
                }
            }
        }
    }

    private class ControlPanel extends JPanel
    {
    }

    private Simulation sim;
    private JFrame frame;
    private JPanel grid;
    private JPanel controls;

    // settings
    protected boolean debug;

    // simulation loop stuff
    private boolean running;
    private int frameCount;  // number of frames that have passed
    private int tickCount;
    private int targetFrameDelta; // target time between each frame
    private int targetTickDelta;

    public Window(Simulation sim)
    {
        this.sim = sim;

        this.debug = true;

        this.running = false;

        // This will change how long each frame takes. Currently
        // set to 1000 milliseconds (1 second) per frame. Should
        // be lower later on.
        this.targetFrameDelta = 1000;

        // setup jframe window
        frame    = new JFrame("Epidemic Simulator");
        grid     = new GridPanel();
        controls = new ControlPanel();

        frame.add(grid);

        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }

    // main loop for the simulation
    public void run() 
    {
        // the initial number of infected will start with 1 but may want to be changed later.
        sim.populateGrid(1);
        
        // init starting values
        this.running = true;
        this.frameCount = 0;

        long prevFrameTime = System.currentTimeMillis();

        while (running)
        {
            // timing data for setting the frame rate
            long frameTime = System.currentTimeMillis();
            long deltaTime = frameTime - prevFrameTime;
            prevFrameTime = frameTime;

            // update grid by running simulationstep
            // ON the simulation step - it is likely the method that should be done in parallel, this is because the range
            // can be given for each thread to iterate on.
            sim.simulationStep(-1); // -1 is used so default on switch is used

            // print grid to screen
            render();

            this.frameCount += 1;

            // apply framerate cap
            long delay = frameTime + this.targetFrameDelta - System.currentTimeMillis();
            try { if (delay > 0) Thread.sleep(delay); } catch (InterruptedException e) { this.running = false; break; }
        }
    }

    // if we want to draw to the screen later
    public void render()
    {
        grid.repaint();
    }
}
