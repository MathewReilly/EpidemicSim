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

    public void Reset()
    {
        this.grid = new Cell[ this.size ][ this.size ];
    }

    // main loop for the simulation
    public void Run() {}

    // for algorithm person
    public void Update() {}

    // if we want to draw to the screen later
    public void Render() {}
}
