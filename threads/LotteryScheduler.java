package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

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
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new LotteryQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
    Lib.assertTrue(Machine.interrupt().disabled());
                
    return getNewThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
    Lib.assertTrue(Machine.interrupt().disabled());
                
    return getNewThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
    Lib.assertTrue(Machine.interrupt().disabled());
                
    Lib.assertTrue(priority >= priorityMinimum &&
            priority <= priorityMaximum);
    
    getNewThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
    boolean intStatus = Machine.interrupt().disable();
                
    KThread thread = KThread.currentThread();

    int priority = getPriority(thread);
    if (priority == priorityMaximum)
        return false;

    setPriority(thread, priority+1);

    Machine.interrupt().restore(intStatus);
    return true;
    }

    public boolean decreasePriority() {
    boolean intStatus = Machine.interrupt().disable();
                
    KThread thread = KThread.currentThread();

    int priority = getPriority(thread);
    if (priority == priorityMinimum)
        return false;

    setPriority(thread, priority-1);

    Machine.interrupt().restore(intStatus);
    return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 1;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = Integer.MAX_VALUE;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected NewThreadState getNewThreadState(KThread thread) {
    if (thread.schedulingState == null)
        thread.schedulingState = new NewThreadState(thread);

    return (NewThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class LotteryQueue extends ThreadQueue {
    LotteryQueue(boolean transferPriority) {
        this.transferPriority = transferPriority;
            if (transferPriority)
                waiting = new TreeSet<NewThreadState>(new PriorityComparator());
            else
                waiting = new TreeSet<NewThreadState>(new EffectivePriorityComparator());
    }
    
    public int max(int x, int y) {
        return x>y ? x : y;
    }
    public void donationUpdate(){//update resource owner from donations
        if (owner != null){
            owner.updatePriority();//get newest priority
            for (PriorityQueue p : owner.owned){//iterate through every resource queue
                if (!p.waiting.isEmpty())
                    for (NewThreadState t : p.waiting){
                        t.updatePriority();	// update every depending thread's effective priority 
                        owner.ePriority=max(t.ePriority,owner.ePriority);
                    }
            }
        }
    }

    public void waitForAccess(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());
        getNewThreadState(thread).waitForAccess(this);
    }

    public void acquire(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());
        getNewThreadState(thread).acquire(this);
    }

    public KThread nextThread() {
        Lib.assertTrue(Machine.interrupt().disabled());
            //print();
            NewThreadState nextThread = this.pickNextThread();
            if (nextThread == null)//null if nothing queued
                return null;
            nextThread.acquire(this);//acquire nextthread
            return nextThread.thread;
    }

    /**
     * Return the next thread that <tt>nextThread()</tt> would return,
     * without modifying the state of this queue.
     *
     * @return	the next thread that <tt>nextThread()</tt> would
     *		return.
        */
    protected NewThreadState pickNextThread() {
        donationUpdate();
        NewThreadState ret = null;
        int totalTickets = getTotalTickets();
        if(totalTickets>0){
            int choose = (new Random()).nextInt(totalTickets) + 1;
            for(KThread thread: waiting){
                choose-= getNewThreadState(thread).getEffectivePriority();
                if(choose<=0){
                    ret = getNewThreadState(thread);
                    break;
                }
            }
        }
        return ret;
    }
    public int getTotalTickets() {
        int totalTickets = 0; 
        for (NewThreadState t : waiting) totalTickets += getNewThreadState(t.getThread()).getEffectivePriority();
        return totalTickets;
    }
    
    public void print() {
        Lib.assertTrue(Machine.interrupt().disabled());
    }

    /**
     * <tt>true</tt> if this queue should transfer priority from waiting
     * threads to the owning thread.
     */
    public boolean transferPriority;
        /** The main treeset of waiting threads */	   
    TreeSet<NewThreadState> waiting;
    /** The NewThreadState indicating which thread acquires priority */	   
    NewThreadState owner = null;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class NewThreadState {
    /**
     * Allocate a new <tt>NewThreadState</tt> object and associate it with the
     * specified thread.
     *
     * @param	thread	the thread this state belongs to.
     */
    public ThreadState(KThread thread) {
        this.thread = thread;
        
        setPriority(priorityDefault);
    }
    public KThread getThread() {	
        return thread;	
    }
    /**
     * Return the priority of the associated thread.
     *
     * @return	the priority of the associated thread.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Return the effective priority of the associated thread.
     *
     * @return	the effective priority of the associated thread.
     */
    public int getEffectivePriority() {
        return ePriority;
    }

    /**
     * Set the priority of the associated thread to the specified value.
     *
     * @param	priority	the new priority.
     */
    public void setPriority(int priority) {
        if (this.priority == priority)
        return;
        
        this.priority = priority;
        this.ePriority = this.priority;
            if (waitingFor != null) {
                waitingFor.waiting.remove(this);
                waitingFor.waiting.add(this);
                waitingFor.donationUpdate();//process new priority
            }
    }

    /**
     * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
     * the associated thread) is invoked on the specified priority queue.
     * The associated thread is therefore waiting for access to the
     * resource guarded by <tt>waitQueue</tt>. This method is only called
     * if the associated thread cannot immediately obtain access.
     *
     * @param	waitQueue	the queue that the associated thread is
     *				now waiting on.
        *
        * @see	nachos.threads.ThreadQueue#waitForAccess
        */
    public void waitForAccess(PriorityQueue waitQueue) {
        if (waitingFor != null)	{
            waitingFor.waiting.remove(this);
        }
        this.waittime = 0;
        for (ThreadState t : waitQueue.waiting)	{
            t.waittime++;//increment time for other threads in queue
        }
        waitQueue.waiting.add(this);
        waitingFor = waitQueue;
        if (owned != null && owned.contains(waitingFor)){
            owned.remove(waitingFor);
        }
        waitQueue.donationUpdate();//process donations in queue
    
    }

    /**
     * Called when the associated thread has acquired access to whatever is
     * guarded by <tt>waitQueue</tt>. This can occur either as a result of
     * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
     * <tt>thread</tt> is the associated thread), or as a result of
     * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
     *
     * @see	nachos.threads.ThreadQueue#acquire
     * @see	nachos.threads.ThreadQueue#nextThread
     */
    public void acquire(PriorityQueue waitQueue) {
        if (waitingFor != null)	{// stop waiting, since thread has acquired it
            if (waitingFor == waitQueue){
                waitingFor.waiting.remove(this);
                waitingFor = null;
            }
        }
        waitQueue.waiting.remove(this);
        owned.add(waitQueue);//update set of acquired resources

        if (waitQueue.owner != null && waitQueue.owner != this){
            // replace previous resource owner and update its priority
            waitQueue.owner.owned.remove(waitQueue);
            waitQueue.owner.updatePriority();
        }
        waitQueue.owner = this;
        waitQueue.donationUpdate();	// process donations, since new thread acquired the resource
    }	
    public void updatePriority(){
            this.ePriority = this.priority;
            if (owned != null)
                for (PriorityQueue Q : owned){
                    for (ThreadState t : Q.waiting){//depending threads
                        if(t.getEffectivePriority()>this.ePriority){
                            this.ePriority = t.getEffectivePriority();
                            if (waitingFor != null && waitingFor.owner != null)
                                waitingFor.owner.updatePriority();//update owners priority
                        }
                    }
                }
        }
        /** The thread with which this object is associated. */	   
        public KThread thread;
        /** The priority of the associated thread. */
        protected int priority = priorityDefault;
        /** The effective priority of the associated thread. */
        public int ePriority = priorityDefault;
        /** The time thread began waiting. */
        public int waittime =0;
        /** The ThreadQueue that the associated thread waiting for. */
        PriorityQueue waitingFor= null;
        /** The hashset sorting donated priorities  */
        HashSet<PriorityQueue> owned = new HashSet<PriorityQueue>();
    }
    
    
    
    class PriorityComparator implements Comparator<ThreadState>
    {
        @Override
        public int compare(ThreadState t1, ThreadState t2) {
            if (t1.getPriority() == t2.getPriority()) return t1.waittime - t2.waittime;
            else return t1.getPriority() - t2.getPriority();
        }
    }

    class EffectivePriorityComparator implements Comparator<ThreadState>
    {
        @Override
        public int compare(ThreadState t1, ThreadState t2) {
            if (t1.getEffectivePriority() == t2.getEffectivePriority()) return t1.waittime - t2.waittime;
            else return t1.getEffectivePriority() - t2.getEffectivePriority();
        }
    }
    public static void selfTest() {
        System.out.println("PriorityScheduler test");
        PriorityScheduler s = new PriorityScheduler();
        ThreadQueue queue1 = s.newThreadQueue(true);
        ThreadQueue queue2 = s.newThreadQueue(true);
        ThreadQueue queue3 = s.newThreadQueue(true);
        
        KThread thread1 = new KThread();
        KThread thread2 = new KThread();
        KThread thread3 = new KThread();
        thread1.setName("thread1");
        thread2.setName("thread2");
        thread3.setName("thread3");

        boolean intStatus = Machine.interrupt().disable();
        
        queue3.acquire(thread1);
        queue1.acquire(thread1);
        queue1.waitForAccess(thread2);
        queue2.acquire(thread3);
        queue2.waitForAccess(thread1);
        System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
        System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
        System.out.println("thread4 EP="+s.getThreadState(thread3).getEffectivePriority());
        
        s.getThreadState(thread1).setPriority(1);
        s.getThreadState(thread2).setPriority(3);
        s.getThreadState(thread3).setPriority(4);
        
        System.out.println("Thread1: 1, Thread2: 3, Thread3:4");
        System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
        System.out.println("thread1 P="+s.getThreadState(thread1).getPriority());
        System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
        System.out.println("thread4 EP="+s.getThreadState(thread3).getEffectivePriority());
        
        s.getThreadState(thread1).setPriority(4);
        s.getThreadState(thread2).setPriority(2);
        s.getThreadState(thread3).setPriority(1);
        
        System.out.println("Thread1: 4, Thread2: 3, Thread3:1");
        System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
        System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
        System.out.println("thread4 EP="+s.getNewThreadState(thread3).getEffectivePriority());
        
        Machine.interrupt().restore(intStatus);
    }
}
    
