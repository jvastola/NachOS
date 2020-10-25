package nachos.threads;

public class SleepBundle implements Comparable<SleepBundle> {
    long wakeUpTime;
    KThread thread;

    public SleepBundle(KThread thread, long wakeUpTime) {
        this.wakeUpTime = wakeUpTime;
        this.thread = thread;
    }

    public int compareTo(SleepBundle t2) {
        if (wakeUpTime > t2.wakeUpTime) {
            return 1;
        } else if (wakeUpTime < t2.wakeUpTime) {
            return -1;
        } else {
            return 0;
        }

    }

}