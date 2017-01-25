package com.github.windsekirun.inappbillingtest.tool;

/**
 * BackgroundExecutor
 * Created by WindSekirun on 2017-01-05.
 */
public class NaraeAsync {
    private static final int DEFAULT_POOL_SIZE = 3;
    private static final String DEFAULT_TASK_TYPE = "Narae.MOE_123123";;

    private int settingPoolSize = DEFAULT_POOL_SIZE;
    private String settingTaskType = DEFAULT_TASK_TYPE;

    private Runnable runnable;

    public NaraeAsync(Runnable runnable) {
        this.runnable = runnable;
    }

    public NaraeAsync(Runnable runnable, int poolSize) {
        this.runnable = runnable;
        settingPoolSize = poolSize;
    }

    public NaraeAsync(Runnable runnable, String taskType) {
        this.runnable = runnable;
        settingTaskType = taskType;
    }

    public NaraeAsync(Runnable runnable, int poolSize, String taskType) {
        this.runnable = runnable;
        settingPoolSize = poolSize;
        settingTaskType = taskType;
    }

    /**
     * Executing default option
     */
    public void execute() {
        execute(runnable, settingPoolSize, settingTaskType);
    }

    /**
     * Executing custom options
     * @param runnable Runnable object
     * @param poolsize poolSize
     * @param tasktype taskType
     */
    public void execute(Runnable runnable, int poolsize, String tasktype) {
        BackgroundThreadExecutor bte = new BackgroundThreadExecutor();
        bte.setTaskType(tasktype);
        bte.setThreadPoolSize(poolsize);
        bte.execute(runnable);
    }
}