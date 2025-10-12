import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class Jraffic extends JPanel implements ActionListener {

    // --- Configuration Constants ---
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final double ROAD_WIDTH = 100.0;
    private static final double INTERSECTION_SIZE = ROAD_WIDTH / 2.0;
    private static final double MIN_SPAWN_DELAY = 0.3;
    private static final double MIN_GAP = INTERSECTION_SIZE * 1.75;
    private static final double CAR_SPEED = 2.5;
    private static final double CAR_SCALE_FACTOR = 0.8; 

    // --- Traffic Light Timings ---
    private static final double MIN_GREEN_DURATION = 3.0;
    private static final double MAX_GREEN_DURATION = 8.0;
    private static final double ALL_RED_DURATION = 0.5;
    private static final int[] LIGHT_ORDER = {2, 0, 3, 1};

    // --- Global State ---
    private static int currentGreenIndex = 0;
    private static Phase phase = Phase.AllRed;
    private static long lastSwitchTime = 0;

    private final List<Car> cars = new ArrayList<>();
    private final List<Light> lights = new ArrayList<>();
    private final List<SpawnPoint> spawnPoints = new ArrayList<>();
    private final HashMap<Integer, Long> lastSpawnTime = new HashMap<>();
    private final HashSet<Integer> activeKeys = new HashSet<>();

    private final Random random = new Random();
    private final Timer timer;

    // --- Stats Tracking ---
    private long totalCarsSpawned = 0;
    private long simulationStartTime = 0;

    public Jraffic() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);

        initializeSimulation();
        simulationStartTime = System.nanoTime();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                activeKeys.add(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                activeKeys.remove(e.getKeyCode());
            }
        });

        timer = new Timer(16, this);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        handleInput();
        updateTrafficLights();
        updateCars();
        repaint();
    }

    private void initializeSimulation() {
        Point center = new Point(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2);

        spawnPoints.add(new SpawnPoint(center.x - INTERSECTION_SIZE, 0, 0, CAR_SPEED));
        spawnPoints.add(new SpawnPoint(center.x, WINDOW_HEIGHT - INTERSECTION_SIZE, 0, -CAR_SPEED));
        spawnPoints.add(new SpawnPoint(0, center.y, CAR_SPEED, 0));
        spawnPoints.add(new SpawnPoint(WINDOW_WIDTH - INTERSECTION_SIZE, center.y - INTERSECTION_SIZE, -CAR_SPEED, 0));

        lights.add(new Light(center.x - 2 * INTERSECTION_SIZE, center.y - 2 * INTERSECTION_SIZE, 0, CAR_SPEED));
        lights.add(new Light(center.x + INTERSECTION_SIZE, center.y - 2 * INTERSECTION_SIZE, -CAR_SPEED, 0));
        lights.add(new Light(center.x - 2 * INTERSECTION_SIZE, center.y + INTERSECTION_SIZE, CAR_SPEED, 0));
        lights.add(new Light(center.x + INTERSECTION_SIZE, center.y + INTERSECTION_SIZE, 0, -CAR_SPEED));

        lastSwitchTime = System.nanoTime();
    }

    private void handleInput() {
        if (activeKeys.contains(KeyEvent.VK_ESCAPE)) System.exit(0);
        if (activeKeys.contains(KeyEvent.VK_DOWN)) trySpawnCar(0);
        if (activeKeys.contains(KeyEvent.VK_UP)) trySpawnCar(1);
        if (activeKeys.contains(KeyEvent.VK_RIGHT)) trySpawnCar(2);
        if (activeKeys.contains(KeyEvent.VK_LEFT)) trySpawnCar(3);
        if (activeKeys.contains(KeyEvent.VK_R)) trySpawnCar(random.nextInt(spawnPoints.size()));
    }

    private void trySpawnCar(int spawnIndex) {
        long now = System.nanoTime();
        if ((now - lastSpawnTime.getOrDefault(spawnIndex, 0L)) / 1e9 < MIN_SPAWN_DELAY) return;

        SpawnPoint sp = spawnPoints.get(spawnIndex);
        Car newCar = new Car(sp.x, sp.y, sp.dirX, sp.dirY, getRandomTurn());

        if (!isCarTooClose(newCar, cars)) {
            cars.add(newCar);
            lastSpawnTime.put(spawnIndex, now);
            totalCarsSpawned++;
        }
    }

    private void updateTrafficLights() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - lastSwitchTime) / 1e9;

        switch (phase) {
            case Green:
                int greenLightId = LIGHT_ORDER[currentGreenIndex];
                if (elapsedSeconds >= calculateGreenDuration(greenLightId)) {
                    phase = Phase.AllRed;
                    lastSwitchTime = now;
                    lights.forEach(light -> light.isGreen = false);
                }
                break;
            case AllRed:
                if (elapsedSeconds >= ALL_RED_DURATION) {
                    currentGreenIndex = (currentGreenIndex + 1) % LIGHT_ORDER.length;
                    int nextGreenLightId = LIGHT_ORDER[currentGreenIndex];
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
                double cx = WINDOW_WIDTH / 2.0;
                double cy = WINDOW_HEIGHT / 2.0;
                if (car.dirY > 0) return (cy - car.y) < 200.0 && (cy - car.y) > 0;
                if (car.dirY < 0) return (car.y - cy) < 200.0 && (car.y - cy) > 0;
                if (car.dirX > 0) return (cx - car.x) < 200.0 && (cx - car.x) > 0;
                if (car.dirX < 0) return (car.x - cx) < 200.0 && (car.x - cx) > 0;
                return false;
            })
            .count();
        if (waitingCars == 0) return 0.0;
        return Math.min(MAX_GREEN_DURATION, MIN_GREEN_DURATION + waitingCars * 0.5);
    }

    private void updateCars() {
        cars.removeIf(car -> car.x < -INTERSECTION_SIZE || car.x > WINDOW_WIDTH + INTERSECTION_SIZE ||
                               car.y < -INTERSECTION_SIZE || car.y > WINDOW_HEIGHT + INTERSECTION_SIZE);
        List<Car> carsSnapshot = new ArrayList<>(cars);
        for (Car car : cars) {
            if (shouldStopAtLight(car) || isCarTooClose(car, carsSnapshot)) continue;
            car.x += car.dirX;
            car.y += car.dirY;
            tryTurn(car);
        }
    }

    private boolean shouldStopAtLight(Car car) {
        double cx = WINDOW_WIDTH / 2.0;
        double cy = WINDOW_HEIGHT / 2.0;
        Light relevantLight = lights.stream()
            .filter(l -> l.dirX == car.dirX && l.dirY == car.dirY)
            .findFirst().orElse(null);
        if (relevantLight == null || relevantLight.isGreen) return false;

        if (car.dirY > 0) return Math.abs(car.y + 2 * INTERSECTION_SIZE - cy) < CAR_SPEED;
        if (car.dirY < 0) return Math.abs(car.y - INTERSECTION_SIZE - cy) < CAR_SPEED;
        if (car.dirX > 0) return Math.abs(car.x + 2 * INTERSECTION_SIZE - cx) < CAR_SPEED;
        if (car.dirX < 0) return Math.abs(car.x - INTERSECTION_SIZE - cx) < CAR_SPEED;
        return false;
    }

    private boolean isCarTooClose(Car car, List<Car> otherCars) {
        for (Car other : otherCars) {
            if (car == other || car.dirX != other.dirX || car.dirY != other.dirY) continue;
            double dx = other.x - car.x;
            double dy = other.y - car.y;
            if (car.dirY > 0 && dy > 0 && Math.abs(dx) < INTERSECTION_SIZE && dy < MIN_GAP) return true;
            if (car.dirY < 0 && dy < 0 && Math.abs(dx) < INTERSECTION_SIZE && -dy < MIN_GAP) return true;
            if (car.dirX > 0 && dx > 0 && Math.abs(dy) < INTERSECTION_SIZE && dx < MIN_GAP) return true;
            if (car.dirX < 0 && dx < 0 && Math.abs(dy) < INTERSECTION_SIZE && -dx < MIN_GAP) return true;
        }
        return false;
    }

    private void tryTurn(Car car) {
        if (car.turn == Turns.FORWARD || car.turned) return;

        double cx = WINDOW_WIDTH / 2.0;
        double cy = WINDOW_HEIGHT / 2.0;
        boolean canTurn = false;

        if (car.dirX > 0) {
            if (car.turn == Turns.RIGHT && Math.abs(car.x - (cx - INTERSECTION_SIZE)) < CAR_SPEED) {
                canTurn = true; car.x = cx - INTERSECTION_SIZE;
            } else if (car.turn == Turns.LEFT && Math.abs(car.x - cx) < CAR_SPEED) {
                canTurn = true; car.x = cx; car.y = cy - INTERSECTION_SIZE;
            }
        } else if (car.dirX < 0) {
            if (car.turn == Turns.RIGHT && Math.abs(car.x - cx) < CAR_SPEED) {
                canTurn = true; car.x = cx;
            } else if (car.turn == Turns.LEFT && Math.abs(car.x - (cx - INTERSECTION_SIZE)) < CAR_SPEED) {
                canTurn = true; car.x = cx - INTERSECTION_SIZE; car.y = cy;
            }
        } else if (car.dirY > 0) {
            if (car.turn == Turns.RIGHT && Math.abs(car.y - (cy - INTERSECTION_SIZE)) < CAR_SPEED) {
                canTurn = true; car.y = cy - INTERSECTION_SIZE;
            } else if (car.turn == Turns.LEFT && Math.abs(car.y - cy) < CAR_SPEED) {
                canTurn = true; car.y = cy; car.x = cx;
            }
        } else if (car.dirY < 0) {
            if (car.turn == Turns.RIGHT && Math.abs(car.y - cy) < CAR_SPEED) {
                canTurn = true; car.y = cy;
            } else if (car.turn == Turns.LEFT && Math.abs(car.y - (cy - INTERSECTION_SIZE)) < CAR_SPEED) {
                canTurn = true; car.y = cy - INTERSECTION_SIZE; car.x = cx - INTERSECTION_SIZE;
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2d);
        drawRoads(g2d);
        drawLights(g2d);
        drawCars(g2d);
        drawStats(g2d);
        drawGuide(g2d); 
    }

    private void drawBackground(Graphics2D g2d) {
        Point2D center = new Point2D.Float(WINDOW_WIDTH / 2f, WINDOW_HEIGHT / 2f);
        float radius = WINDOW_WIDTH / 1.5f;
        float[] dist = {0.0f, 1.0f};
        Color[] colors = {new Color(50, 150, 50), new Color(25, 77, 25)};
        RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);
        g2d.setPaint(p);
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    private void drawRoads(Graphics2D g2d) {
        int centerX = WINDOW_WIDTH / 2, centerY = WINDOW_HEIGHT / 2;
        int roadWidthInt = (int) ROAD_WIDTH;

        g2d.setColor(Color.GRAY);
        g2d.fillRect(0, centerY - roadWidthInt / 2, WINDOW_WIDTH, roadWidthInt);
        g2d.fillRect(centerX - roadWidthInt / 2, 0, roadWidthInt, WINDOW_HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(0, centerY - roadWidthInt / 2, centerX - roadWidthInt / 2, centerY - roadWidthInt / 2);
        g2d.drawLine(centerX + roadWidthInt / 2, centerY - roadWidthInt / 2, WINDOW_WIDTH, centerY - roadWidthInt / 2);
        g2d.drawLine(0, centerY + roadWidthInt / 2, centerX - roadWidthInt / 2, centerY + roadWidthInt / 2);
        g2d.drawLine(centerX + roadWidthInt / 2, centerY + roadWidthInt / 2, WINDOW_WIDTH, centerY + roadWidthInt / 2);
        g2d.drawLine(centerX - roadWidthInt / 2, 0, centerX - roadWidthInt / 2, centerY - roadWidthInt / 2);
        g2d.drawLine(centerX - roadWidthInt / 2, centerY + roadWidthInt / 2, centerX - roadWidthInt / 2, WINDOW_HEIGHT);
        g2d.drawLine(centerX + roadWidthInt / 2, 0, centerX + roadWidthInt / 2, centerY - roadWidthInt / 2);
        g2d.drawLine(centerX + roadWidthInt / 2, centerY + roadWidthInt / 2, centerX + roadWidthInt / 2, WINDOW_HEIGHT);

        drawLaneMarkings(g2d);
    }

    private void drawLaneMarkings(Graphics2D g2d) {
        int centerX = WINDOW_WIDTH / 2, centerY = WINDOW_HEIGHT / 2;
        int intersectionStartX = (int) (centerX - ROAD_WIDTH / 2), intersectionEndX = (int) (centerX + ROAD_WIDTH / 2);
        int intersectionStartY = (int) (centerY - ROAD_WIDTH / 2), intersectionEndY = (int) (centerY + ROAD_WIDTH / 2);

        g2d.setColor(Color.YELLOW);
        Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{20, 15}, 0);
        g2d.setStroke(dashed);

        g2d.drawLine(0, centerY, intersectionStartX, centerY);
        g2d.drawLine(intersectionEndX, centerY, WINDOW_WIDTH, centerY);
        g2d.drawLine(centerX, 0, centerX, intersectionStartY);
        g2d.drawLine(centerX, intersectionEndY, centerX, WINDOW_HEIGHT);
    }

    private void drawLights(Graphics2D g2d) {
        for (Light light : lights) {
            g2d.setColor(Color.BLACK);
            g2d.fill(new RoundRectangle2D.Double(light.x, light.y, INTERSECTION_SIZE, INTERSECTION_SIZE, 10, 10));

            Color redOff = new Color(40, 0, 0), greenOff = new Color(0, 40, 0);
            Color redOn = Color.RED, greenOn = new Color(50, 205, 50);

            Color redColor = light.isGreen ? redOff : redOn;
            Color greenColor = light.isGreen ? greenOn : greenOff;

            if (light.dirX != 0) {
                g2d.setColor(redColor); g2d.fillOval((int) light.x + 8, (int) light.y + 17, 15, 15);
                g2d.setColor(greenColor); g2d.fillOval((int) light.x + 27, (int) light.y + 17, 15, 15);
            } else {
                g2d.setColor(redColor); g2d.fillOval((int) light.x + 17, (int) light.y + 8, 15, 15);
                g2d.setColor(greenColor); g2d.fillOval((int) light.x + 17, (int) light.y + 27, 15, 15);
            }
        }
    }

    private void drawCars(Graphics2D g2d) {
        double carSize = INTERSECTION_SIZE * CAR_SCALE_FACTOR;
        double offset = (INTERSECTION_SIZE - carSize) / 2.0;

        for (Car car : cars) {
            Color carColor;
            switch (car.turn) {
                case FORWARD: carColor = new Color(30, 144, 255); break;
                case LEFT:    carColor = Color.ORANGE; break;
                case RIGHT:   carColor = new Color(153, 50, 204); break;
                default:      carColor = Color.WHITE;
            }

            double drawX = car.x + offset;
            double drawY = car.y + offset;

            g2d.setColor(carColor);
            g2d.fill(new RoundRectangle2D.Double(drawX, drawY, carSize, carSize, 15, 15));
            g2d.setColor(new Color(255, 255, 255, 70));
            g2d.fill(new RoundRectangle2D.Double(drawX + 5, drawY + 5, carSize - 10, carSize - 25, 10, 10));
            g2d.setColor(carColor.darker());
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(new RoundRectangle2D.Double(drawX, drawY, carSize, carSize, 15, 15));
        }
    }

    private void drawStats(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fill(new RoundRectangle2D.Double(10, 10, 200, 85, 15, 15));

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));

        long elapsedNanos = System.nanoTime() - simulationStartTime;
        long elapsedSeconds = elapsedNanos / 1_000_000_000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        String uptime = String.format("%02d:%02d", minutes, seconds);

        g2d.drawString("Uptime: " + uptime, 20, 35);
        g2d.drawString("Active Cars: " + cars.size(), 20, 55);
        g2d.drawString("Total Cars Spawned: " + totalCarsSpawned, 20, 75);
    }

    private void drawGuide(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fill(new RoundRectangle2D.Double(WINDOW_WIDTH - 210, WINDOW_HEIGHT - 110, 200, 100, 15, 15));

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2d.drawString("Controls:", WINDOW_WIDTH - 200, WINDOW_HEIGHT - 85);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.drawString("Arrow Keys: Spawn Car", WINDOW_WIDTH - 200, WINDOW_HEIGHT - 65);
        g2d.drawString("R Key: Spawn Random Car", WINDOW_WIDTH - 200, WINDOW_HEIGHT - 45);
        g2d.drawString("ESC Key: Exit Simulation", WINDOW_WIDTH - 200, WINDOW_HEIGHT - 25);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Jraffic - Traffic Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new Jraffic(), BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    enum Turns { RIGHT, LEFT, FORWARD }
    enum Phase { Green, AllRed }
    static class Light {
        double x, y, dirX, dirY; boolean isGreen = false;
        Light(double x, double y, double dX, double dY) { this.x=x; this.y=y; this.dirX=dX; this.dirY=dY; }
    }
    static class Car {
        double x, y, dirX, dirY; Turns turn; boolean turned = false;
        Car(double x, double y, double dX, double dY, Turns t) { this.x=x; this.y=y; this.dirX=dX; this.dirY=dY; this.turn=t; }
    }
    static class SpawnPoint {
        double x, y, dirX, dirY;
        SpawnPoint(double x, double y, double dX, double dY) { this.x=x; this.y=y; this.dirX=dX; this.dirY=dY; }
    }
}