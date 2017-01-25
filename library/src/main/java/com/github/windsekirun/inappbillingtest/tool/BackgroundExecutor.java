package com.github.windsekirun.inappbillingtest.tool;

import java.util.concurrent.Executor;

public interface BackgroundExecutor extends Executor {

    BackgroundExecutor setTaskType(String taskType);

    BackgroundExecutor setThreadPoolSize(int poolSize);
}