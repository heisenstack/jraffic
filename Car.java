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
}