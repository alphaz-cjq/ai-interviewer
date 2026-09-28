package com.cjq.service;

public interface DistributedLockService {
    boolean tryLock(String key,String value,long expireSeconds);
    boolean unlock(String key,String value);
}
