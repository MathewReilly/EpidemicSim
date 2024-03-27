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
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
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
        private JTextField deltaTickTimeTF;
        private JLabel fpsL;
        private JLabel tpsL;

        public ControlPanel()
        {
            resetB              = new JButton("Reset");
            pauseB              = new JButton("Pause");
            debugCB             = new JCheckBox("Show Border");
            deltaFrameTimeTF    = new JTextField();
            deltaTickTimeTF     = new JTextField();
            gridSizeTF          = new JTextField();
            fpsL                = new JLabel();
            tpsL                = new JLabel();

            resetB.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.reset = true;
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
                }
            });

            deltaFrameTimeTF.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.targetFrameDelta = Double.parseDouble(deltaFrameTimeTF.getText());
                }
            });

            deltaTickTimeTF.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.targetTickDelta = Double.parseDouble(deltaTickTimeTF.getText());
                }
            });

            gridSizeTF.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Window.this.reset = true;
                    Window.this.newGridSize = Integer.parseInt(gridSizeTF.getText());
                }
            });

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            //setLayout(new GridLayout(4, 1));

            // add stuff to panel
            add(resetB);
            add(pauseB);
            add(debugCB);

            add(new JLabel(" "));
            add(new JLabel("Frame Delta:"));
            add(deltaFrameTimeTF);

            add(new JLabel(" "));
            add(new JLabel("Tick Delta:"));
            add(deltaTickTimeTF);

            add(new JLabel(" "));
            add(new JLabel("Grid Size:"));
            add(gridSizeTF);

            add(new JLabel(" "));
            add(fpsL);
            add(tpsL);

            // set settings for things
            debugCB.setSelected(Window.this.debug);
            gridSizeTF.setText(Integer.toString(Window.this.sim.gridSize));
            gridSizeTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, gridSizeTF.getPreferredSize().height));
            deltaFrameTimeTF.setText(Double.toString(Window.this.targetFrameDelta));
            deltaFrameTimeTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, deltaFrameTimeTF.getPreferredSize().height));
            deltaTickTimeTF.setText(Double.toString(Window.this.targetTickDelta));
            deltaTickTimeTF.setMaximumSize(new Dimension(Integer.MAX_VALUE, deltaTickTimeTF.getPreferredSize().height));
            updateTiming(0, 0);
        }

        public void updateTiming(long fps, long tps)
        {
            this.fpsL.setText("FPS: " + fps);
            this.tpsL.setText("TPS: " + tps);
        }
    }

    private Simulation sim;
    private JFrame frame;
    private JPanel grid;
    private JPanel controls;

    // settings
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
    protected ArrayList<Integer> sCounts, iCounts, rCounts;
    private ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

    public Window(Simulation sim)
    {
        this.sim = sim;

        this.debug = true;

        this.running = false;
        this.pause = false;
        this.reset = false;
        this.newGridSize = sim.gridSize;

        // This will change how long each frame takes. Currently
        // set to 1000 milliseconds (1 second) per frame. Should
        // be lower later on.
        this.targetFrameDelta = 16;
        this.targetTickDelta = 1000;

        this.fps = 0;
        this.tps = 0;

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
    }

    private void tick()
    {
        ArrayList<Callable<Object>> tasks = new ArrayList<Callable<Object>>();

        // Update grid by running simulation step
        for (int i = 0; i < NUM_THREADS; i++)
        {
            // Submit tasks to executor for parallel execution
            final int threadNum = i;
            tasks.add(Executors.callable(() -> sim.simulationStep(threadNum)));
            //executor.submit(() -> sim.simulationStep(threadNum));
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

        updateListCounts((int)tickCount); // Update SIR
    }

    private void update()
    {
        if (this.reset)
        {
            this.sim.reset(this.newGridSize);
            this.reset = false;
        }
    }

    // main loop for the simulation
    public void run() 
    {

        // the initial number of infected will start with 1 but may want to be changed later.
        sim.populateGrid(1);
        
        // init starting values
        this.running = true;
        this.frameCount = 0;
        this.sCounts = new ArrayList<Integer>();
        this.iCounts = new ArrayList<Integer>();
        this.rCounts = new ArrayList<Integer>();

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

                    if (tmpDeltaTime > targetFrameDelta)
                    {
                        tickTime -= tmpDeltaTime;
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

        executor.shutdown();
    }

    // Update SIR for this frame (day)
    public void updateListCounts(int frameCount)
    {
        if (sCounts.size() < frameCount+1)
        {
            sCounts.add(0);
            iCounts.add(0);
            rCounts.add(0);
        }
        sCounts.set(frameCount, sim.sCount);
        iCounts.set(frameCount, sim.iCount);
        rCounts.set(frameCount, sim.rCount);
    }

    // if we want to draw to the screen later
    public void render()
    {
        grid.repaint();
        ((ControlPanel)controls).updateTiming(this.fps, this.tps);
    }
}
