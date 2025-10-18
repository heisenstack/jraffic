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
}
