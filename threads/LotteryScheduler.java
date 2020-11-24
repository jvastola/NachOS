package nachos.threads;

import nachos.machine.*;

import java.util.Random;
import java.util.Iterator;
import java.util.HashSet;

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
    public LotteryScheduler() {
    }
    public static int priorityMinimum = 1;
    public static int priorityDefault = 1;
    public static int priorityMaximum = Integer.MAX_VALUE;

        @Override
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

    public ThreadQueue newThreadQueue(boolean transferPriority){
    	return new LotteryQueue(transferPriority);
    }
    
    protected class LotteryQueue extends PriorityScheduler.PriorityQueue{
    	LotteryQueue(boolean transferPriority){
    		super(transferPriority)  ;
    		waitQueue = new HashSet<ThreadState>();
    	}
		@Override
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}
		@Override
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
        }
		@Override
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
				ThreadState nextThread = this.pickNextThread();
				if (nextThread == null)
					return null;
				nextThread.acquire(this);
				return nextThread.thread;
		}
		@Override
		protected ThreadState pickNextThread() {
			if (totalTickets == 0) return null; //nothing to do
            int choose =  (new Random()).nextInt(totalTickets); //random ticket
			for (ThreadState T : waitQueue){ 
				if (choose >= T.base && choose < T.base + T.getEffectivePriority()){ 
					return T;//choose next thread
				}
			}
			return null;
		}
		
    	public void updateTotalTickets(){
    		totalTickets = 0; // number of tickets starts at 0
    		for (ThreadState T : waitQueue){ // all waiting threads donate then submit tickets
    			if (owner != null && this.transferPriority){
					T.revert();
					if (T.ePriority > 0){
						T.donated = T.ePriority; // save amount donated
						owner.ePriority += T.donated; // donate 
						T.borrower = owner; // save a reference to thread donated to
					}
				}
    			T.base = totalTickets; 
    			totalTickets += T.ePriority;
    		}
    	}
        ThreadState owner;
    	HashSet<ThreadState> waitQueue;
    	int totalTickets = 0; // number of tickets in this queue
    	
    }
    protected class ThreadState extends PriorityScheduler.ThreadState{

		public ThreadState(KThread thread) {
			super(thread)  ;
		}
	    @Override
		public int getEffectivePriority() {

			for (LotteryQueue Q : owned){ 
				for (ThreadState T : Q.waitQueue){
					T.getEffectivePriority();
				}
				Q.updateTotalTickets(); 
			}
			revert(); // reclaim donation
			if (waiting != null) {
				waiting.updateTotalTickets();//try donating
			} 
        	return (ePriority > 0)? ePriority:1;
		}
	    @Override
		public void setPriority(int priority) {
			if (priority > 0 && priority != this.priority){
				this.priority = this.ePriority = priority;
				updatePriority(); 
			}
		}
		public void waitForAccess(LotteryQueue waitQueue) {
			if (owned.remove(waitQueue)){ 
				waitQueue.owner = null;//previously owned
			}
			waiting = waitQueue; 
			waiting.waitQueue.add(this); 
			updatePriority(); 
		}
		public void acquire(LotteryQueue ownedQueue) {
			if (waiting == ownedQueue){ 
				waiting.waitQueue.remove(this);//stop waiting
				waiting = null;
			}
			if (ownedQueue.owner != null && ownedQueue.owner != this){
				ownedQueue.owner.owned.remove(ownedQueue); 
				ownedQueue.owner.updatePriority(); 
			}
			ownedQueue.owner = this; 
			updatePriority();
		}
		public void updatePriority(){
			for (LotteryQueue lq : owned){ 
				lq.updateTotalTickets();//all owned donations
			}
			if (waiting != null) waiting.updateTotalTickets();
		}
		public void revert(){
			if (borrower != null){ 
				borrower.revert(); 
				borrower.ePriority -= donated; 
				donated = 0;
				borrower = null; 
			}
		}
		public int donated = 0;
    	public int base = 0;
    	public ThreadState borrower = null;
    	public HashSet<LotteryQueue> owned =new HashSet<LotteryQueue>();
    	public LotteryQueue waiting = null; 
    	
	}
    
}
