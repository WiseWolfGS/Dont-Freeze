package wwgs.dontfreeze.core.temperature.fuel;

import wwgs.dontfreeze.api.temperature.fuel.IFuelData;

public class FuelData implements IFuelData
{
    private int stored;
    private final int capacity;

    public FuelData(int stored, int capacity)
    {
        this.capacity = Math.max(0, capacity);
        this.stored = Math.max(0, Math.min(stored, capacity));
    }

    @Override
    public int getStored()
    {
        return stored;
    }

    @Override
    public int getCapacity()
    {
        return capacity;
    }

    @Override
    public int addFuel(int amount)
    {
        if (amount <= 0)
        {
            return 0;
        }

        int before = stored;
        stored = Math.min(capacity, stored + amount);
        return stored - before;
    }

    @Override
    public int consumeFuel(int amount)
    {
        if (amount <= 0)
        {
            return 0;
        }

        int before = stored;
        stored = Math.max(0, stored - amount);
        return before - stored;
    }

    @Override
    public void setStored(int amount)
    {
        stored = Math.max(0, Math.min(amount, capacity));
    }
}