public class Simulation
{
    private int size;
    private int startingPopulation;
    private float infectionChance;

    private Cell[][] grid;


    // setup simulation
    public Simulation( int size )
    {
        this.size = size;
        this.grid = new Cell[ size ][ size ];
    }

    public void reset()
    {
        this.grid = new Cell[ this.size ][ this.size ];
    }

    // main loop for the simulation
    public void run() 
    {
        // the initial number of infected will start with 1 but may want to be changed later.
        populateGrid(1);

        // ON the simulation step - it is likely the method that should be done in parallel, this is because the range
        // can be given for each thread to iterate on.

        printGrid();
    }

    // The cells states by default is susceptible making our default grid that of susceptible cells, however there needs to be
    // a border and generated infected cells
    private void populateGrid(int numInitialInfected)
    {
        for(int rows = 0; rows < size; rows++)
        {
            for(int cols = 0; cols < size; cols++)
            {
                grid[rows][cols] = new Cell();
            }
        }


        // start with border so infected cell will not be generated on that location
        // to divide up the grid, the most simple solution will be to divide it into 3rds, rounded down (by default), 
        // marking cells as border.
        int borderLocation = size / 3 - 1;
        
        // creates vertical borders along the grid (marks each row at that column location)
        for(int rows = 0; rows < size; rows++)
        {
            grid[rows][borderLocation].setState(CellState.BORDER);
            grid[rows][borderLocation - 1].setState(CellState.BORDER);

            grid[rows][2 * borderLocation].setState(CellState.BORDER);
            grid[rows][(2 * borderLocation) + 1].setState(CellState.BORDER);
        }

        // creates horizonral borders along the grid (marks each column at that row's location)
        for(int cols = 0; cols < size; cols++)
        {
            grid[borderLocation][cols].setState(CellState.BORDER);
            grid[borderLocation - 1][cols].setState(CellState.BORDER);

            grid[2 * borderLocation][cols].setState(CellState.BORDER);
            grid[(2 * borderLocation) + 1][cols].setState(CellState.BORDER);
        }
    }
    // for algorithm person
    public void simulationStep() {}

    // if we want to draw to the screen later
    public void render() {}

    private void printGrid()
    {
        for(int rows = 0; rows < size; rows++)
        {
            for(int cols = 0; cols < size; cols++)
            {
                if(grid[rows][cols].getState() == CellState.SUSCEPTIBLE)
                {
                    System.out.printf("S ", grid[rows][cols]);
                }
                if(grid[rows][cols].getState() == CellState.BORDER)
                {
                    System.out.printf("B ", grid[rows][cols]);
                }
            }
            System.out.println();
        }
    }
}
