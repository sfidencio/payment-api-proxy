package config;

import java.util.concurrent.*;

import static config.Constants.MSG_INSTANCE;

public class DynamicThreadPool {

    private DynamicThreadPool() {
        throw new IllegalStateException(MSG_INSTANCE);
    }

    public static ExecutorService createExecutor() {
        ThreadFactory threadFactory = Thread.ofVirtual().factory();
        return new ThreadPoolExecutor(
                0,
                Integer.MAX_VALUE,
                60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public static ScheduledExecutorService createScheduler() {
        ThreadFactory threadFactory = Thread.ofVirtual().factory();
        return Executors.newScheduledThreadPool(10, threadFactory);
    }
}
