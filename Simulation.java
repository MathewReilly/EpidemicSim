// This Simulation file supports the actual implementation of our simulation. It pertains to creating the grid
// and containing the methods used to run the simulatio.

import java.util.Vector;



public class Simulation
{
    public int gridSize;
    public int borderedGridSize;
    public int communitySize;
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

    public Cell[][][] grid;
    public int sCount, iCount, rCount;
    public int[] gravityPopulation;

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
    }

    // this method is largely untested and may run into errors as testing happens
    public void updateGrid(int communitySize, int row, int col, CellState type)
    {
        Cell c = getFromGrid(communitySize, row, col);
        c.setState(type);

        if(type == CellState.INFECTIOUS)
        {
            c.setCounter(100);
        }
    }

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

    public void reset()
    {
        this.grid = new Cell[this.communitySize][ this.borderedGridSize ][ this.borderedGridSize ];
        populateGrid(1);
    }

    public void resetNewGridSize(int size)
    {
        // force size to be a multiple of 3
        size = size - size % 3;

        this.gridSize = size;
        this.borderedGridSize = size + 8;
        this.grid = new Cell[communitySize][ borderedGridSize ][ borderedGridSize ];
        populateGrid(1);
    }

    // The cells states by default is susceptible making our default grid that of susceptible cells, however there needs to be
    // a border and generated infected cells
    public void populateGrid(int numInitialInfected)
    {
        double numLand = Math.floor((borderedGridSize*borderedGridSize)/0.5);
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

        // infect random cells at the start
        int row = (int)(Math.random() * gridSize);
        int col = (int)(Math.random() * gridSize);
        int com = (int)(Math.random() * communitySize);
        Cell curCell = getFromGrid(com, row, col);
        row = (int)(Math.random() * gridSize);
        col = (int)(Math.random() * gridSize);
        com = (int)(Math.random() * communitySize);
        curCell = getFromGrid(com, row, col);
        int counter = 0;
        while (counter != numLand) {
            row = (int)(Math.random() * gridSize);
            col = (int)(Math.random() * gridSize);
            com = (int)(Math.random() * communitySize);
            
            updateGrid(com, row, col, CellState.LAND);
            counter++;
        }
        while(curCell.getState() == CellState.LAND || curCell.getState() == CellState.BORDER)
        {
            row = (int)(Math.random() * gridSize);
            col = (int)(Math.random() * gridSize);
            com = (int)(Math.random() * communitySize);
            curCell = getFromGrid(com, row, col);
            
        }
        if (curCell.getState() == CellState.SUSCEPTIBLE) {
            updateGrid(com, row, col, CellState.INFECTIOUS);
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
        for (int com = 0; com < communitySize; com++) {
            for(int rows = 0; rows < gridSize; rows++)
            {
                for(int cols = 0; cols < gridSize; cols++)
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
            if(Math.random() * 100 <= 40)
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

        int max = 9;
        int min = 1;
        int range = max - min + 1;
        for (int com = 0; com < communitySize; com++) {
            for (int rows = 0; rows < gridSize; rows++) {
                for (int cols = 0; cols < gridSize; cols++) {
                    int move = (int)(Math.random() * range) + min;
                    Cell temp;
                    Cell current = getFromGrid(com, rows, cols);
                    switch(move) {
                        case 1: 
                        temp = getFromGrid(com, rows, cols + nU);
                        if (temp.getState() == CellState.LAND && (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            updateGrid(com, rows, cols + nU, current.getState());
                            current.setState(CellState.LAND);
                            // System.out.println(move);
                        }
                        break;
    
                        case 2:
                        temp = getFromGrid(com, rows, cols + nD);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            updateGrid(com, rows, cols + nD, current.getState());
                            current.setState(CellState.LAND);
                        }
                        break;
    
                        case 3:
                        temp = getFromGrid(com, rows + nR, cols);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            updateGrid(com, rows + nR, cols, current.getState());
                            current.setState(CellState.LAND);
                        }
                        break;
    
                        case 4:
                        temp = getFromGrid(com, rows + nL, cols);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            updateGrid(com, rows + nL, cols, current.getState());
                            current.setState(CellState.LAND);
                        }
                        break;

                        case 5:
                        temp = getFromGrid(com, rows + nR, cols + nU);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            updateGrid(com, rows + nR, cols + nU, current.getState());
                            current.setState(CellState.LAND);
                        }
                        break;

                        case 6:
                        temp = getFromGrid(com, rows + nL, cols + nU);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            updateGrid(com, rows + nL, cols + nU, current.getState());
                            current.setState(CellState.LAND);
                        }
                        break;
                        case 7:
                        temp = getFromGrid(com, rows + nL, cols + nD);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            updateGrid(com, rows + nL, cols + nD, current.getState());
                            current.setState(CellState.LAND);
                        }
                        break;
                        case 8:
                        temp = getFromGrid(com, rows + nR, cols + nD);
                        if (temp.getState() == CellState.LAND&& (current.getState() == CellState.INFECTIOUS || current.getState() == CellState.SUSCEPTIBLE)) {
                            updateGrid(com, rows + nR, cols + nD, current.getState());
                            current.setState(CellState.LAND);
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
                                            updateGrid(i, rows, cols, current.getState());
                                            if (current.getState() == CellState.INFECTIOUS) {
                                                // System.out.println("Infectious");
                                            }
                                            current.setState(CellState.LAND);
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
        
        
        GetSIR();
    }

    

    private void GetSIR()
    {
        sCount = 0;
        iCount = 0;
        rCount = 0;
        for (int com = 0; com < communitySize; com++) {
            for(int rows = 0; rows < gridSize; rows++)
            {
                for(int cols = 0; cols < gridSize; cols++)
                {
                    Cell curCell = getFromGrid(com, rows, cols);
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

}
