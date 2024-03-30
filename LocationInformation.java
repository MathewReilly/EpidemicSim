public class LocationInformation {
  private int row;
  private int col;
  private int com;
  //private CellState type;

  public LocationInformation(int com, int row, int col)
  {
    this.com = com;
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

  public int getCom()
  {
    return com;
  }

  /*public CellState getType()
  {
    return type;
  }*/
  
}
