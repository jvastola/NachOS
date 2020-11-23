package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Comparator;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /** Allocate a new lottery scheduler.*/
    public LotteryScheduler() {
    }
    public static final int priorityDefault = 1;
    public static final int priorityMinimum = 1;
    public static final int priorityMaximum = Integer.MAX_VALUE;    

    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new LotteryQueue();
        }
    protected class LotteryQueue extends PriorityQueue {
        LotteryQueue(){
            TreeSet<ThreadState> waiting = new TreeSet<ThreadState>();
        }
        protected ThreadState pickNextThread() {
            donationUpdate();
            ThreadState ret = null;
            int totalTickets = getTotalTickets();
            if(totalTickets>0){
                int choose = (new Random()).nextInt(totalTickets) + 1;
                for(ThreadState thread: waiting){
                    choose-= getThreadState(thread.getThread()).getEffectivePriority();
                    if(choose<=0){
                        ret = getThreadState(thread.getThread());
                        break;
                    }
                }
            }
            return ret;
        }
        public int getTotalTickets() {
            int totalTickets = 0; 
            for (ThreadState t : waiting) totalTickets += getThreadState(t.getThread()).getEffectivePriority();
            return totalTickets;
        }   
        TreeSet<ThreadState> waiting;
    }
}
    
    
