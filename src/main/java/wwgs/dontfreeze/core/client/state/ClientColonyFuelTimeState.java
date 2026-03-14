package wwgs.dontfreeze.core.client.state;

public final class ClientColonyFuelTimeState {
    private static volatile int colonyId = -1;
    private static volatile int minutes = 0;
    private static volatile int seconds = 0;
    private static volatile double tickPerFuel = 0;

    private ClientColonyFuelTimeState() {}

    public static int getColonyId() {
        return colonyId;
    }

    public static int getMinutes() {
        return minutes;
    }

    public static int getSeconds() {
        return seconds;
    }

    public static double getTickPerFuel() { return tickPerFuel; }

    public static void update(int newColonyId, int newMinutes, int newSeconds, double newTickPerFuel) {
        colonyId = newColonyId;
        minutes = Math.max(0, newMinutes);
        seconds = Math.max(0, newSeconds);
        tickPerFuel = Math.max(0, newTickPerFuel);
    }
}