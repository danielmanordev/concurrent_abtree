package Locks;

public interface ReadWriteLock {
    Lock readLock();
    Lock writeLock();
}