import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;

public class InputHandler extends KeyAdapter {

    private final SimulationModel model;
    private final HashSet<Integer> activeKeys = new HashSet<>();

    public InputHandler(SimulationModel model) {
        this.model = model;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        activeKeys.add(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        activeKeys.remove(e.getKeyCode());
    }

    public void handleInput() {
        if (activeKeys.contains(KeyEvent.VK_ESCAPE)) System.exit(0);
        if (activeKeys.contains(KeyEvent.VK_DOWN)) model.trySpawnCar(0);
        if (activeKeys.contains(KeyEvent.VK_UP)) model.trySpawnCar(1);
        if (activeKeys.contains(KeyEvent.VK_RIGHT)) model.trySpawnCar(2);
        if (activeKeys.contains(KeyEvent.VK_LEFT)) model.trySpawnCar(3);
        if (activeKeys.contains(KeyEvent.VK_R)) model.trySpawnRandomCar();
    }
}