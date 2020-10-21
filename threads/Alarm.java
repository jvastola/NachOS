package nachos.threads;
import java.util.PriorityQueue;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * 
     */
    PriorityQueue<SleepBundle> sleepQueue;

    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
        });
        
        sleepQueue = new PriorityQueue<>();
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        KThread.currentThread().yield();
        
        long currTime = Machine.timer().getTime();

        //Check the top most element of the queue (if its not empty) and see if its past its wakeup time
        //While loop is used here to make sure that all threads that need to be awoken at this time is awoken at the same time
        while(!sleepQueue.isEmpty() && sleepQueue.peek().wakeUpTime <= currTime) {
            //Remove the thread from the sleep queue and put it on the ready queue
            sleepQueue.poll().thread.ready();
        }

    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        //Check if x is invalid (0 or negative)
        if(x <= 0) {
            return;
        }

        //Add thread to queue based on its wake up time (x + Timer.getTime());
        long wakeUpTime = Machine.timer().getTime() + x;

        //Create a new sleepbundle with out current thread and the time to wake it up at
        SleepBundle toAdd = new SleepBundle(KThread.currentThread(), wakeUpTime);

        //Put thread to sleep on the lock inside sleep bundle
        toAdd.sleepLock.acquire();

        //Add the bundle to our sleep queue
        sleepQueue.add(toAdd);

        //Put thread to sleep on its sleepCond
        toAdd.sleepCond.sleep();
    }


    //Testing methods
    public static void alarmTest1() {
        int durations[] = {0, 20, 500, 1000000};
        long t0, t1;
        for (int d : durations) {    
            t0 = Machine.timer().getTime();    
            ThreadedKernel.alarm.waitUntil (d);    
            t1 = Machine.timer().getTime();    
            System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
        }
    }

    public static void selfTest() {
        alarmTest1();
    }
}
