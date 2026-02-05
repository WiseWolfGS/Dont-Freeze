package net.WWGS.dontfreeze.api.colony.building.job;

import com.minecolonies.api.colony.jobs.registry.JobEntry;
import net.WWGS.dontfreeze.Dontfreeze;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

public class DFJobs {
    public static final String FIREKEEPER_TAG = "firekeeper";

    public static final ResourceLocation FIREKEEPER_ID = ResourceLocation.fromNamespaceAndPath(Dontfreeze.MODID, FIREKEEPER_TAG);

    public static DeferredHolder<JobEntry, JobEntry> firekeeper;

    private DFJobs() {
        throw new IllegalStateException("Tried to initialize: DFJobs but this is a Utility class.");
    }

    public static List<ResourceLocation> getJobs() {
        List<ResourceLocation> jobs = new ArrayList<>() { };
        jobs.add(FIREKEEPER_ID);

        return jobs;
    }
}
