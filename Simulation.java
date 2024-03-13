// This Simulation file supports the actual implementation of our simulation. It pertains to creating the grid
// and containing the methods used to run the simulatio.

import java.util.Vector;



public class Simulation
{
    public int gridSize;
    public int borderedGridSize;
    private int startingPopulation;
    private float infectionChance;

    // sim things - neighborModifiers, make it easy for the thread to locate neighbors in a clearer way
    final int fL = -2; // far left neighbor
    final int nL = -1; // near left neighbor
    final int fU = -2; // far upper neighbor
    final int nU = -1; // near upper neighbor
    final int fR = 2; // far right neighbor
    final int nR = 1; // near right neighbor
    final int fD = 2; // far down neighbor
    final int nD = 1; // near down neighbor

    public Cell[][] grid;
    public int sCount, iCount, rCount;

    // setup simulation
    public Simulation( int size )
    {
        // force size to be a multiple of 3
        size = size - size % 3;

        this.gridSize = size;
        this.borderedGridSize = size + 8;
        this.grid = new Cell[ borderedGridSize ][ borderedGridSize ];
    }

    // this method is largely untested and may run into errors as testing happens
    public void updateGrid(int row, int col, CellState type)
    {
        Cell c = getFromGrid(row, col);
        c.setState(type);

        if(type == CellState.INFECTIOUS)
        {
            c.setCounter(7);
        }
    }

    public Cell getFromGrid(int row, int col)
    {
        // wrap coordinates
        row = row % gridSize;
        col = col % gridSize;

        int split = gridSize / 3;
        int x = (2 * ((col / split) + 1) + col);
        int y = (2 * ((row / split) + 1) + row);

        return grid[y][x];
    }

    public void reset()
    {
        this.grid = new Cell[ this.borderedGridSize ][ this.borderedGridSize ];
        populateGrid(1);
    }

    public void resetNewGridSize(int size)
    {
        // force size to be a multiple of 3
        size = size - size % 3;

        this.gridSize = size;
        this.borderedGridSize = size + 8;
        this.grid = new Cell[ borderedGridSize ][ borderedGridSize ];
        populateGrid(1);
    }

    // The cells states by default is susceptible making our default grid that of susceptible cells, however there needs to be
    // a border and generated infected cells
    public void populateGrid(int numInitialInfected)
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
            row = (int)(Math.random() * gridSize);
            col = (int)(Math.random() * gridSize);

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
        int rowStart = 0, rowEnd = 0, colStart = 0, colEnd = 0;
        switch (threadNum) {
            case 0:
                rowStart = 0;
                rowEnd = gridSize / 3;
                colStart = 0;
                colEnd = gridSize / 3;
                break;
            case 1:
                rowStart = 0;
                rowEnd = gridSize / 3;
                colStart = gridSize / 3;
                colEnd = 2 * gridSize / 3;
                break;
            case 2:
                rowStart = 0;
                rowEnd = gridSize / 3;
                colStart = 2 * gridSize / 3;
                colEnd = gridSize;
                break;
            case 3:
                rowStart = gridSize / 3;
                rowEnd = 2 * gridSize / 3;
                colStart = 0;
                colEnd = gridSize / 3;
                break;
            case 4:
                rowStart = gridSize / 3;
                rowEnd = 2 * gridSize / 3;
                colStart = gridSize / 3;
                colEnd = 2 * gridSize / 3;
                break;
            case 5:
                rowStart = gridSize / 3;
                rowEnd = 2 * gridSize / 3;
                colStart = 2 * gridSize / 3;
                colEnd = gridSize;
                break;
            case 6:
                rowStart = 2 * gridSize / 3;
                rowEnd = gridSize;
                colStart = 0;
                colEnd = gridSize / 3;
                break;
            case 7:
                rowStart = 2 * gridSize / 3;
                rowEnd = gridSize;
                colStart = gridSize / 3;
                colEnd = 2 * gridSize / 3;
                break;
            case 8:
                rowStart = 2 * gridSize / 3;
                rowEnd = gridSize;
                colStart = 2 * gridSize / 3;
                colEnd = gridSize;
                break;
            default:
                // Handle default case
                break;
        }
    
        // Loop through the specified section of the grid
        for (int rows = rowStart; rows < rowEnd; rows++) {
            for (int cols = colStart; cols < colEnd; cols++) {                
                
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

        GetSIR();
    }

    private void GetSIR()
    {
        sCount = 0;
        iCount = 0;
        rCount = 0;
        for(int rows = 0; rows < gridSize; rows++)
        {
            for(int cols = 0; cols < gridSize; cols++)
            {
                Cell curCell = getFromGrid(rows, cols);
                if (curCell.getState() == CellState.SUSCEPTIBLE)
                {
                    sCount++;
                }
                else if (curCell.getState() == CellState.INFECTIOUS)
                {
                    iCount++;
                }
                else
                {
                    rCount++;
                }
            }
        }
    }

}
