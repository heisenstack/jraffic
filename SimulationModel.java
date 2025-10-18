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
}
