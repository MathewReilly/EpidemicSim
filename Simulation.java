// This Simulation file supports the actual implementation of our simulation. It pertains to creating the grid
// and containing the methods used to run the simulatio.

import java.util.Vector;



public class Simulation
{
    private int size;
    private int borderedGridSize;
    private int startingPopulation;
    private float infectionChance;

    // simulation loop stuff
    private boolean running;
    private int frameCount;  // number of frames that have passed
    private int targetDelta; // target time between each frame

    // sim things - neighborModifiers, make it easy for the thread to locate neighbors in a clearer way
    final int fL = -2; // far left neighbor
    final int nL = -1; // near left neighbor
    final int fU = -2; // far upper neighbor
    final int nU = -1; // near upper neighbor
    final int fR = 2; // far right neighbor
    final int nR = 1; // near right neighbor
    final int fD = 2; // far down neighbor
    final int nD = 1; // near down neighbor

    private Cell[][] grid;


    // setup simulation
    public Simulation( int size )
    {
        this.size = size;
        this.borderedGridSize = size + 8;
        this.grid = new Cell[ borderedGridSize ][ borderedGridSize ];

        this.running = false;

        // This will change how long each frame takes. Currently
        // set to 1000 milliseconds (1 second) per frame. Should
        // be lower later on.
        this.targetDelta = 1000;
    }

    // this method is largely untested and may run into errors as testing happens
    public void updateGrid(int row, int col, CellState type)
    {
        int borderLoc = size / 3;

        // will correctly index, ignoring the padding
        if(row < borderLoc)
        {
            row = row + 2;
        } else if (row < borderLoc * 2)
        {
            row = row + 4;
        } else
        {
            row = row + 6;
        }

        if(col < borderLoc)
        {
            col = col + 2;
        } else if (col < borderLoc * 2)
        {
            col = col + 4;
        } else
        {
            col = col + 6;
        }

        // System.out.printf("\n %d, %d \n", row, col);

        grid[row][col].setState(type);

        if(type == CellState.INFECTIOUS)
        {
            grid[row][col].setCounter(7);
        }

    }

    public Cell getFromGrid(int row, int col)
    {
        int borderLoc = size / 3;

        // will correctly index, ignoring the padding
        if(row < borderLoc + 1)
        {
            row = row + 2;
        } else if (row < borderLoc * 2 + 1)
        {
            row = row + 4;
        } else
        {
            row = row + 6;
        }

        if(col < borderLoc + 1)
        {
            col = col + 2;
        } else if (col < borderLoc * 2 + 1)
        {
            col = col + 4;
        } else
        {
            col = col + 6;
        }

        return grid[row][col];
    }

    public void reset()
    {
        this.grid = new Cell[ this.borderedGridSize ][ this.borderedGridSize ];
    }

    // main loop for the simulation
    public void run() 
    {
        // the initial number of infected will start with 1 but may want to be changed later.
        populateGrid(1);
        
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
            simulationStep(-1); // -1 is used so default on switch is used

            // print grid to screen
            printGrid();
            System.out.println();
            System.out.println();

            this.frameCount += 1;

            // apply framerate cap
            long delay = frameTime + this.targetDelta - System.currentTimeMillis();
            try { if (delay > 0 ) Thread.sleep(delay); } catch (InterruptedException e) { this.running = false; break; }
        }
    }

    // The cells states by default is susceptible making our default grid that of susceptible cells, however there needs to be
    // a border and generated infected cells
    private void populateGrid(int numInitialInfected)
    {
        for(int rows = 0; rows < borderedGridSize; rows++)
        {
            for(int cols = 0; cols < borderedGridSize; cols++)
            {
                grid[rows][cols] = new Cell();
            }
        }


        // start with border so infected cell will not be generated on that location
        // to divide up the grid, the most simple solution will be to divide it into 3rds, rounded down (by default), 
        // marking cells as border.
        int borderLocation = borderedGridSize / 3;
        
        // creates vertical borders along the grid (marks each row at that column location)
        for(int rows = 0; rows < borderedGridSize; rows++)
        {
            grid[rows][0].setState(CellState.BORDER);
            grid[rows][1].setState(CellState.BORDER);

            grid[rows][borderLocation].setState(CellState.BORDER);
            grid[rows][borderLocation + 1].setState(CellState.BORDER);

            grid[rows][2 * borderLocation].setState(CellState.BORDER);
            grid[rows][(2 * borderLocation) + 1].setState(CellState.BORDER);

            grid[rows][borderedGridSize - 2].setState(CellState.BORDER);
            grid[rows][borderedGridSize - 1].setState(CellState.BORDER);
        }

        // creates horizonral borders along the grid (marks each column at that row's location)
        for(int cols = 0; cols < borderedGridSize; cols++)
        {
            grid[0][cols].setState(CellState.BORDER);
            grid[1][cols].setState(CellState.BORDER);

            grid[borderLocation][cols].setState(CellState.BORDER);
            grid[borderLocation + 1][cols].setState(CellState.BORDER);

            grid[2 * borderLocation][cols].setState(CellState.BORDER);
            grid[(2 * borderLocation) + 1][cols].setState(CellState.BORDER);

            grid[borderedGridSize - 2][cols].setState(CellState.BORDER);
            grid[borderedGridSize - 1][cols].setState(CellState.BORDER);
        }



        // infect random cells at the start
        int row = 0;
        int col = 0;
        for(int i = 0; i < numInitialInfected; i++)
        {
            row = (int)(Math.random() * size);
            col = (int)(Math.random() * size);

            updateGrid(row, col, CellState.INFECTIOUS);
        }

        
    }

    // The simulation step will take the current grid and apply changes to it.
    public void simulationStep(int threadNum) 
    {
        

        Vector<LocationInformation> gridChanges = new Vector<>(borderedGridSize);
        Vector<LocationInformation> susCells = new Vector<>(borderedGridSize);
        Cell curCell;
        Cell neighborCell;

        // Depending on the thread number, work on that section of the grid.
        // This section is resposible for collecting all of the changes made during this simulation step.
        // I would recommend that each of these cases set some type of grid bounds variables that can then replace grid boundery
        // information in the code from default (remocing code from default?)
        switch (threadNum) {
            case 0:
                
                break;
            case 1:
                
                break;
            case 2:
                
                break;
            case 3:
                
                break;
            case 4:
                
                break;
            case 5:
                
                break;
            case 6:
                
                break;
            case 7:
                
                break;
            case 8:
                
                break;
        
            default: // for now default will hold all of the implementation but the switch statement is in preparation for parallel.
                
                

                break;
        }

        // collect all of the neighboring susceptible cells
        for(int rows = 0; rows < size; rows++)
        {
            for(int cols = 0; cols < size; cols++)
            {
                curCell = getFromGrid(rows, cols);
                // If a cell is infected, find all susceptible neighbors. Once neighbors are found, decreate infection timer.
                if(curCell.getState() == CellState.INFECTIOUS)
                {
                    // upper-left neighbor
                    neighborCell = getFromGrid(rows + nL, cols + nU);
                    if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(rows + nL, cols + nU));}

                    // upper neighbor
                    neighborCell = getFromGrid(rows, cols + nU);
                    if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(rows, cols + nU));}

                    // upper-right neighbor
                    neighborCell = getFromGrid(rows + nR, cols + nU);
                    if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(rows + nR, cols + nU));}

                    // left neighbor
                    neighborCell = getFromGrid(rows + nL, cols);
                    if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(rows + nL, cols));}

                    // right neighbor
                    neighborCell = getFromGrid(rows + nR, cols);
                    if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(rows + nR, cols));}

                    // down-left neighbor
                    neighborCell = getFromGrid(rows + nL, cols + nD);
                    if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(rows + nL, cols + nD));}

                    // down neighbor
                    neighborCell = getFromGrid(rows, cols + nD);
                    if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(rows, cols + nD));}

                    // down-right nieghbor
                    neighborCell = getFromGrid(rows + nR, cols + nD);
                    if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(rows + nR, cols + nD));}

                    // get the amount of time left of infectiousness, if it is no longer infectious remove it.
                    if(curCell.getAndDecrementCounter() == 1)
                    {
                        curCell.setState(CellState.REMOVED);
                    }
                }
            }
        }

        // for all of the susceptible neighbors set for infection based on chance.
        for(int i = 0; i < susCells.size(); i++)
        {
            if(Math.random() * 100 <= 20)
            {
                gridChanges.add(susCells.elementAt(i));
            }
        }

        // Update grid with all new infections.
        for(int i = 0; i < gridChanges.size(); i++)
        {
            updateGrid(gridChanges.elementAt(i).getRow(), gridChanges.elementAt(i).getCol(), CellState.INFECTIOUS);
        }

    }

    // if we want to draw to the screen later
    public void render() {}

    private void printGrid()
    {
        for(int rows = 0; rows < borderedGridSize; rows++)
        {
            for(int cols = 0; cols < borderedGridSize; cols++)
            {
                if(grid[rows][cols].getState() == CellState.SUSCEPTIBLE)
                {
                    System.out.printf("S ", grid[rows][cols]);
                }
                if(grid[rows][cols].getState() == CellState.BORDER)
                {
                    System.out.printf("B ", grid[rows][cols]);
                }
                if(grid[rows][cols].getState() == CellState.INFECTIOUS)
                {
                    System.out.printf("I ", grid[rows][cols]);
                }
                if(grid[rows][cols].getState() == CellState.REMOVED)
                {
                    System.out.printf("R ", grid[rows][cols]);
                }
            }
            System.out.println();
        }
    }
}
