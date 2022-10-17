package com.codeages.javaskeletonserver.common.cache;

import java.util.Optional;

@FunctionalInterface
public interface CacheGetCallable<T> {
    Optional<T> get();
}
