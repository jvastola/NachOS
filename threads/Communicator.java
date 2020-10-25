package nachos.threads;

import nachos.machine.*;


import java.util.*;
/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
  
    
	/**
     * Allocate a new communicator.
     */
    public Communicator() {
    	
        oneLock = new Lock();
        
    	S_WaitingQueue = new Condition(oneLock);
    	S_SendingWord = new Condition(oneLock);
    	L_WaitingQueue = new Condition(oneLock);
        L_ReceivingWord = new Condition(oneLock);
        
    	waitingL = false;
    	waitingS = false;
    	messageReceived = false;
    	
    	
    }
    

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	oneLock.acquire();
    	while(waitingS) {
    		S_WaitingQueue.sleep();
    		
    	}
    	waitingS = true;
    	hold = word;
    	while( !waitingL || !messageReceived) {
    		L_ReceivingWord.wake();
    		S_SendingWord.sleep();
    		
    	}
    	waitingL = false;
    	waitingS = false;
    	messageReceived = false;
    	S_WaitingQueue.wake();
    	L_WaitingQueue.wake();
    	oneLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	oneLock.acquire();
    	
    	while(waitingL) {
    		L_WaitingQueue.sleep();
    		
    	}
    	waitingL = true;
    	
    	while(!waitingS) {
    		L_ReceivingWord.sleep();
    		
    	}
    	S_SendingWord.wake();
    	messageReceived = true;
    	oneLock.release();
    	
	    return hold;
    }

    public static void testCase() {
    	final Communicator myComm = new Communicator();
    	
    	KThread thread1 = new KThread(new Runnable() {
    		public void run() {
    			System.out.println("Thread 1 begin listening");
    			myComm.listen();
    			System.out.println("Thread 1 finished listening");
    		}
    	});
    	KThread thread2 = new KThread(new Runnable() {
    		public void run() {
    			System.out.println("Thread 2 begin speaking");
    			myComm.speak(1);
    			System.out.println("Thread 2 finished speaking");
    			
    		}
    	});
    	thread2.fork();
    	thread1.fork();
    	thread2.join();
    	thread1.join();
    }

    private Lock oneLock;
   
    private Condition S_WaitingQueue;
    private Condition S_SendingWord;
    private Condition L_WaitingQueue;
    private Condition L_ReceivingWord;
   
    private boolean waitingL;
    private boolean waitingS;
    private boolean messageReceived;
    
    private int hold;  
    
}