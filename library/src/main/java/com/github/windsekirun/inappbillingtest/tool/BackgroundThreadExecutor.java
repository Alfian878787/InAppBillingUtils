package com.github.windsekirun.inappbillingtest.tool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * BackgroundThreadExecutor
 * Created by WindSekirun on 2017-01-05.
 */
public class BackgroundThreadExecutor implements BackgroundExecutor {
    private static Map<ExecutorId, Executor> sCachedExecutors = new HashMap<>();
    public String settingTaskType;
    public int settingPoolSize;

    /**
     * Set taskType of Thread
     * @param taskType taskType
     * @return BackgroundExecutor object
     */
    @Override
    public BackgroundExecutor setTaskType(String taskType) {
        settingTaskType = taskType;
        return null;
    }

    /**
     * Set Poolsize of Thread
     * @param poolSize poolSize
     * @return BackgroundExecutor object
     */
    @Override
    public BackgroundExecutor setThreadPoolSize(int poolSize) {
        settingPoolSize = poolSize;
        return null;
    }

    /**
     * Executing
     * @param command runnable object
     */
    @Override
    public void execute(Runnable command) {
        getExecutor().execute(command);
    }

    private Executor getExecutor() {
        final ExecutorId executorId = new ExecutorId(settingPoolSize, settingTaskType);
        synchronized (BackgroundThreadExecutor.class) {
            Executor executor = sCachedExecutors.get(executorId);
            if (executor == null) {
                executor = Executors.newFixedThreadPool(settingPoolSize);
                sCachedExecutors.put(executorId, executor);
            }
            return executor;
        }
    }
}