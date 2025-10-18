public class Config {
    // Private constructor to prevent instantiation
    private Config() {}

    // --- Window and Road ---
    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 600;
    public static final double ROAD_WIDTH = 100.0;
    public static final double INTERSECTION_SIZE = ROAD_WIDTH / 2.0;

    // --- Simulation Timing ---
    public static final int TIMER_DELAY_MS = 16; // Approx 60 FPS
    public static final double MIN_SPAWN_DELAY_SEC = 0.3;

    // --- Car Properties ---
    public static final double MIN_GAP = INTERSECTION_SIZE * 1.75;
    public static final double CAR_SPEED = 2.5;
    public static final double CAR_SCALE_FACTOR = 0.8;

    // --- Traffic Light Timings ---
    public static final double MIN_GREEN_DURATION_SEC = 3.0;
    public static final double MAX_GREEN_DURATION_SEC = 8.0;
    public static final double ALL_RED_DURATION_SEC = 0.5;
    public static final int[] LIGHT_ORDER = {2, 0, 3, 1};
}