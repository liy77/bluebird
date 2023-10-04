package io.github.justawaifuhunter.bluebird.async;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents the eventual completion (or failure) of an asynchronous operation and its resulting value.
 * @param <T> The value to be returned in promise
 */
public class Promise<T> {
    private volatile Exception rejected = null; // Declaration of a volatile field for storing a rejected Exception
    private volatile T received; // Declaration of a volatile field for storing received value
    /**
     * A volatile boolean field indicating if the promise is rejected
     */
    public volatile boolean isRejected = false;
    /**
     * A volatile boolean field indicating if the promise is resolved
     */
    public volatile boolean isResolved = false;
    /**
     * A volatile boolean field indicating if an exception is caught
     */
    public volatile boolean isCaught = false;
    private final Object lock = new Object(); // Creating a synchronization lock object
    private final CountDownLatch latch = new CountDownLatch(1); // Creating a CountDownLatch with an initial count of 1
    private volatile Callable<T> caught; // Declaration of a volatile field for a Callable that catches exceptions
    private volatile Runnable onFinal = null; // Declaration of a volatile field for a Runnable to be executed on finalization
    private volatile  boolean onFinalCalled = false; // Declaration of a volatile boolean field to track if onFinal has been called
    private final List<Consumer<T>> thenablesCallbacks = new ArrayList<>(); // Creating a list to store Consumer callbacks

    public Promise() {} // Default Constructor

    public Promise(@Nonnull Function<Consumer<Exception>, T> callback) {
        this.resolve(callback.apply(this::reject)); // Resolving the promise with a callback function
    }

    public Promise(@Nonnull BiConsumer<Consumer<T>, Consumer<Exception>> callback) {
        callback.accept(this::resolve, this::reject); // Accepting a callback for resolving or rejecting the promise
    }

    /**
     * Resolves the promise value
     * @param value The value to be resolved
     */
    public void resolve(T value) {
        this.receive(value);
    }

    private void notifyAndCountDown() {
        lock.notifyAll(); // Notifies waiting threads
        latch.countDown();
    }

    private void receive(T t) {
        try {
            Thread thread = new Thread(() -> {
                try {
                    synchronized (lock) {
                        received = t;
                        isResolved = true;
                        notifyAndCountDown();
                    }
                } catch (Exception e) {
                    synchronized (lock) {
                        _reject(e); // Error received, rejecting current promise
                        notifyAndCountDown();
                    }
                }
            });

            thread.start(); // Starts a new thread to handle promise resolution
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reject current promise
     * @param e Exception to reject promise
     */
    public void reject(@Nonnull Exception e) {
        synchronized (lock) {
            if (!isResolved && !isRejected) {
                _reject(e);
                notifyAndCountDown();
            }
        }
    }

    /**
     * Reject current promise
     * @param e Exception message to reject promise
     */
    public void reject(@Nonnull String e) {
        reject(new Exception(e));
    }

    private void _reject(Exception e) {
        rejected = e;
        isRejected = true; // Indicates that current promise was rejected
    }

    /**
     * If the current promise is rejected, the value passed in catch will be returned.
     * @param r Function that will be executed in catch with the value to be returned
     */
    @NotNull
    public Promise<T> catchException(@Nonnull Callable<T> r) {
        isCaught = true;
        caught = r;

        Iterator<Consumer<T>> _thenablesCallbacks = thenablesCallbacks.iterator();

        // Check if there are thenables callbacks and if so, execute them
        if (_thenablesCallbacks.hasNext()) {
            try {
                Consumer<T> current = _thenablesCallbacks.next(); // Get the current thenable callback value from the list
                thenablesCallbacks.remove(current); // Removing the unnecessary callback from the list
                current.accept(r.call()); // Executes the Callable and passes the result to the Consumer
            } catch (Exception e) {
                // Ignore this
            }

            if (onFinal != null && !onFinalCalled) {
                onFinal.run();
            }
        }

        return this;
    }

    /**
     * Executes a function when the current promise ends
     * @param callback Function that will be executed at the end of the promise
     */
    @NotNull
    public Promise<T> then(@Nonnull Consumer<T> callback) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Consumer<T> callback2 = (value) -> {
            callback.accept(value);
            executorService.shutdown(); // Shutdown the service to avoid improper code running
        };

        thenablesCallbacks.add(callback2);

        executorService.submit(() -> {
            Runnable shutdown = () -> {
                if (onFinal != null && !onFinalCalled) {
                    onFinal.run();
                    onFinalCalled = true;
                }

                thenablesCallbacks.remove(callback2); // Removing the unnecessary callback from the list
                executorService.shutdown(); // Shutdown the service to avoid improper code running
            };

            Runnable closeAndAccept = () -> {
                callback.accept(received);
                shutdown.run();
            };

            Runnable closeAndCatch = () -> {
                try {
                    callback.accept(caught.call()); // Promise rejected, running caught function
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                shutdown.run();
            };

            synchronized (lock) {
                if (isResolved) {
                    closeAndAccept.run();
                } else if (isRejected) {
                    if (!isCaught) {
                        throw new RuntimeException(rejected.getMessage());
                    }

                    closeAndCatch.run();
                } else {
                    try {
                        lock.wait();

                        if (isResolved) {
                            closeAndAccept.run();
                        } else if (isRejected) {
                            if (!isCaught) {
                                throw new RuntimeException(rejected.getMessage());
                            }

                           closeAndCatch.run();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        return this;
    }

    /**
     * Executes a function if the current promise is resolved and executes another if it is rejected
     * @param callback Function that will be executed at the end of the promise
     * @param caught Function that will be executed at the promise is rejected
     */
    @NotNull
    public Promise<T> then(@Nonnull Consumer<T> callback, Callable<T> caught) {
        if (!Objects.isNull(caught)) catchException(caught);
        return then(callback);
    }

    /**
     * Executes a function at the end of the current promise regardless of whether it was resolved or rejected
     * @param fn Function that will be executed at the end of the promise
     */
    @NotNull
    public Promise<T> onFinally(@Nonnull Runnable fn) {
        onFinal = fn;
        return this;
    }

    /**
     * Wait the result of this promise and return it
     */
    @Nullable
    public T await() throws Exception {
        if (isRejected) {
            if (!isCaught) {
                throw rejected;
            }

            return caught.call();
        }

        latch.await();

        if (onFinal != null && !onFinalCalled) {
            onFinal.run();
            onFinalCalled = true;
        }

        return received;
    }

    /**
     * Returns a single promise with all the given promises resolved
     * @param promises The promises to be resolved
     * @param <T> The value to be returned in promise
     */
    @SafeVarargs
    public static <T> PromiseArray<T> all(Promise<T>... promises) {
        return new PromiseArray<>(promises);
    }

    /**
     * Returns a single promise with all the given promises resolved
     * @param values The values to be resolved
     * @param <T> The value to be returned in promise
     */
    @SafeVarargs
    public static <T> PromiseArray<T> all(T... values) {
        ArrayList<Promise<T>> promises = new ArrayList<>(); // Creating an array list to store resolved promises

        for (T value : values) {
            promises.add(Promise.resolver(value));
        }

        @SuppressWarnings("unchecked")
        Promise<T>[] arr = promises.toArray(new Promise[0]);

        return all(arr); // Values transformed into promises, running original function
    }

    /**
     * Creates a new resolved promise
     * @param value The value to resolve promise
     * @param <T> The value to be returned in promise
     */
    @NotNull
    public static <T> Promise<T> resolver(@Nullable T value) {
        Promise<T> promise = new Promise<>();
        promise.resolve(value);
        return promise;
    }

    /**
     * Creates a new resolved promise
     * @param value The promise value to resolve promise
     * @param <T> The value to be returned in promise
     */
    @NotNull
    public static <T> Promise<T> resolver(Promise<T> value) {
        Promise<T> promise = new Promise<>();
        try {
            if (Objects.isNull(value)) {
                promise.resolve(null);
            } else {
                promise.resolve(value.await());
            }
        } catch (Exception e) {
            promise.reject(e);
        }

        return promise;
    }

    /**
     * Creates a new rejected promise
     * @param e The exception value to reject promise
     * @param <T> The value to be returned in promise
     */
    @NotNull
    public static <T> Promise<T> rejected(Exception e) {
        Promise<T> promise = new Promise<>();
        promise.reject(e);
        return promise;
    }

    /**
     * Creates a new rejected promise
     * @param e The exception message to reject promise
     * @param <T> The value to be returned in promise
     */
    @NotNull
    public static <T> Promise<T> rejected(String e) {
        Promise<T> promise = new Promise<>();
        promise.reject(e);
        return promise;
    }

    /**
     * Returns a promise that can be resolved or rejected as soon as one of the past promises is resolved or rejected
     * @param promises Promises to be resolved or rejected
     * @param <T> The value to be returned in promise
     */
    @SafeVarargs
    @NotNull
    public static <T> Promise<T> race(Promise<T>... promises) {
        ExecutorService executorService = Executors.newFixedThreadPool(promises.length);

        return new Promise<>((resolve, reject) -> {
            try {
                T result = executorService.invokeAny(Arrays.stream(promises).map((cp) -> (Callable<T>) cp::await).toList());
                resolve.accept(result);
            } catch (InterruptedException | ExecutionException e) {
                reject.accept(e);
            }

            executorService.shutdown(); // Shutdown the service to avoid improper code running
        });
    }
}
