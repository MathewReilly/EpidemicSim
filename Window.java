import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.concurrent.*;

public class Window
{

    private static final int NUM_THREADS = 9;

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
        private JButton resetB;
        private JButton pauseB;
        private JCheckBox debugCB;
        private JTextField gridSizeTF;
        private JTextField deltaFrameTimeTF;

        public ControlPanel()
        {
            resetB              = new JButton("Reset");
            pauseB              = new JButton("Pause");
            debugCB             = new JCheckBox("Show Border");
            deltaFrameTimeTF    = new JTextField();
            gridSizeTF          = new JTextField();

            resetB.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // Reset start values
                    Window.this.sim.reset();
                    Window.this.frameCount = 0;
                    Window.this.curSim++;
                    Window.this.AddSIRData();
                    Window.this.grid.repaint();
                }
            });

            pauseB.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.pause = !Window.this.pause;
                    String s = Window.this.pause ? "Unpause" : "Pause";
                    pauseB.setText(s);
                }
            });

            debugCB.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.debug = !Window.this.debug;
                    Window.this.grid.repaint();
                }
            });

            deltaFrameTimeTF.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.targetFrameDelta = Integer.parseInt(deltaFrameTimeTF.getText());
                }
            });

            gridSizeTF.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.sim.resetNewGridSize(Integer.parseInt(gridSizeTF.getText()));
                    Window.this.frameCount = 0;
                    Window.this.grid.repaint();
                }
            });

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            //setLayout(new GridLayout(4, 1));

            // add stuff to panel
            add(resetB);
            add(pauseB);
            add(debugCB);
            add(deltaFrameTimeTF);
            add(gridSizeTF);

            // set settings for things
            debugCB.setSelected(Window.this.debug);
            gridSizeTF.setText(Integer.toString(Window.this.sim.gridSize));
            gridSizeTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, gridSizeTF.getPreferredSize().height));
            deltaFrameTimeTF.setText(Integer.toString(Window.this.targetFrameDelta));
            deltaFrameTimeTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, deltaFrameTimeTF.getPreferredSize().height));
        }
    }

    private Simulation sim;
    private JFrame frame;
    private JPanel grid;
    private JPanel controls;

    // settings
    protected boolean debug;
    protected boolean pause;

    // simulation loop stuff
    protected boolean running;
    protected int frameCount;  // number of frames that have passed
    protected int tickCount;
    protected int targetFrameDelta; // target time between each frame
    protected int targetTickDelta;
    protected int curSim; // Simulation the model is currently on
    protected int day; // Current day of simulation

    // SIR values. The outer list represents the simulation #, and the inner list is the value for each day
    protected ArrayList<ArrayList<Integer>> sCounts, iCounts, rCounts;

    public Window(Simulation sim)
    {
        this.sim = sim;

        this.debug = true;

        this.running = false;
        this.pause = false;

        // This will change how long each frame takes. Currently
        // set to 1000 milliseconds (1 second) per frame. Should
        // be lower later on.
        this.targetFrameDelta = 1000;

        // setup jframe window
        frame    = new JFrame("Epidemic Simulator");
        grid     = new GridPanel();
        controls = new ControlPanel();

        frame.add(grid, BorderLayout.CENTER);
        frame.add(controls, BorderLayout.EAST);

        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Window.this.EvaluateData();
                frame.setVisible(false);
                frame.dispose();
            }
            });
    }

    // main loop for the simulation
    public void run() 
    {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        // the initial number of infected will start with 1 but may want to be changed later.
        sim.populateGrid(1);
        
        // init starting values
        this.running = true;
        this.frameCount = 0;
        InitializeSIRData();

        long prevFrameTime = System.currentTimeMillis();

        while (running)
        {
            // Timing data for setting the frame rate
            long frameTime = System.currentTimeMillis();
            long deltaTime = frameTime - prevFrameTime;
            prevFrameTime = frameTime;

            // Update grid by running simulation step
            if (!pause)
            {
                // Submit tasks to executor for parallel execution
                for (int i = 0; i < NUM_THREADS; i++)
                {
                    final int threadNum = i;
                    executor.submit(() -> sim.simulationStep(threadNum));
                }

                // Update SIR values and move to next day
                updateListCounts(); // Update SIR

                // Keeps track of model metrics in terminal
                //System.out.print("\nSim: " + (curSim+1) + ", Day: " + (day+1) + "\nS: " + sCounts.get(curSim).get(day));
                //System.out.println("\nI: " + iCounts.get(curSim).get(day) + "\nR: " + rCounts.get(curSim).get(day));
                day++;
            }
            render();

            this.frameCount += 1;

            // Apply framerate cap
            long delay = frameTime + this.targetFrameDelta - System.currentTimeMillis();
            try 
            {
                if (delay > 0) Thread.sleep(delay);
            } catch (InterruptedException e)
            {
                this.running = false;
                break;
            }
        }

        executor.shutdown();
    }

    // Update SIR for this frame (day)
    public void updateListCounts()
    {
        while (sCounts.get(curSim).size() < day+1)
        {
            sCounts.get(curSim).add(0);
            iCounts.get(curSim).add(0);
            rCounts.get(curSim).add(0);
        }
        
        int[] tempSIR = sim.GetSIR();
        sCounts.get(curSim).set(day, tempSIR[0]);
        iCounts.get(curSim).set(day,tempSIR[1]);
        rCounts.get(curSim).set(day, tempSIR[2]);
    }

    // if we want to draw to the screen later
    public void render()
    {
        grid.repaint();
    }

    // Initialize variables to store SIR data
    private void InitializeSIRData()
    {
        curSim = 0;
        sCounts = new ArrayList<ArrayList<Integer>>();
        iCounts = new ArrayList<ArrayList<Integer>>();
        rCounts = new ArrayList<ArrayList<Integer>>();
        AddSIRData();
    }

    // Add new list for next simulation
    private void AddSIRData()
    {
        day = 0;
        sCounts.add(new ArrayList<Integer>());
        iCounts.add(new ArrayList<Integer>());
        rCounts.add(new ArrayList<Integer>());
    }

    // Perform various calulations on SIR data
    // 1. Average, Lowest, and Highest length of epidemic
    // 2. Rate of simulations in which all cells are infected
    public void EvaluateData()
    {
        int hSimLen=0, lSimLen=0, completedSims=0;
        float avgSimLen = 0.0f, fullIRate = 0.0f;

        // Loop through simulations
        System.out.println("\nStarting Population: " + sim.GetStartingPopulation() + ", Infection Chance: " + String.format("%.2f", sim.GetInfectionChance()) + "%");
        for (int i=0; i<=curSim; i++)
        {
            // Skip if simulation is empty
            System.out.println("\nSimulation " + (i+1) + ":");
            if (sCounts.get(i).isEmpty())
            {
                System.out.println("No data");
                continue;
            }

            // Loop through days
            int j=0;
            do 
            {
                System.out.print("\tDay " + (j+1) + ":" + "\tS: " + (sCounts.get(i).get(j)));
                System.out.println("\tI: " + iCounts.get(i).get(j) + "\tR: " + rCounts.get(i).get(j));
                j++;
            } while (j < iCounts.get(i).size() && iCounts.get(i).get(j-1) != 0); // until no more indices or no more infected

            // Don't add to metrics if simulation wasn't completed
            if (iCounts.get(i).get(j-1) != 0)
            {
                System.out.println("\tSimulation unfinished.");
                continue;
            }

            // Calculate metrics
            completedSims++;
            avgSimLen += j;
            if (i==0 || j > hSimLen)
            {
                hSimLen = j;
            }
            if (i==0 || j < lSimLen)
            {
                lSimLen = j;
            }

            // if all cells got infected
            if (sCounts.get(i).get(j-1) == 0)
            {
                fullIRate++;
            }
        }
        if (completedSims > 0)
        {
            avgSimLen /= (float)completedSims;
            fullIRate /= (float)completedSims;
        }

        // Print metrics
        System.out.println("\nCompleted simulations: " + completedSims);
        if (completedSims == 0)
        {
            System.out.println("No data to evaluate.");
            return;
        }
        System.out.println("Average length of epidemic: " + String.format("%.2f", avgSimLen) + " days");
        System.out.println("Shortest epidemic: " + lSimLen + " days");
        System.out.println("Longest epidemic: " + hSimLen + " days");
        System.out.println("Rate in which all cells were infected: " + String.format("%.2f", (fullIRate*100)) + "%");
    }
}
