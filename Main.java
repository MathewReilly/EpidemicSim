public class Main
{
    public static void main( String[] args )
    {
        Simulation s = new Simulation(81, 2);
        Window w = new Window(s);
        w.run();
    }
}
