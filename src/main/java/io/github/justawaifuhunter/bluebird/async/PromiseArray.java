package io.github.justawaifuhunter.bluebird.async;

import java.util.ArrayList;

public class PromiseArray<T> extends Promise<ArrayList<T>> {
    volatile ArrayList<T> resolved;

    @SafeVarargs
    PromiseArray(Promise<T> ...promises) {
        super();
        resolved = new ArrayList<>();

        for (Promise<T> promise : promises) {
            try {
                resolved.add(promise.await());
            } catch (Exception e) {
                this.reject(e);
            }
        }

        this.resolve(resolved);
    }

    public void add(Promise<T> promise) {
        try {
            resolved.add(promise.await());
        } catch (Exception e) {
            this.reject(e);
        }

        this.resolve(resolved);
    }

    public void remove(Promise<T> promise) {
        try {
            resolved.remove(promise.await());
        } catch (Exception e) {
            this.reject(e);
        }

        this.resolve(resolved);
    }
}