import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferStrategy;
import java.util.*;
import java.util.concurrent.*;

public class Window
{
    private class GridPanel extends Canvas
    {
        private BufferStrategy strategy;
        public JPanel panel;
        private Dimension dim;

        // used for graph
        private final int boarder = 100;
        private double xScale;
        private double yScale;

        public GridPanel()
        {
            panel = (JPanel) Window.this.frame.getContentPane();;
            panel.add(this);

            this.setIgnoreRepaint(true);
            this.createBufferStrategy(2);
            strategy = getBufferStrategy();
        }

        private void drawGrid(Graphics2D g2d)
        {
            int gridSize = Window.this.debug ? Window.this.sim.borderedGridSize : Window.this.sim.gridSize;

            int communitySize = Window.this.sim.communitySize;
            double w = (double) dim.width / (gridSize * communitySize);
            double h = (double) dim.height / gridSize;
            double cellSize = Math.min(w, h);

            for (int com = 0; com < communitySize; com++)
            {
                for(int rows = 0; rows < gridSize; rows++)
                {
                    for(int cols = 0; cols < gridSize; cols++)
                    {
                        double x = cols * cellSize + (com * cellSize * gridSize);
                        double y = rows * cellSize;
    
                        CellState cs = Window.this.debug ?
                            Window.this.sim.grid[com][rows][cols].getState() :
                            Window.this.sim.getFromGrid(com, rows, cols).getState();
    
                        Rectangle2D.Double r = new Rectangle2D.Double(x, y, cellSize, cellSize);
    
                        switch (cs)
                        {
                            case CellState.SUSCEPTIBLE -> { g2d.setColor(Color.blue);  }
                            case CellState.INFECTIOUS  -> { g2d.setColor(Color.red);   }
                            case CellState.REMOVED     -> { g2d.setColor(Color.green); }
                            case CellState.BORDER      -> { g2d.setColor(Color.gray);  }
                            case CellState.LAND        -> { g2d.setColor(Color.white); }
                        }
    
                        g2d.fill(r);
                    }
                }
            }
        }

        private void drawHistory(Graphics2D g2d, ArrayList<Integer> history)
        {
            // setup graph variables
            double prevX;
            double prevY;

            // draw line graph for S history
            prevX = boarder;
            prevY = history.size() > 0 ? (double)(dim.height - (history.get(0) * yScale) - boarder) : boarder;
            for (int h : history)
            {
                double x = prevX + xScale;
                double y = (double)(dim.height - (h * yScale) - boarder);

                Shape l = new Line2D.Double(prevX, prevY, x, y);
                g2d.draw(l);

                prevX = x;
                prevY = y;
            }
        }

        private void drawGraph(Graphics2D g2d)
        {
            // get simulation sir data
            ArrayList<Integer> sHistory = Window.this.sCounts.get(Window.this.curSim);
            ArrayList<Integer> iHistory = Window.this.iCounts.get(Window.this.curSim);
            ArrayList<Integer> rHistory = Window.this.rCounts.get(Window.this.curSim);

            // get min and max height and width
            int maxY = Window.this.day > 0 ? sHistory.get(0) : 0;
            int minY = 0;

            int maxX = Window.this.day > 0 ? sHistory.size() : 1;
            int minX = 0;

            yScale = (double)(dim.height - boarder * 2) / maxY;
            xScale = (double)(dim.width - boarder * 2) / maxX;

            // draw graph outline
            g2d.setColor(Color.BLACK);
            g2d.drawLine(
                    boarder,
                    dim.height - boarder,
                    boarder,
                    boarder);

            g2d.drawLine(
                    boarder,
                    dim.height - boarder,
                    dim.width  - boarder,
                    dim.height - boarder);

            // draw graph labels
            String maxYS = Integer.toString(maxY);
            String minYS = Integer.toString(minY);
            String maxXS = Integer.toString(maxX);
            String minXS = Integer.toString(minX);

            g2d.drawString(maxYS, boarder - (maxYS.length() * 8) - 8, boarder - 8);
            g2d.drawString(minYS, boarder - (minYS.length() * 8) + 8, dim.height - boarder + 24);
            g2d.drawString(maxXS, dim.width - boarder, dim.height - boarder + 24);
            g2d.drawString(minXS, boarder - (minXS.length() * 8) - 8, dim.height - boarder);

            g2d.drawString("Days", dim.width / 2, dim.height - boarder + 24);
            g2d.drawString("Population", boarder - 64, dim.height / 2);

            // draw graph histories
            g2d.setColor(Color.BLUE);
            drawHistory(g2d, sHistory);

            g2d.setColor(Color.RED);
            drawHistory(g2d, iHistory);

            g2d.setColor(Color.GREEN);
            drawHistory(g2d, rHistory);
        }

        //@Override
        public void paintComponent(Graphics g)
        {
            //super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) strategy.getDrawGraphics();

            // get panel size
            dim = this.getSize();

            // clear screen
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, dim.width, dim.height);

            if (Window.this.drawGrid)
            {
                drawGrid(g2d);
            }
            else
            {
                drawGraph(g2d);
            }

            g2d.dispose();
            strategy.show();
        }
    }

    private class ControlPanel extends JPanel
    {
        private JButton resetB;
        private JButton pauseB;
        private JCheckBox debugCB;
        private JCheckBox graphCB;
        private JTextField gridSizeTF;
        private JTextField deltaFrameTimeTF;
        private JTextField deltaTickTimeTF;
        private JLabel sL;
        private JLabel iL;
        private JLabel rL;
        private JLabel statusL;
        private JLabel fpsL;
        private JLabel tpsL;

        public ControlPanel()
        {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            /*
             * RESET BUTTON
             */

            resetB = new JButton("Reset");
            resetB.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.reset = true;
                }
            });
            add(resetB);

            /*
             * PAUSE BUTTON
             */

            pauseB = new JButton("Pause");
            pauseB.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.pause = !Window.this.pause;
                    String s = Window.this.pause ? "Unpause" : "Pause";
                    pauseB.setText(s);
                }
            });
            add(pauseB);

            /*
             * BOARDER CHECKBOX
             */

            debugCB = new JCheckBox("Show Border");
            debugCB.setSelected(Window.this.debug);
            debugCB.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.debug = !Window.this.debug;
                }
            });
            add(new JLabel(" "));
            add(debugCB);

            /*
             * GRAPH CHECKBOX
             */

            graphCB = new JCheckBox("Show Graph");
            graphCB.setSelected(Window.this.debug);
            graphCB.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.drawGrid = !Window.this.drawGrid;
                }
            });
            add(graphCB);

            /*
             * FRAME TIME TEXT FIELD
             */

            deltaFrameTimeTF = new JTextField();
            deltaFrameTimeTF.setText(Double.toString(Window.this.targetFrameDelta));
            deltaFrameTimeTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, deltaFrameTimeTF.getPreferredSize().height));
            deltaFrameTimeTF.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.targetFrameDelta = Double.parseDouble(deltaFrameTimeTF.getText());
                }
            });
            add(new JLabel(" "));
            add(new JLabel("Frame Delta:"));
            add(deltaFrameTimeTF);

            /*
             * TICK TIME TEXT FIELD
             */

            deltaTickTimeTF = new JTextField();
            deltaTickTimeTF.setText(Double.toString(Window.this.targetTickDelta));
            deltaTickTimeTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, deltaTickTimeTF.getPreferredSize().height));
            deltaTickTimeTF.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.targetTickDelta = Double.parseDouble(deltaTickTimeTF.getText());
                }
            });
            add(new JLabel(" "));
            add(new JLabel("Tick Delta:"));
            add(deltaTickTimeTF);

            /*
             * GRID SIZE TEXT FIELD
             */

            gridSizeTF = new JTextField();
            gridSizeTF.setText(Integer.toString(Window.this.sim.gridSize));
            gridSizeTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, gridSizeTF.getPreferredSize().height));
            gridSizeTF.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.reset = true;
                    Window.this.newGridSize = Integer.parseInt(gridSizeTF.getText());
                }
            });
            add(new JLabel(" "));
            add(new JLabel("Grid Size:"));
            add(gridSizeTF);

            /*
             * SIR LABELS
             */

            sL = new JLabel();
            iL = new JLabel();
            rL = new JLabel();
            updateSIRLabel(0, 0, 0);
            add(new JLabel(" "));
            add(sL);
            add(iL);
            add(rL);

            /*
             * STATUS LABELS
             */

            statusL = new JLabel();
            add(new JLabel(" "));
            add(statusL);

            /*
             * FPS & TPS LABELS
             */

            fpsL = new JLabel();
            tpsL = new JLabel();
            updateTimingLabel(0, 0);
            add(new JLabel(" "));
            add(fpsL);
            add(tpsL);
        }

        public void updateTimingLabel(long fps, long tps)
        {
            this.fpsL.setText("FPS: " + fps);
            this.tpsL.setText("TPS: " + tps);
        }

        public void updateStatusLabel(String mes)
        {
            this.statusL.setText("Status: " + mes);
        }

        public void updateSIRLabel(int s, int i, int r)
        {
            this.sL.setText("S: " + s);
            this.iL.setText("I: " + i);
            this.rL.setText("R: " + r);
        }
    }

    private enum SimStatus
    {
        RUNNING,
        PAUSED,
        FINISHED
    }

    private Simulation sim;
    private SimStatus sStatus;

    private JFrame frame;
    private Canvas grid;
    private JPanel controls;

    // settings
    protected boolean drawGrid; // will draw graph on false
    protected boolean debug;
    protected boolean pause;
    protected boolean reset;
    protected int newGridSize;

    // simulation loop stuff
    protected boolean running;
    protected long frameCount;  // number of frames that have passed
    protected long tickCount;
    protected double targetFrameDelta; // target time between each frame
    protected double targetTickDelta;
    protected long fps;
    protected long tps;
    protected int curSim; // Simulation the model is currently on
    protected int day; // Current day of simulation

    // SIR values. The outer list represents the simulation #, and the inner list is the value for each day
    protected ArrayList<ArrayList<Integer>> sCounts, iCounts, rCounts;

    private static final int NUM_THREADS = 9;
    private ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

    public Window(Simulation sim)
    {
        this.sim = sim;
        this.sStatus = SimStatus.RUNNING;

        this.drawGrid = true;
        this.debug = false;
        this.running = false;
        this.pause = false;
        this.reset = false;
        this.newGridSize = sim.gridSize;

        // This will change how long each frame takes. Currently
        // set to 1000 milliseconds (1 second) per frame. Should
        // be lower later on.
        this.targetFrameDelta = 16;
        this.targetTickDelta = 20;

        this.fps = 0;
        this.tps = 0;

        // setup jframe window
        frame    = new JFrame("Epidemic Simulator");
        controls = new ControlPanel();
        frame.add(controls, BorderLayout.EAST);

        frame.pack();
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e)
            {
                Window.this.cleanup();
            }
        });

        grid = new GridPanel();
        //frame.add(grid, BorderLayout.CENTER);
    }

    private void tick()
    {
        if (day > 0 && this.iCounts.get(curSim).get(day - 1) == 0)
        {
            this.sStatus = getSimStatus();
            return;
        }

        ArrayList<Callable<Object>> tasks = new ArrayList<Callable<Object>>();

        // Update grid by running simulation step
        for (int i = 0; i < NUM_THREADS; i++)
        {
            // Submit tasks to executor for parallel execution
            final int threadNum = i;
            tasks.add(Executors.callable(() -> sim.simulationStep(threadNum)));
        }

        java.util.List<Future<Object>> f;
        try
        {
            // this will run and wait for each task
            f = executor.invokeAll(tasks);
        }
        catch(Exception e)
        {
            System.out.println(e);
        }

        // Update Cell Movement
        sim.cellMovement();

        // Update SIR
        updateListCounts();
        day++;
    }

    private SimStatus getSimStatus()
    {
        if (this.pause)
            return SimStatus.PAUSED;

        if (day > 0 && this.iCounts.get(curSim).get(day - 1) == 0)
            return SimStatus.FINISHED;

        return SimStatus.RUNNING;
    }

    private void update()
    {
        if (this.reset)
        {
            this.sim.reset(this.newGridSize);
            ((ControlPanel)controls).updateSIRLabel(0, 0, 0);

            // Reset start values
            Window.this.curSim++;
            Window.this.AddSIRData();

            this.reset = false;
        }

        // update simulation status
        this.sStatus = getSimStatus();
    }

    private void cleanup()
    {
        this.running = false;
        frame.setVisible(false);
        frame.dispose();
        executor.shutdown();

        // do final evaluation of simulations
        EvaluateData();
        System.exit(0);
    }

    // main loop for the simulation
    public void run() 
    {

        // the initial number of infected will start with 1 but may want to be changed later.
        sim.populateGrid(1);
        
        // init starting values
        this.running = true;
        this.frameCount = 0;
        InitializeSIRData();

        long prevFrameTime = System.currentTimeMillis();
        long frameTimer = prevFrameTime;
        long frameTimerLast = 0;
        long tickTimerLast = 0;
        double tickTime = 0;

        while (running)
        {
            // Timing data for setting the frame rate
            long frameTime = System.currentTimeMillis();
            long deltaTime = frameTime - prevFrameTime;

            // get fps and tps every second
            if ( frameTime - frameTimer >= 1000 )
            {
                long frames = this.frameCount - frameTimerLast;
                long ticks = this.tickCount - tickTimerLast;

                this.fps = frames;
                this.tps = ticks;

                frameTimerLast = this.frameCount;
                tickTimerLast = this.tickCount;

                frameTimer = frameTime;
            }

            prevFrameTime = frameTime;

            // do ticks
            if ( !this.pause )
            {
                tickTime += deltaTime;
                while ( tickTime >= this.targetTickDelta )
                {
                    long tmpFrameTime = System.currentTimeMillis();
                    long tmpDeltaTime = tmpFrameTime - prevFrameTime;

                    // if (tmpDeltaTime > targetFrameDelta)
                    // start skipping ticks at 2 fps instead of target framerate
                    if (tmpDeltaTime > 500.0)
                    {
                        tickTime -= 500.0;
                        break;
                    }

                    tick();
                    this.tickCount += 1;
                    tickTime -= this.targetTickDelta;
                }
            }

            update();
            render();

            this.frameCount += 1;

            // Apply framerate cap
            long delay = (long)(frameTime + this.targetFrameDelta - System.currentTimeMillis());

            try 
            {
                if (delay > 0)
                {
                    Thread.sleep(delay);
                }
            }
            catch (InterruptedException e)
            {
                this.running = false;
                break;
            }
        }
    }

    // Update SIR for this frame (tickCount)
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
        //grid.repaint();
        ((GridPanel)grid).paintComponent(grid.getGraphics());
        ((ControlPanel)controls).updateTimingLabel(this.fps, this.tps);

        if ( curSim >= 0 && day > 0 )
        {
            int s = this.sCounts.get(curSim).get(day - 1);
            int i = this.iCounts.get(curSim).get(day - 1);
            int r = this.rCounts.get(curSim).get(day - 1);
            ((ControlPanel)controls).updateSIRLabel(s, i, r);
        }

        switch (sStatus)
        {
            case SimStatus.RUNNING  -> { ((ControlPanel)controls).updateStatusLabel("running"); }
            case SimStatus.FINISHED -> { ((ControlPanel)controls).updateStatusLabel("finished"); }
            case SimStatus.PAUSED   -> { ((ControlPanel)controls).updateStatusLabel("paused"); }
        }
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
