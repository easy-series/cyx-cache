package com.caoyixin.cache.lock;

import com.caoyixin.cache.api.DistributedLock;

import java.time.Duration;

public class DefaultDistributedLock implements DistributedLock<String> {
    @Override
    public boolean tryLock(String key, Duration timeout) {
        return false;
    }

    @Override
    public void unlock(String key) {

    }
}
