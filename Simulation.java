// This Simulation file supports the actual implementation of our simulation. It pertains to creating the grid
// and containing the methods used to run the simulatio.

import java.util.Vector;



public class Simulation
{
    // Variables defining grid information
    public int gridSize;
    public int borderedGridSize;
    public int startingPopulation;
    public float infectionChance;
    public int communitySize;

    // keeps track of multithreading average (in nanoseconds)
    private double averageSimulationStepTime = 0;
    private long totalSimulationStepTime = 0;
    private long numSimulationSteps = 0;

    // sim things - neighborModifiers, make it easy for the thread to locate neighbors in a clearer way
    final int fL = -2; // far left neighbor
    final int nL = -1; // near left neighbor
    final int fU = -2; // far upper neighbor
    final int nU = -1; // near upper neighbor
    final int fR = 2; // far right neighbor
    final int nR = 1; // near right neighbor
    final int fD = 2; // far down neighbor
    final int nD = 1; // near down neighbor

    // the grid and SIR variables
    public Cell[][][] grid;
    public int sCount, iCount, rCount;
    public int[] gravityPopulation;         // models gavity off the population of each community

    // setup simulation
    public Simulation( int size, int communitySize )
    {
        // force size to be a multiple of 3
        size = size - size % 3;
        this.gridSize = size;
        this.borderedGridSize = size + 8;
        this.communitySize = communitySize;
        this.gravityPopulation = new int[borderedGridSize*borderedGridSize];
        this.grid = new Cell[communitySize][ borderedGridSize ][ borderedGridSize ];
        startingPopulation = size*size;
        infectionChance = 20f;
    }

    // this method is largely untested and may run into errors as testing happens
    // Will update the current grid by changing a cell's type.
    public void updateGrid(int communitySize, int row, int col, CellState type)
    {
        Cell c = getFromGrid(communitySize, row, col);
        c.setState(type);

        if(type == CellState.INFECTIOUS)
        {
            // how large should this be set to?
            c.setCounter(14);
        }
    }

    // method for keeping cell counters while also moving their location
    public void moveSingleCell(int communitySize, int row, int col, CellState type, int counterVal)
    {
        Cell c = getFromGrid(communitySize, row, col);
        c.setState(type);
        c.setCounter(counterVal);

        /*if(type == CellState.INFECTIOUS)
        {
            // how large should this be set to?
            c.setCounter(14);
        }*/
    }

    // Gets a cell from a location on the grid
    // returns the actual cell as a value
    public Cell getFromGrid(int communitySize, int row, int col)
    {
        // wrap coordinates
        row = row % gridSize;
        col = col % gridSize;

        int split = gridSize / 3;
        int x = (2 * ((col / split) + 1) + col);
        int y = (2 * ((row / split) + 1) + row);

        return grid[communitySize][y][x];
    }

    // re-allocate the grid to a new cell array, start with an infected
    public void reset()
    {
        this.grid = new Cell[this.communitySize][ this.borderedGridSize ][ this.borderedGridSize ];
        populateGrid(1);
    }

    // If you alredy have a size, reset grid using that size directly
    public void reset(int size)
    {
        // force size to be a multiple of 3
        size = size - size % 3;

        this.gridSize = size;
        this.borderedGridSize = size + 8;
        this.startingPopulation = size*size;
        this.grid = new Cell[communitySize][ borderedGridSize ][ borderedGridSize ];
        populateGrid(1);
    }

    // The cells states by default is susceptible making our default grid that of susceptible cells, however there needs to be
    // a border and generated infected cells
    public void populateGrid(int numInitialInfected)
    {
        // generates the number of "Land" cells we will have in our simulation
        double numLand = Math.floor((borderedGridSize*borderedGridSize)/0.5);

        // Generate a cell for every location in every community
        for (int com = 0; com < communitySize; com++) {
            for(int rows = 0; rows < borderedGridSize; rows++)
            {
                for(int cols = 0; cols < borderedGridSize; cols++)
                {
                    grid[com][rows][cols] = new Cell();
                }
            }
        }


        // start with border so infected cell will not be generated on that location
        // to divide up the grid, the most simple solution will be to divide it into 3rds, rounded down (by default), 
        // marking cells as border.
        int borderLocation = borderedGridSize / 3;
        
        // creates vertical borders along the grid (marks each row at that column location)
        for (int com = 0; com < communitySize; com++) {
            for(int rows = 0; rows < borderedGridSize; rows++)
            {
                grid[com][rows][0].setState(CellState.BORDER);
                grid[com][rows][1].setState(CellState.BORDER);
    
                grid[com][rows][borderLocation].setState(CellState.BORDER);
                grid[com][rows][borderLocation + 1].setState(CellState.BORDER);
    
                grid[com][rows][2 * borderLocation].setState(CellState.BORDER);
                grid[com][rows][(2 * borderLocation) + 1].setState(CellState.BORDER);
    
                grid[com][rows][borderedGridSize - 2].setState(CellState.BORDER);
                grid[com][rows][borderedGridSize - 1].setState(CellState.BORDER);
            }
    
            // creates horizonral borders along the grid (marks each column at that row's location)
            for(int cols = 0; cols < borderedGridSize; cols++)
            {
                grid[com][0][cols].setState(CellState.BORDER);
                grid[com][1][cols].setState(CellState.BORDER);
    
                grid[com][borderLocation][cols].setState(CellState.BORDER);
                grid[com][borderLocation + 1][cols].setState(CellState.BORDER);
    
                grid[com][2 * borderLocation][cols].setState(CellState.BORDER);
                grid[com][(2 * borderLocation) + 1][cols].setState(CellState.BORDER);
    
                grid[com][borderedGridSize - 2][cols].setState(CellState.BORDER);
                grid[com][borderedGridSize - 1][cols].setState(CellState.BORDER);
            }
        }

        // this next section focuses on infecting random cells at the start
        // pick initial cell to infect
        int row = (int)(Math.random() * gridSize);
        int col = (int)(Math.random() * gridSize);
        int com = (int)(Math.random() * communitySize);
        Cell curCell = getFromGrid(com, row, col);
        // row = (int)(Math.random() * gridSize);       // is this repetitive to above?
        // col = (int)(Math.random() * gridSize);       // removing this section didn't appear to make a difference
        // com = (int)(Math.random() * communitySize);
        // curCell = getFromGrid(com, row, col);

        // while we have yet to reach our number of inactive "land" cells (serving as natural separations of cells),
        // mark cells on the map.
        int counter = 0;
        while (counter != numLand) {
            row = (int)(Math.random() * gridSize);
            col = (int)(Math.random() * gridSize);
            com = (int)(Math.random() * communitySize);
            
            updateGrid(com, row, col, CellState.LAND);
            counter++;
        }

        // for the cell we want to infect, make sure it is not a land or border cell initially
        while(curCell.getState() == CellState.LAND || curCell.getState() == CellState.BORDER)
        {
            row = (int)(Math.random() * gridSize);
            col = (int)(Math.random() * gridSize);
            com = (int)(Math.random() * communitySize);
            curCell = getFromGrid(com, row, col);
            
        }

        // set a susceptible cell to be infectious.
        if (curCell.getState() == CellState.SUSCEPTIBLE) {
            updateGrid(com, row, col, CellState.INFECTIOUS);
        }
    }

    // The simulation step will take the current grid and apply changes to it.
    public void simulationStep(int threadNum) 
    {
        // current cell and changing cell variables
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
                rowStart = 0;
                rowEnd = gridSize;
                colStart = 0;
                colEnd = gridSize;
                break;
        }
    
        // collect all of the neighboring susceptible cells
        for (int com = 0; com < communitySize; com++) {
            for (int rows = rowStart; rows < rowEnd; rows++) 
            {
                for (int cols = colStart; cols < colEnd; cols++) 
                {                
                
                    curCell = getFromGrid(com, rows, cols);
                    // If a cell is infected, find all susceptible neighbors. Once neighbors are found, decreate infection timer.
                    if(curCell.getState() == CellState.INFECTIOUS)
                    {
                        // upper-left neighbor
                        neighborCell = getFromGrid(com, rows + nL, cols + nU);
                        if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(com, rows + nL, cols + nU));}
    
                        // upper neighbor
                        neighborCell = getFromGrid(com, rows, cols + nU);
                        if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(com, rows, cols + nU));}
    
                        // upper-right neighbor
                        neighborCell = getFromGrid(com, rows + nR, cols + nU);
                        if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(com, rows + nR, cols + nU));}
    
                        // left neighbor
                        neighborCell = getFromGrid(com, rows + nL, cols);
                        if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(com, rows + nL, cols));}
    
                        // right neighbor
                        neighborCell = getFromGrid(com, rows + nR, cols);
                        if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(com, rows + nR, cols));}
    
                        // down-left neighbor
                        neighborCell = getFromGrid(com, rows + nL, cols + nD);
                        if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(com, rows + nL, cols + nD));}
    
                        // down neighbor
                        neighborCell = getFromGrid(com, rows, cols + nD);
                        if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(com, rows, cols + nD));}
    
                        // down-right nieghbor
                        neighborCell = getFromGrid(com, rows + nR, cols + nD);
                        if(neighborCell.getState() == CellState.SUSCEPTIBLE) {susCells.add(new LocationInformation(com, rows + nR, cols + nD));}
    
                        // get the amount of time left of infectiousness, if it is no longer infectious remove it.
                        if(curCell.getAndDecrementCounter() == 1)
                        {
                            curCell.setState(CellState.REMOVED);
                        }
                    }
                }
            }
        }
        
        // for all of the susceptible neighbors set for infection based on chance.
        for(int i = 0; i < susCells.size(); i++)
        {
            if(Math.random() * 100 <= infectionChance)
            {
                gridChanges.add(susCells.elementAt(i));
            }
        }
        
        // Update grid with all new infections.
        for(int i = 0; i < gridChanges.size(); i++)
        {
            for(int j = 0; j < borderedGridSize*borderedGridSize; j++) {
                gravityPopulation[j] = 0;
            }
            updateGrid(gridChanges.elementAt(i).getCom(), gridChanges.elementAt(i).getRow(), gridChanges.elementAt(i).getCol(), CellState.INFECTIOUS);
            gravityPopulation[gridChanges.elementAt(i).getCom()]++;
        }

        
        // maybe move to a unique SIR total and have each thread calculate individually?
        // is this a race condition to access or are copies made for eacth thread?
        // or maybe calculate under window's tick method, as a final separate step <- currently trying this
        //GetSIR();
    }

    public void cellMovement()
    {
        // Once infections are calculated, cells will move around the land
        int max = 9;
        int min = 1;
        int range = max - min + 1;

        // For every community, each section will run the moving cells algorithm
        for (int com = 0; com < communitySize; com++) {
            for (int rows = 0; rows < gridSize; rows++) {
                for (int cols = 0; cols < gridSize; cols++) {
                    
                    // gather a random chance to move to a neighboring position
                    int move = (int)(Math.random() * range) + min;
                    Cell temp;
                    Cell current = getFromGrid(com, rows, cols);

                    // When a new position is available, set that as the new cells position, and set the currrent location to land
                    // If a cell remains in a spot, pull neighboring cells closer to form a community
                    switch(move) {
                        case 1: 
                        temp = getFromGrid(com, rows, cols + nU);
                        if (temp.getState() == CellState.LAND && (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            moveSingleCell(com, rows, cols + nU, current.getState(), current.getCounter());
                            current.setState(CellState.LAND);
                            current.setCounter(0);
                            // System.out.println(move);
                        }
                        break;
    
                        case 2:
                        temp = getFromGrid(com, rows, cols + nD);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            moveSingleCell(com, rows, cols + nD, current.getState(), current.getCounter());
                            current.setState(CellState.LAND);
                            current.setCounter(0);
                        }
                        break;
    
                        case 3:
                        temp = getFromGrid(com, rows + nR, cols);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            moveSingleCell(com, rows + nR, cols, current.getState(), current.getCounter());
                            current.setState(CellState.LAND);
                            current.setCounter(0);
                        }
                        break;
    
                        case 4:
                        temp = getFromGrid(com, rows + nL, cols);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            moveSingleCell(com, rows + nL, cols, current.getState(), current.getCounter());
                            current.setState(CellState.LAND);
                            current.setCounter(0);
                        }
                        break;

                        case 5:
                        temp = getFromGrid(com, rows + nR, cols + nU);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            moveSingleCell(com, rows + nR, cols + nU, current.getState(), current.getCounter());
                            current.setState(CellState.LAND);
                            current.setCounter(0);
                        }
                        break;

                        case 6:
                        temp = getFromGrid(com, rows + nL, cols + nU);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            moveSingleCell(com, rows + nL, cols + nU, current.getState(), current.getCounter());
                            current.setState(CellState.LAND);
                            current.setCounter(0);
                        }
                        break;
                        case 7:
                        temp = getFromGrid(com, rows + nL, cols + nD);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            moveSingleCell(com, rows + nL, cols + nD, current.getState(), current.getCounter());
                            current.setState(CellState.LAND);
                            current.setCounter(0);
                        }
                        break;
                        case 8:
                        temp = getFromGrid(com, rows + nR, cols + nD);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            moveSingleCell(com, rows + nR, cols + nD, current.getState(), current.getCounter());
                            current.setState(CellState.LAND);
                            current.setCounter(0);
                        }
                        break;
                        case 9:
                        // System.out.println(com);
                        if (gravityPopulation[com] != 0) {
                            int currentCommunityPopulation = gravityPopulation[com];
                            for (int i = 0; i < communitySize; i++) {
                                if (i != com) {
                                    int nextCommunityPopulation = gravityPopulation[i];
                                    int populationMultiple = currentCommunityPopulation * nextCommunityPopulation;
                                    int distanceBetween = Math.abs(currentCommunityPopulation - nextCommunityPopulation);
                                    double strengthOfInteraction;
                                    // int totalDistance;
                                    // int size; 
                                    // switch(distanceBetween) {
                                    //     case 1:
                                    //     case 3: 
                                    //     case 4:
                                    //     size = gridSize;
                                    //     totalDistance = (int)Math.pow(size, 2);
                                    //     strengthOfInteraction = populationMultiple/totalDistance;
                                    //     break;
                                    //     case 5:
                                    //     case 6:
                                    //     size = gridSize;
                                    //     totalDistance = (int)Math.pow(size, 2);
                                    //     strengthOfInteraction = populationMultiple/totalDistance;
                                    //     break;
                                    // }
                                    strengthOfInteraction = populationMultiple/((double)Math.pow((borderedGridSize*distanceBetween),2)+1);
                                    max = 100;
                                    min = 0;
                                    range = max - min + 1;
                                    strengthOfInteraction *= 100;
                                    move = (int)(Math.random() * range) + min;
                                    if (move <= strengthOfInteraction) {
                                        temp = getFromGrid(i, rows, cols);
                                        if (temp.getState() == CellState.LAND && (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                                            moveSingleCell(i, rows, cols, current.getState(), current.getCounter());
                                            if (current.getState() == CellState.INFECTIOUS) {
                                                // System.out.println("Infectious");
                                            }
                                            current.setState(CellState.LAND);
                                            current.setCounter(0);
                                        }
                                    }
                                }
                            }
                        }
                        break;
    
                        default:
                        break;
                        
                    }
                }
            }
        }
    }

    // Get SIR values from this simulation step by returning an array holding
    // S, I, and R respectively
    public int[] GetSIR()
    {
        int[] sir = {0, 0, 0};
        for (int com = 0; com < communitySize; com++) {
            for(int rows = 0; rows < gridSize; rows++)
            {
                for(int cols = 0; cols < gridSize; cols++)
                {
                    Cell curCell = getFromGrid(com, rows, cols);
                    if (curCell.getState() == CellState.SUSCEPTIBLE)
                    {
                        sir[0]++;
                    }
                    else if (curCell.getState() == CellState.INFECTIOUS)
                    {
                        sir[1]++;
                    } 
                    else if (curCell.getState() == CellState.REMOVED) if (curCell.getState() == CellState.REMOVED)
                    {
                        sir[2]++;
                    }
                }
            }
        }

        return sir;
    }
    
    public int GetStartingPopulation()
    {
        return this.startingPopulation;
    }

    public float GetInfectionChance()
    {
        return this.infectionChance;
    }


    public void addSimulationStepTime(long thisTicksSimulationTime)
    {
        totalSimulationStepTime += thisTicksSimulationTime;
        numSimulationSteps++;
        averageSimulationStepTime = totalSimulationStepTime / numSimulationSteps;
    }

    public double getAverageSimulationStepTime()
    {
        return averageSimulationStepTime;
    }
}


