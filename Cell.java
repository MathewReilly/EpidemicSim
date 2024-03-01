public class Cell
{
    private CellState state;

    public Cell()
    {
        state = CellState.SUSCEPTIBLE;
    }

    public CellState getState()
    {
        return this.state;
    }

    public void setState( CellState state )
    {
        this.state = state;
    }
}
