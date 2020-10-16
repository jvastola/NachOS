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
      //  System.out.println(Machine.timer().getTime());
        long currTime = Machine.timer().getTime();

        while(!sleepQueue.isEmpty() && sleepQueue.peek().wakeUpTime <= currTime) {
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

        SleepBundle toAdd = new SleepBundle(KThread.currentThread(), wakeUpTime);
        //Put thread to sleep on the lock inside sleep bundle
        toAdd.sleepLock.acquire();
        sleepQueue.add(toAdd);
        toAdd.sleepCond.sleep();

        //Put the thread to sleep/block?
        

        //Change this so its instead a queue of locks -> wakeUpTime + sleepCond, 
        //have the thread acquire the lock and then sleep on it, 
        //create the threadbundle obj and put it into the priority queue
        //I think i also need to put the Lock
        

    }


    //Testing methods
    public static void alarmTest1() {
        int durations[] = {0, 20, 50, 1000000};
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
