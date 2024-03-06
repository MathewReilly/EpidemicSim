public class Cell
{
    private CellState state;
    private int counter = 0;

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

    public void setCounter(int counter)
    {
        this.counter = counter;
    }

    public int getAndDecrementCounter()
    {
        int curCounter = counter;
        counter--;
        return curCounter;
    }
}
