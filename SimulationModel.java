import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.awt.Point;

public class SimulationModel {

    // --- State ---
    private final List<Car> cars = new ArrayList<>();
    private final List<Light> lights = new ArrayList<>();
    private final List<SpawnPoint> spawnPoints = new ArrayList<>();
    private final HashMap<Integer, Long> lastSpawnTime = new HashMap<>();
    private final Random random = new Random();

    private int currentGreenIndex = 0;
    private Phase phase = Phase.AllRed;
    private long lastSwitchTime = 0;

    // --- Stats ---
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

    /**
     * The main update tick for the entire simulation.
     */
    public void update() {
        updateTrafficLights();
        updateCars();
    }

    public void trySpawnCar(int spawnIndex) {
        long now = System.nanoTime();
        if ((now - lastSpawnTime.getOrDefault(spawnIndex, 0L)) / 1e9 < Config.MIN_SPAWN_DELAY_SEC) return;

        SpawnPoint sp = spawnPoints.get(spawnIndex);
        Car newCar = new Car(sp.x, sp.y, sp.dirX, sp.dirY, getRandomTurn());

        if (!isCarTooClose(newCar, cars)) {
            cars.add(newCar);
            lastSpawnTime.put(spawnIndex, now);
            totalCarsSpawned++;
        }
    }

    public void trySpawnRandomCar() {
        trySpawnCar(random.nextInt(spawnPoints.size()));
    }

    private void updateTrafficLights() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - lastSwitchTime) / 1e9;

        switch (phase) {
            case Green:
                int greenLightId = Config.LIGHT_ORDER[currentGreenIndex];
                if (elapsedSeconds >= calculateGreenDuration(greenLightId)) {
                    phase = Phase.AllRed;
                    lastSwitchTime = now;
                    lights.forEach(light -> light.isGreen = false);
                }
                break;
            case AllRed:
                if (elapsedSeconds >= Config.ALL_RED_DURATION_SEC) {
                    currentGreenIndex = (currentGreenIndex + 1) % Config.LIGHT_ORDER.length;
                    int nextGreenLightId = Config.LIGHT_ORDER[currentGreenIndex];
                    phase = Phase.Green;
                    lastSwitchTime = now;
                    double nextGreenDuration = calculateGreenDuration(nextGreenLightId);
                    for (int i = 0; i < lights.size(); i++) {
                        lights.get(i).isGreen = (i == nextGreenLightId && nextGreenDuration > 0);
                    }
                }
                break;
        }
    }

    private double calculateGreenDuration(int greenLightId) {
        Light greenLight = lights.get(greenLightId);
        long waitingCars = cars.stream()
                .filter(car -> car.dirX == greenLight.dirX && car.dirY == greenLight.dirY)
                .filter(car -> {
                    double cx = Config.WINDOW_WIDTH / 2.0;
                    double cy = Config.WINDOW_HEIGHT / 2.0;
                    if (car.dirY > 0) return (cy - car.y) < 200.0 && (cy - car.y) > 0;
                    if (car.dirY < 0) return (car.y - cy) < 200.0 && (car.y - cy) > 0;
                    if (car.dirX > 0) return (cx - car.x) < 200.0 && (cx - car.x) > 0;
                    if (car.dirX < 0) return (car.x - cx) < 200.0 && (car.x - cx) > 0;
                    return false;
                })
                .count();
        if (waitingCars == 0) return 0.0;
        return Math.min(Config.MAX_GREEN_DURATION_SEC, Config.MIN_GREEN_DURATION_SEC + waitingCars * 0.5);
    }

    private void updateCars() {
        cars.removeIf(car -> car.x < -Config.INTERSECTION_SIZE || car.x > Config.WINDOW_WIDTH + Config.INTERSECTION_SIZE ||
                car.y < -Config.INTERSECTION_SIZE || car.y > Config.WINDOW_HEIGHT + Config.INTERSECTION_SIZE);
        
        List<Car> carsSnapshot = new ArrayList<>(cars);
        for (Car car : cars) {
            if (shouldStopAtLight(car) || isCarTooClose(car, carsSnapshot)) continue;
            car.x += car.dirX;
            car.y += car.dirY;
            tryTurn(car);
        }
    }

    private boolean shouldStopAtLight(Car car) {
        double cx = Config.WINDOW_WIDTH / 2.0;
        double cy = Config.WINDOW_HEIGHT / 2.0;
        Light relevantLight = lights.stream()
                .filter(l -> l.dirX == car.dirX && l.dirY == car.dirY)
                .findFirst().orElse(null);
        if (relevantLight == null || relevantLight.isGreen) return false;

        if (car.dirY > 0) return Math.abs(car.y + 2 * Config.INTERSECTION_SIZE - cy) < Config.CAR_SPEED;
        if (car.dirY < 0) return Math.abs(car.y - Config.INTERSECTION_SIZE - cy) < Config.CAR_SPEED;
        if (car.dirX > 0) return Math.abs(car.x + 2 * Config.INTERSECTION_SIZE - cx) < Config.CAR_SPEED;
        if (car.dirX < 0) return Math.abs(car.x - Config.INTERSECTION_SIZE - cx) < Config.CAR_SPEED;
        return false;
    }

    private boolean isCarTooClose(Car car, List<Car> otherCars) {
        for (Car other : otherCars) {
            if (car == other || car.dirX != other.dirX || car.dirY != other.dirY) continue;
            double dx = other.x - car.x;
            double dy = other.y - car.y;
            if (car.dirY > 0 && dy > 0 && Math.abs(dx) < Config.INTERSECTION_SIZE && dy < Config.MIN_GAP) return true;
            if (car.dirY < 0 && dy < 0 && Math.abs(dx) < Config.INTERSECTION_SIZE && -dy < Config.MIN_GAP) return true;
            if (car.dirX > 0 && dx > 0 && Math.abs(dy) < Config.INTERSECTION_SIZE && dx < Config.MIN_GAP) return true;
            if (car.dirX < 0 && dx < 0 && Math.abs(dy) < Config.INTERSECTION_SIZE && -dx < Config.MIN_GAP) return true;
        }
        return false;
    }

    private void tryTurn(Car car) {
        if (car.turn == Turns.FORWARD || car.turned) return;

        double cx = Config.WINDOW_WIDTH / 2.0;
        double cy = Config.WINDOW_HEIGHT / 2.0;
        boolean canTurn = false;

        if (car.dirX > 0) {
            if (car.turn == Turns.RIGHT && Math.abs(car.x - (cx - Config.INTERSECTION_SIZE)) < Config.CAR_SPEED) {
                canTurn = true; car.x = cx - Config.INTERSECTION_SIZE;
            } else if (car.turn == Turns.LEFT && Math.abs(car.x - cx) < Config.CAR_SPEED) {
                canTurn = true; car.x = cx; car.y = cy - Config.INTERSECTION_SIZE;
            }
        } else if (car.dirX < 0) {
            if (car.turn == Turns.RIGHT && Math.abs(car.x - cx) < Config.CAR_SPEED) {
                canTurn = true; car.x = cx;
            } else if (car.turn == Turns.LEFT && Math.abs(car.x - (cx - Config.INTERSECTION_SIZE)) < Config.CAR_SPEED) {
                canTurn = true; car.x = cx - Config.INTERSECTION_SIZE; car.y = cy;
            }
        } else if (car.dirY > 0) {
            if (car.turn == Turns.RIGHT && Math.abs(car.y - (cy - Config.INTERSECTION_SIZE)) < Config.CAR_SPEED) {
                canTurn = true; car.y = cy - Config.INTERSECTION_SIZE;
            } else if (car.turn == Turns.LEFT && Math.abs(car.y - cy) < Config.CAR_SPEED) {
                canTurn = true; car.y = cy; car.x = cx;
            }
        } else if (car.dirY < 0) {
            if (car.turn == Turns.RIGHT && Math.abs(car.y - cy) < Config.CAR_SPEED) {
                canTurn = true; car.y = cy;
            } else if (car.turn == Turns.LEFT && Math.abs(car.y - (cy - Config.INTERSECTION_SIZE)) < Config.CAR_SPEED) {
                canTurn = true; car.y = cy - Config.INTERSECTION_SIZE; car.x = cx - Config.INTERSECTION_SIZE;
            }
        }

        if (canTurn) {
            double oldDirX = car.dirX, oldDirY = car.dirY;
            if (car.turn == Turns.LEFT) { car.dirX = oldDirY; car.dirY = -oldDirX; }
            else if (car.turn == Turns.RIGHT) { car.dirX = -oldDirY; car.dirY = oldDirX; }
            car.turned = true;
        }
    }

    private Turns getRandomTurn() {
        return Turns.values()[random.nextInt(Turns.values().length)];
    }

    // --- Getters for the View ---
    public List<Car> getCars() { return new ArrayList<>(cars); }
    public List<Light> getLights() { return new ArrayList<>(lights); }
    public long getTotalCarsSpawned() { return totalCarsSpawned; }
    public long getSimulationStartTimeNanos() { return simulationStartTime; }
}