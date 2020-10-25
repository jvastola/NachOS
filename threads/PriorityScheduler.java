package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer priority from waiting
	 *            threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority - 1);

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
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			if (transferPriority)
				waiting = new TreeSet<ThreadState>(new PriorityComparator());
			else
				waiting = new TreeSet<ThreadState>(new EffectivePriorityComparator());
		}
		public void donationUpdate()		// update resource owner's effective priority via donations
		{
			if (owner != null)
			{
				owner.updatePriority();
				for (PriorityQueue p : owner.owned)	// iterate through every resource queue
				{
					if (!p.waiting.isEmpty())
						for (ThreadState t : p.waiting)
						{
							t.updatePriority();		// update every thread's effective priority 
							if (t.epriority > owner.epriority)
							{
								owner.epriority = t.epriority;
							}
						
						
						}
				}
			}
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			
			//print();
			ThreadState nextThread = this.pickNextThread();
			if (nextThread == null)
				return null;
			nextThread.acquire(this);
			return nextThread.thread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return, without
		 * modifying the state of this queue.
		 *
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			// implement me
			if (!waiting.isEmpty())
				return waiting.last();
			return null;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());

			for (Iterator<ThreadState> iterator = waiting.iterator(); iterator.hasNext();) {
				ThreadState state = iterator.next();
				System.out.print(state.getThread());
			}
			System.out.println();
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting threads to
		 * the owning thread.
		 */
		public boolean transferPriority;

		TreeSet<ThreadState> waiting;
		ThreadState owner = null;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's priority,
	 * its effective priority, any objects it owns, and the queue it's waiting for,
	 * if any.
	 *
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param thread
		 *            the thread this state belongs to.
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
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// implement me
			return epriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param priority
		 *            the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;
			this.epriority = this.priority;		
			// implement me
			this.updatePriority();

			if (waitingFor != null) {// update priority in
				waitingFor.waiting.remove(this);
				waitingFor.waiting.add(this);
				waitingFor.donationUpdate();
			}
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is the
		 * associated thread) is invoked on the specified priority queue. The associated
		 * thread is therefore waiting for access to the resource guarded by
		 * <tt>waitQueue</tt>. This method is only called if the associated thread
		 * cannot immediately obtain access.
		 *
		 * @param waitQueue
		 *            the queue that the associated thread is now waiting on.
		 *
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me
			if (waitingFor != null) // illegal to be in multile queues
			{
				waitingFor.waiting.remove(this);
			}
			this.waittime = 0;
			for (ThreadState t : waitQueue.waiting) {// increment other threads in queue
				t.waittime++;
			}
			waitQueue.waiting.add(this);
			waitingFor = waitQueue;
			if (owned != null && owned.contains(waitingFor)) // update set of acquired resources
			{
				owned.remove(waitingFor);
			}
			waitQueue.donationUpdate();		// process donations in queue, since a new thread is queued

		}

		/**
		 * Called when the associated thread has acquired access to whatever is guarded
		 * by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			// implement me
			if (waitingFor != null) {
				if (waitingFor == waitQueue) {
					waitingFor.waiting.remove(this);// stop waiting
					waitingFor = null;
				}
			}
			waitQueue.waiting.remove(this);
			owned.add(waitQueue);
			if (waitQueue.owner != null && waitQueue.owner != this) {
				waitQueue.owner.owned.remove(waitQueue);
				waitQueue.owner.updatePriority();
			}
			waitQueue.owner = this;

			waitQueue.donationUpdate();

		}

		protected void updatePriority() {
			this.epriority = this.priority;
			if(owned!=null) {
				for(PriorityQueue res: owned) {
					for (ThreadState t : res.waiting){
						if (t.getEffectivePriority()> t.epriority) {
							this.epriority = t.getEffectivePriority();
							if(waitingFor != null && waitingFor.owner != null)
								waitingFor.owner.updatePriority();
						}
					}
				}
			}
		}

		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority =priorityDefault;
		/** The effective priority of the associated thread. */
		protected int epriority = priorityDefault;
		/** The time thread began waiting. */
		protected int waittime = 0;
		/** The ThreadQueue that the associated thread waiting for. */
		PriorityQueue waitingFor = null;
		/** The hashset sotring donated priorities  */
		HashSet<PriorityQueue> owned = new HashSet<PriorityQueue>();

	}

	class PriorityComparator implements Comparator<ThreadState> {
		@Override
		public int compare(ThreadState t1, ThreadState t2) {
			if (t1.getPriority() == t2.getPriority())
				return t1.waittime - t2.waittime;
			else
				return t1.getPriority() - t2.getPriority();
		}
	}

	class EffectivePriorityComparator implements Comparator<ThreadState> {
		@Override

		public int compare(ThreadState t1, ThreadState t2) {
			if (t1.getEffectivePriority() == t2.getEffectivePriority())
				return t1.waittime - t2.waittime;
			else
				return t1.getEffectivePriority() - t2.getEffectivePriority();
		}
}
	public static void selfTest() {
		System.out.println("PriorityScheduler test:");
		PriorityScheduler s = new PriorityScheduler();
		ThreadQueue queue = s.newThreadQueue(true);
		ThreadQueue queue2 = s.newThreadQueue(true);
		ThreadQueue queue3 = s.newThreadQueue(true);
		
		KThread thread1 = new KThread();
		KThread thread2 = new KThread();
		KThread thread3 = new KThread();
		KThread thread4 = new KThread();
		KThread thread5 = new KThread();
		thread1.setName("thread1");
		thread2.setName("thread2");
		thread3.setName("thread3");
		thread4.setName("thread4");
		thread5.setName("thread5");
		boolean intStatus = Machine.interrupt().disable();
		
		queue3.acquire(thread1);
		queue.acquire(thread1);
		queue.waitForAccess(thread2);
		queue2.acquire(thread4);
		queue2.waitForAccess(thread1);
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());
		
		s.getThreadState(thread2).setPriority(4);
		
		System.out.println("After setting thread2's EP=4:");
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());
		
		Machine.interrupt().restore(intStatus);
		System.out.println("--------End PriorityScheduler test------------------");
	}
}
