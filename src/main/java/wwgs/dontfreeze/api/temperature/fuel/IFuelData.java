package wwgs.dontfreeze.api.temperature.fuel;

public interface IFuelData
{
    int getStored();
    int getCapacity();

    default boolean isEmpty()
    {
        return getStored() <= 0;
    }

    default boolean isFull()
    {
        return getStored() >= getCapacity();
    }

    default int getRemainingCapacity()
    {
        return Math.max(0, getCapacity() - getStored());
    }

    int addFuel(int amount);
    int consumeFuel(int amount);
    void setStored(int amount);
}