import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Jraffic implements ActionListener {

    private final SimulationModel model;
    private final TrafficPanel panel;
    private final InputHandler inputHandler;
    private final Timer timer;

    public Jraffic() {
        // 1. Create the Model
        model = new SimulationModel();

        // 2. Create the View (and give it the model)
        panel = new TrafficPanel(model);

        // 3. Create the Controller (and give it the model)
        inputHandler = new InputHandler(model);

        // 4. Set up the main window (JFrame)
        JFrame frame = new JFrame("Jraffic Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null); 
        frame.setResizable(false);
        
        panel.addKeyListener(inputHandler);
        
        timer = new Timer(Config.TIMER_DELAY_MS, this);
        timer.start();

        frame.setVisible(true);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        // 1. Check for and handle user input
        inputHandler.handleInput();

        // 2. Update the simulation state
        model.update();

        // 3. Redraw the screen with the new state
        panel.repaint();
    }

    public static void main(String[] args) {
        // Ensures Swing components are created on the Event Dispatch Thread
        SwingUtilities.invokeLater(Jraffic::new);
    }
}