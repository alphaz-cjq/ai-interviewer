package com.cjq.service;



public interface IdempotencyService {
    boolean tryAcquire(String token,long expireSeconds);
}
