public class Main
{
    public static void main( String[] args )
    {
        Simulation s = new Simulation(9);
        Window w = new Window(s);
        w.run();
    }
}
