package com.msc.utils;

/**
 * ThreadLocal utility for storing current user ID
 */
public class ThreadLocalUtil {

    private static final ThreadLocal<Long> THREAD_LOCAL = new ThreadLocal<>();

    public static void set(Long userId) {
        THREAD_LOCAL.set(userId);
    }

    public static Long get() {
        return THREAD_LOCAL.get();
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }
}