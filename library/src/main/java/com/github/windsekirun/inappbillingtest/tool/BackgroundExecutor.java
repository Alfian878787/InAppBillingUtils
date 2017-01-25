package com.github.windsekirun.inappbillingtest.tool;

import java.util.concurrent.Executor;

/**
 * BackgroundExecutor
 * Created by WindSekirun on 2017-01-05.
 */
public interface BackgroundExecutor extends Executor {

    BackgroundExecutor setTaskType(String taskType);

    BackgroundExecutor setThreadPoolSize(int poolSize);
}