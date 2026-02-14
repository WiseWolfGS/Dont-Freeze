package net.WWGS.dontfreeze.core.client.state;

/**
 * 클라이언트에 캐시된 "플레이어 소속 콜로니"의 발전기(코어) 남은 시간.
 *
 * - colonyId <= 0 이면 미소속/조회 실패
 * - minutes/seconds 는 HUD 출력용
 */
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
