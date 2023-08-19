package Locks;

import java.util.concurrent.locks.Condition;

public interface Lock {
    void lock();
    void unlock();
    Condition newCondition();
}
