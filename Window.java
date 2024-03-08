import javax.swing.*;
import java.awt.*;

public class Window
{
    private class GridPanel extends JPanel
    {
        private Simulation sim;

        public GridPanel(Simulation sim)
        {
            this.sim = sim;
        }

        @Override
        public void paint(Graphics g)
        {
            super.paint(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // draw grid
            Dimension dim = this.getSize();
            int w = dim.width / sim.borderedGridSize;
            int h = dim.height / sim.borderedGridSize;
            int size = Math.min(w, h);

            for(int rows = 0; rows < sim.borderedGridSize; rows++)
            {
                for(int cols = 0; cols < sim.borderedGridSize; cols++)
                {
                    int x = cols * size;
                    int y = rows * size;

                    switch (sim.grid[rows][cols].getState())
                    {
                        case CellState.SUSCEPTIBLE -> { g2d.setColor(Color.blue);  g2d.fillRect(x, y, size, size); } 
                        case CellState.INFECTIOUS  -> { g2d.setColor(Color.red);   g2d.fillRect(x, y, size, size); }
                        case CellState.REMOVED     -> { g2d.setColor(Color.green); g2d.fillRect(x, y, size, size); }
                        case CellState.BORDER      -> { g2d.setColor(Color.gray);  g2d.fillRect(x, y, size, size); }
                    }

                    g2d.setColor(Color.black);
                    g2d.drawRect(x, y, size, size);
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

    // simulation loop stuff
    private boolean running;
    private int frameCount;  // number of frames that have passed
    private int targetFrameDelta; // target time between each frame

    public Window(Simulation sim)
    {
        this.sim = sim;

        this.running = false;

        // This will change how long each frame takes. Currently
        // set to 1000 milliseconds (1 second) per frame. Should
        // be lower later on.
        this.targetFrameDelta = 1000;

        // setup jframe window
        frame    = new JFrame("Epidemic Simulator");
        grid     = new GridPanel(sim);
        controls = new ControlPanel();

        frame.add(grid);

        frame.setSize(600, 400);
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
            printGrid();
            System.out.println();
            System.out.println();

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

    private void printGrid()
    {
        for(int rows = 0; rows < sim.borderedGridSize; rows++)
        {
            for(int cols = 0; cols < sim.borderedGridSize; cols++)
            {
                if(sim.grid[rows][cols].getState() == CellState.SUSCEPTIBLE)
                {
                    System.out.printf("S ", sim.grid[rows][cols]);
                }
                if(sim.grid[rows][cols].getState() == CellState.BORDER)
                {
                    System.out.printf("B ", sim.grid[rows][cols]);
                }
                if(sim.grid[rows][cols].getState() == CellState.INFECTIOUS)
                {
                    System.out.printf("I ", sim.grid[rows][cols]);
                }
                if(sim.grid[rows][cols].getState() == CellState.REMOVED)
                {
                    System.out.printf("R ", sim.grid[rows][cols]);
                }
            }
            System.out.println();
        }
    }
}
