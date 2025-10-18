import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class SimulationModel {
    private final List<Car> cars = new ArrayList<>();
    private final List<Light> lights = new ArrayList<>();
    private final List<SpawnPoint> spawnPoints = new ArrayList<>();
    private final HashMap<Integer, Long> lastSpawnTime = new HashMap<>();
    private final Random random = new Random();
    private int currentGreenIndex = 0;
    private Phase phase = Phase.AllRed;
    private long lastSwitchTime = 0;

    private long totalCarsSpawned = 0;
    private long simulationStartTime = 0;

    public SimulationModel() {
        initializeSimulation();
        simulationStartTime = System.nanoTime();
        lastSwitchTime = System.nanoTime();
    }

    private void initializeSimulation() {
        Point center = new Point(Config.WINDOW_WIDTH / 2, Config.WINDOW_HEIGHT / 2);

        // SpawnPoints
        spawnPoints.add(new SpawnPoint(center.x - Config.INTERSECTION_SIZE, 0, 0, Config.CAR_SPEED));
        spawnPoints.add(new SpawnPoint(center.x, Config.WINDOW_HEIGHT - Config.INTERSECTION_SIZE, 0, -Config.CAR_SPEED));
        spawnPoints.add(new SpawnPoint(0, center.y, Config.CAR_SPEED, 0));
        spawnPoints.add(new SpawnPoint(Config.WINDOW_WIDTH - Config.INTERSECTION_SIZE, center.y - Config.INTERSECTION_SIZE, -Config.CAR_SPEED, 0));

        // Lights
        lights.add(new Light(center.x - 2 * Config.INTERSECTION_SIZE, center.y - 2 * Config.INTERSECTION_SIZE, 0, Config.CAR_SPEED));
        lights.add(new Light(center.x + Config.INTERSECTION_SIZE, center.y - 2 * Config.INTERSECTION_SIZE, -Config.CAR_SPEED, 0));
        lights.add(new Light(center.x - 2 * Config.INTERSECTION_SIZE, center.y + Config.INTERSECTION_SIZE, Config.CAR_SPEED, 0));
        lights.add(new Light(center.x + Config.INTERSECTION_SIZE, center.y + Config.INTERSECTION_SIZE, 0, -Config.CAR_SPEED));
    }

    public void update() {
        updateTrafficLights();
        updateCars();
    }
}
