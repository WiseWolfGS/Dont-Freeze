package wwgs.dontfreeze.api.temperature.heat;

public interface IHeatData
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

    int addHeat(int amount);

    int consumeHeat(int amount);

    void setStored(int amount);
}