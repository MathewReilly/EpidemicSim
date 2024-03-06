public class LocationInformation {
  private int row;
  private int col;
  //private CellState type;

  public LocationInformation(int row, int col)
  {
      this.row = row;
      this.col = col;
     // this.type = type;
  }

  public int getRow()
  {
    return row;
  }

  public int getCol()
  {
    return col;
  }

  /*public CellState getType()
  {
    return type;
  }*/
  
}
