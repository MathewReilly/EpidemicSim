import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

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
            int communitySize = Window.this.sim.communitySize;
            double w = (double) dim.width;
            double h = (double) dim.height / gridSize;
            double cellSize = Math.min(w, h);
            for (int com = 0; com < communitySize; com++) {
                for(int rows = 0; rows < gridSize; rows++)
                {
                    for(int cols = 0; cols < gridSize; cols++)
                    {
                        double x = cols * cellSize + (com * cellSize * Window.this.sim.borderedGridSize);
                        double y = rows * cellSize;
    
                        CellState cs = Window.this.debug ?
                            Window.this.sim.grid[com][rows][cols].getState() :
                            Window.this.sim.getFromGrid(com, rows, cols).getState();
    
                        Rectangle2D.Double r = new Rectangle2D.Double(x, y, cellSize, cellSize);
    
                        switch (cs)
                        {
                            case SUSCEPTIBLE: g2d.setColor(Color.blue); 
                            break;
                            case INFECTIOUS: g2d.setColor(Color.red);  
                            break; 
                            case REMOVED: g2d.setColor(Color.green); 
                            break;
                            case BORDER: g2d.setColor(Color.gray);  
                            break;
                            case LAND: g2d.setColor(Color.white);
                            break;
                        }
    
                        g2d.fill(r);
                        g2d.setColor(Color.black);
                        g2d.draw(r);
                    }
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
                    Window.this.sim.reset();
                    Window.this.frameCount = 0;
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
    private double cellSize;

    // settings
    protected boolean debug;
    protected boolean pause;

    // simulation loop stuff
    protected boolean running;
    protected int frameCount;  // number of frames that have passed
    protected int tickCount;
    protected int targetFrameDelta; // target time between each frame
    protected int targetTickDelta;
    protected ArrayList<Integer> sCounts, iCounts, rCounts;

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
        Dimension dim = grid.getSize();
        cellSize = Math.min((double) dim.width / (sim.gridSize * sim.communitySize),
                (double) dim.height / sim.gridSize);
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

        while (running)
        {
            // timing data for setting the frame rate
            long frameTime = System.currentTimeMillis();
            long deltaTime = frameTime - prevFrameTime;
            prevFrameTime = frameTime;

            // update grid by running simulationstep
            // ON the simulation step - it is likely the method that should be done in parallel, this is because the range
            // can be given for each thread to iterate on.
            if (!pause)
            {
                sim.simulationStep(-1); // -1 is used so default on switch is used
            }
            updateListCounts(frameCount); // Update SIR
            // System.out.println("\nS: " + sCounts.get(frameCount));
            // System.out.println("I: " + iCounts.get(frameCount));
            // System.out.println("R: " + rCounts.get(frameCount));

            // print grid to screen
            render();

            this.frameCount += 1;

            // apply framerate cap
            long delay = frameTime + this.targetFrameDelta - System.currentTimeMillis();
            try { if (delay > 0) Thread.sleep(delay); } catch (InterruptedException e) { this.running = false; break; }
        }
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
    }
}
