package wwgs.dontfreeze.core.temperature.heat;

import wwgs.dontfreeze.api.temperature.heat.IHeatData;

public class HeatData implements IHeatData
{
    private int stored;
    private final int capacity;

    public HeatData(int stored, int capacity)
    {
        this.capacity = Math.max(0, capacity);
        this.stored = Math.max(0, Math.min(stored, this.capacity));
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
    public int addHeat(int amount)
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
    public int consumeHeat(int amount)
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