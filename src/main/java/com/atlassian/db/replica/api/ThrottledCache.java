package com.atlassian.db.replica.api;

import com.atlassian.db.replica.internal.util.ThreadSafe;
import com.atlassian.db.replica.spi.SuppliedCache;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;


/**
 * Every write can potentially bump the LSN, however it can be bumped only by one thread at the time.
 * Other threads are waiting for LSN to be bumped.
 */
@ThreadSafe
public class ThrottledCache<T> implements SuppliedCache<T> {
    private AtomicReference<CountDownLatch> latch;
    private T value = null;

    @Override
    public Optional<T> get(Supplier<T> supplier) {
        maybeBump(supplier);
        return Optional.ofNullable(value);
    }

    private void maybeBump(Supplier<T> supplier) { //TODO: Do we need to support re-entrance?
        final CountDownLatch newLatch = new CountDownLatch(1);
        boolean firstRun = true;
        while (true) {
            final boolean doIHaveALock = this.latch.compareAndSet(null, newLatch);
            if (doIHaveALock) {
                try {
                    value = supplier.get();   // TODO: Error handling. Now one thread fetches the data, and other threads assume it's refreshed after it finishes.
                    newLatch.countDown();
                } finally {
                    this.latch.compareAndSet(newLatch, null);
                }
                // I had a lock and bumped LSN. No need to wait more.
                break;
            } else {
                final CountDownLatch countDownLatch = this.latch.get();
                if (countDownLatch != null) {// Another thread still keeps the latch. Let's wait for the thread to finish.
                    try {
                        countDownLatch.await(10, TimeUnit.SECONDS);  //TODO: what timeout should I use?
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    //the latch reference has been already released. The thread that was holding it already done
                }
                if (firstRun) {
                    //We still can't be sure about the data. The propagation may start after we did a database modification. We
                    // Need to do another round.
                    firstRun = false;
                } else {
                    // Should be ok. I waited for a thread that started bumping after I wanted to bump.
                    break;
                }
            }
        }
    }
}
