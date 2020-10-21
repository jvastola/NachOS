package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

public class PriorityScheduler extends Scheduler {

	public PriorityScheduler() {
	}

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

	public static final int priorityDefault = 1;
	public static final int priorityMinimum = 0;
	public static final int priorityMaximum = 7;

	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);
		return (ThreadState) thread.schedulingState;
	}

	protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			if (transferPriority)
				waiting = new TreeSet<ThreadState>(new PriorityComparator());
			else
				waiting = new TreeSet<ThreadState>(new EffectivePriorityComparator());
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			// Calls ThreadState.waitForAccess(This PriorityQueue)
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			// Calls ThreadState.acquire(This PriorityQueue)
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			ThreadState nextThread = this.pickNextThread();
			if (nextThread == null)
				return null;
			nextThread.acquire(this);
			return nextThread.thread;
		}

		protected ThreadState pickNextThread() {
			// implement me
			if (!waiting.isEmpty())
				return waiting.last();
			return null;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
			// no I don't
		}

		public boolean transferPriority;
		TreeSet<ThreadState> waiting;
		ThreadState owner= null;

	}

	protected class ThreadState {
		public ThreadState(KThread thread) {
			this.thread = thread;
			setPriority(priorityDefault);
		}

		public int getPriority() {
			return priority;
		}

		public int getEffectivePriority() {
			// implement me
			return epriority;
		}

		public void setPriority(int priority) {
			if (this.priority == priority)
				return;
			this.priority = priority;
			// implement me
		    this.updatePriority(this);

			if (waitingQ != null) {// update priority in
				waitingQ.waiting.remove(this);
				waitingQ.waiting.add(this);
			}
		}

		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me
			this.waittime = 0;
			for (ThreadState t : waitQueue.waiting) {// increment other threads in queue
				t.waittime++;
			}
			waitQueue.waiting.add(this);
			waitingQ = waitQueue;

		}

		public void acquire(PriorityQueue waitQueue) {
			// implement me
			if (waitQueue.owner != null && waitQueue.owner != this) {
				waitQueue.owner.owned.remove(waitQueue);
					if(waitQueue.transferPriority)
						updatePriority(waitQueue.owner);	;
			}
			waitQueue.owner = this;
			owned.add(waitQueue);
		}
		protected void updatePriority(ThreadState s) {
			int newEffectivePriority = priority;
			for (PriorityQueue res : owned) if (res.transferPriority && !res.waiting.isEmpty())
					if (res.waiting.last().epriority > newEffectivePriority)
						newEffectivePriority = res.waiting.last().epriority;
			if (newEffectivePriority != epriority) {
				if (this.waitingQ != null)
					Lib.assertTrue(this.waitingQ.waiting.remove(this));
				epriority = newEffectivePriority;
				if (this.waitingQ != null) {
					this.waitingQ.waiting.add(this);
					Lib.assertTrue(this.waitingQ.owner != null);
					updatePriority(getThreadState(this.waitingQ.owner.thread));
				}
			}
		}

		protected KThread thread;
		protected int priority;
		protected int epriority = priority;
		public int waittime;
		PriorityQueue waitingQ;
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

		public int compare(ThreadState t0, ThreadState t1) {
			if (t0.getEffectivePriority() == t1.getEffectivePriority())
				return t0.waittime - t1.waittime;
			else
				return t0.getEffectivePriority() - t1.getEffectivePriority();
		}

	}
}
