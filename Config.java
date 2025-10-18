public class Config {
    private Config() {}

    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 600;
    public static final double ROAD_WIDTH = 100.0;
    public static final double INTERSECTION_SIZE = ROAD_WIDTH / 2.0;

    public static final int TIMER_DELAY_MS = 16; 
    public static final double MIN_SPAWN_DELAY_SEC = 0.3;

    public static final double MIN_GAP = INTERSECTION_SIZE * 1.75;
    public static final double CAR_SPEED = 2.5;
    public static final double CAR_SCALE_FACTOR = 0.8;
}
