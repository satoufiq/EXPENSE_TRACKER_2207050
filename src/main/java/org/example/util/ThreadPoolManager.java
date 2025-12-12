package org.example.util;

import javafx.application.Platform;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Thread Pool Manager for handling background operations
 * Provides centralized thread management with JavaFX UI integration
 */
public class ThreadPoolManager {

    private static ThreadPoolManager instance;

    // Thread pool for database operations
    private final ExecutorService databaseExecutor;

    // Thread pool for general background tasks
    private final ExecutorService backgroundExecutor;

    // Scheduled executor for periodic tasks
    private final ScheduledExecutorService scheduledExecutor;

    private ThreadPoolManager() {
        // Database operations pool - limited threads to prevent DB lock contention
        this.databaseExecutor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactory() {
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "DB-Worker-" + counter++);
                    t.setDaemon(true);
                    return t;
                }
            }
        );

        // Background tasks pool - more threads for general operations
        this.backgroundExecutor = new ThreadPoolExecutor(
            4, 8, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactory() {
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "Background-Worker-" + counter++);
                    t.setDaemon(true);
                    return t;
                }
            }
        );

        // Scheduled executor for periodic tasks
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Scheduled-Worker-" + counter++);
                t.setDaemon(true);
                return t;
            }
        });
    }

    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }

    /**
     * Execute a database operation asynchronously
     * @param task The database task to execute
     * @return Future for tracking completion
     */
    public <T> CompletableFuture<T> executeDatabase(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        databaseExecutor.submit(() -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Execute a database operation and update UI on completion
     * @param task The database task
     * @param onSuccess Called on JavaFX thread with result
     * @param onError Called on JavaFX thread with exception
     */
    public <T> void executeDatabaseWithCallback(
            Callable<T> task,
            Consumer<T> onSuccess,
            Consumer<Exception> onError) {

        databaseExecutor.submit(() -> {
            try {
                T result = task.call();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e));
            }
        });
    }

    /**
     * Execute a background task asynchronously
     * @param task The task to execute
     * @return Future for tracking completion
     */
    public <T> CompletableFuture<T> executeBackground(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        backgroundExecutor.submit(() -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Execute a background task and update UI on completion
     * @param task The background task
     * @param onSuccess Called on JavaFX thread with result
     * @param onError Called on JavaFX thread with exception
     */
    public <T> void executeBackgroundWithCallback(
            Callable<T> task,
            Consumer<T> onSuccess,
            Consumer<Exception> onError) {

        backgroundExecutor.submit(() -> {
            try {
                T result = task.call();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e));
            }
        });
    }

    /**
     * Schedule a task to run periodically
     * @param task The task to run
     * @param initialDelay Initial delay before first execution
     * @param period Period between executions
     * @param unit Time unit
     * @return ScheduledFuture for canceling the task
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable task,
            long initialDelay,
            long period,
            TimeUnit unit) {

        return scheduledExecutor.scheduleWithFixedDelay(task, initialDelay, period, unit);
    }

    /**
     * Run a task on the JavaFX Application Thread
     * @param task The task to run
     */
    public static void runOnUIThread(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    /**
     * Shutdown all thread pools gracefully
     */
    public void shutdown() {
        System.out.println("Shutting down thread pools...");

        databaseExecutor.shutdown();
        backgroundExecutor.shutdown();
        scheduledExecutor.shutdown();

        try {
            if (!databaseExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                databaseExecutor.shutdownNow();
            }
            if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            databaseExecutor.shutdownNow();
            backgroundExecutor.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Thread pools shut down successfully");
    }
}

