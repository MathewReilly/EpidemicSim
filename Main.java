public class Main
{
    public static void main( String[] args )
    {
        Simulation s = new Simulation(81, 4);
        Window w = new Window(s);
        w.run();
    }
}
