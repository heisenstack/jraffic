import java.awt.Color;

public class Car {
    double x, y, dirX, dirY;
    Turns turn;
    boolean turned = false;
    Color color;

    public Car(double x, double y, double dX, double dY, Turns t) {
        this.x = x;
        this.y = y;
        this.dirX = dX;
        this.dirY = dY;
        this.turn = t;
        this.color = getRandomColor();
    }

    // Assigns color based on turn for easy debugging/viewing
    private Color getRandomColor() {
        switch (turn) {
            case FORWARD: return new Color(30, 144, 255); // Blue
            case LEFT:    return Color.ORANGE;
            case RIGHT:   return new Color(153, 50, 204); // Purple
            default:      return Color.WHITE;
        }
    }
}