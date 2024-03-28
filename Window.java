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

        public GridPanel()
        {
            panel = (JPanel) Window.this.frame.getContentPane();;
            panel.add(this);

            this.setIgnoreRepaint(true);
            this.createBufferStrategy(2);
            strategy = getBufferStrategy();
        }

        //@Override
        public void paintComponent(Graphics g)
        {
            //super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) strategy.getDrawGraphics();

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
                }
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
        private JTextField gridSizeTF;
        private JTextField deltaFrameTimeTF;
        private JTextField deltaTickTimeTF;
        private JLabel sL;
        private JLabel iL;
        private JLabel rL;
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
            add(debugCB);

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
            updateSIR(0, 0, 0);
            add(new JLabel(" "));
            add(sL);
            add(iL);
            add(rL);

            /*
             * FPS & TPS LABELS
             */

            fpsL = new JLabel();
            tpsL = new JLabel();
            updateTiming(0, 0);
            add(new JLabel(" "));
            add(fpsL);
            add(tpsL);
        }

        public void updateTiming(long fps, long tps)
        {
            this.fpsL.setText("FPS: " + fps);
            this.tpsL.setText("TPS: " + tps);
        }

        public void updateSIR(int s, int i, int r)
        {
            this.sL.setText("S: " + s);
            this.iL.setText("I: " + i);
            this.rL.setText("R: " + r);
        }
    }

    private Simulation sim;
    private JFrame frame;
    private Canvas grid;
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
    private static final int NUM_THREADS = 9;
    private ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

    public Window(Simulation sim)
    {
        this.sim = sim;

        this.debug = false;

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
        controls = new ControlPanel();
        frame.add(controls, BorderLayout.EAST);

        frame.pack();
        frame.setSize(800, 800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        grid = new GridPanel();
        //frame.add(grid.panel, BorderLayout.CENTER);
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

        // Update SIR
        sim.GetSIR();
        updateListCounts((int)tickCount);
    }

    private void update()
    {
        if (this.reset)
        {
            this.sim.reset(this.newGridSize);
            this.reset = false;
            ((ControlPanel)controls).updateSIR(0, 0, 0);
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

                    // if (tmpDeltaTime > targetFrameDelta)
                    // start skipping ticks at 2 fps instead of target framerate
                    if (tmpDeltaTime > 512.0)
                    {
                        tickTime -= targetFrameDelta;
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
        //grid.repaint();
        ((GridPanel)grid).paintComponent(grid.getGraphics());
        ((ControlPanel)controls).updateTiming(this.fps, this.tps);
        if ( sCounts.size() > 0 )
        {
            ((ControlPanel)controls).updateSIR(
                this.sCounts.get((int)tickCount - 1),
                this.iCounts.get((int)tickCount - 1),
                this.rCounts.get((int)tickCount - 1));
        }
    }
}
