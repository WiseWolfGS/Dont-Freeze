package net.WWGS.dontfreeze.apiimp.initializer;

import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import com.minecolonies.core.colony.jobs.views.DefaultJobView;
import com.mojang.logging.LogUtils;
import net.WWGS.dontfreeze.Dontfreeze;
import net.WWGS.dontfreeze.api.colony.building.job.DFJobs;
import net.WWGS.dontfreeze.api.util.NullnessBridge;
import net.WWGS.dontfreeze.core.colony.job.JobFireKeeper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public final class DFJobsInitializer {
    public final static DeferredRegister<JobEntry> DEFERRED_REGISTER = DeferredRegister.create(NullnessBridge.assumeNonnull(CommonMinecoloniesAPIImpl.JOBS), Dontfreeze.MODID);
    private static final Logger LOGGER = LogUtils.getLogger();

    static {
        DFJobs.firekeeper = register(DEFERRED_REGISTER, DFJobs.FIREKEEPER_ID.getPath(), () -> new JobEntry.Builder()
                .setJobProducer(JobFireKeeper::new)
                .setJobViewProducer(() -> DefaultJobView::new)
                .setRegistryName(DFJobs.FIREKEEPER_ID)
                .createJobEntry());
    }

    private static DeferredHolder<JobEntry, JobEntry> register(final DeferredRegister<JobEntry> deferredRegister, final String path, final @Nonnull Supplier<JobEntry> supplier)
    {
        if (path == null) return null;

        LOGGER.info("Registering job: " + path);
        return deferredRegister.register(path, supplier);
    }
}
