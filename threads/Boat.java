//GENERAL PSEUDOCODE
//while there are adults on Oahu
//	send 2 children to Molokai
//	send 1st child back to Oahu
//	send 1 adult to Molokai
//	send 2nd child back to Oahu
//
//while there are more than 2 children on Oahu
//	send 2 children to Molokai
//	send 1st child back to Oahu
//send remaining 2 children to Molokai
//terminate simulation

package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat {
    static BoatGrader bg;
    final static boolean OAHU = false;
    final static boolean MOLO = true;
    //global ints for island and boat populations
    static int childrenOnOahu, adultsOnOahu, childrenOnMolo, adultsOnMolo, passengers;
    //global boolean to keep track of where the boat is and if the sim is finished
    static boolean boatLocation, finished;
    //locks for each island - loading/unloading and transporting people
    static Lock oahu = new Lock(), molo = new Lock();
    //conditions for allowing people on and off the boat and for sync
    static Condition adultWaitingOnOahu = new Condition(oahu), childWaitingOnOahu = new Condition(oahu), childWaitingOnMolo = new Condition(molo), 
    		childWaitingForPartner = new Condition(oahu);
    //to keep begin from terminating too early
    static Semaphore terminate = new Semaphore(0);
    public static void selfTest() {
	BoatGrader b = new BoatGrader();
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin(int adults, int children, BoatGrader b ) {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;
	
	// Instantiate global variables here
	childrenOnOahu = 0;
	adultsOnOahu = 0;
	childrenOnMolo = 0;
	adultsOnMolo = 0;
	passengers = 0;
	boatLocation = OAHU;
	finished = false;
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.
//		Runnable r = new Runnable() {
//	    public void run() {
//                SampleItinerary();
//            }
//        };
//        KThread t = new KThread(r);
//        t.setName("Sample Boat Thread");
//        t.fork();

		int i;
		
        Runnable c = new Runnable() {
    	    public void run() {
                ChildItinerary();
            }
        };
        for (i = 0; i < children; i++) {
        	KThread t = new KThread(c);
        	t.setName("Child Thread " + i);
        	t.fork();
        }
        
        Runnable a = new Runnable() {
    	    public void run() {
                AdultItinerary();
            }
        };
        for (i = 0; i < adults; i++) {
        	KThread t = new KThread(a);
        	t.setName("Adult Thread " + i);
        	t.fork();
        }
        terminate.P();
        System.out.println("Oahu: Children: " + childrenOnOahu + " Adults: " + adultsOnOahu);
    	System.out.println("Molo: Children: " + childrenOnMolo + " Adults: " + adultsOnMolo);
    }

    static void AdultItinerary() {
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
    	oahu.acquire();
    	adultsOnOahu++;
    	oahu.release();
    	//System.out.println("Oahu: Children: " + childrenOnOahu + " Adults: " + adultsOnOahu);
    	//send 1 adult to Molokai
    	oahu.acquire();
    	//wait for a child to be available on Molokai before rowing an adult
    	adultWaitingOnOahu.sleep();
    	//dec/inc island population and boat population
    	adultsOnOahu--;
    	passengers++;
    	//pilot changes boat location to destination
    	boatLocation = MOLO;
    	oahu.release();
    	//row to Molokai
    	bg.AdultRowToMolokai();
    	
    	molo.acquire();
    	//get off boat and inc Molo population
    	passengers--;
    	adultsOnMolo++;
    	//wake child waiting to go back to Oahu
    	childWaitingOnMolo.wake();
    	molo.release();
    }

    static void ChildItinerary() {
    	oahu.acquire();
    	childrenOnOahu++;
    	oahu.release();
    	ThreadedKernel.alarm.waitUntil(100000);
    	//System.out.println("Oahu: Children: " + childrenOnOahu + " Adults: " + adultsOnOahu);
    	while (!finished) {
    		//very similar to child loop so less comments where they're ==
    		while (adultsOnOahu > 0) {
    			//send 2 children to Molokai
    			oahu.acquire();
    			while(passengers > 2 || boatLocation) {
    				childWaitingOnOahu.sleep();
    			}
    			if (passengers == 0) {
    				passengers++;
    				childWaitingOnOahu.wake();
    				childWaitingForPartner.sleep();
    				childrenOnOahu--;
    				bg.ChildRideToMolokai();
    			} else {
    				passengers++;
    				childWaitingForPartner.wake();
    				childrenOnOahu--;
    				bg.ChildRowToMolokai();
    				boatLocation = MOLO;
    			}
    			oahu.release();
    			
    			//send 1 child to Oahu
    			molo.acquire();
    			childrenOnMolo++;
    			passengers--;
    			if (passengers == 0) {
    				passengers++;
    				childrenOnMolo--;
    				bg.ChildRowToOahu();
    				molo.release();
    				
    				oahu.acquire();
    				passengers--;
    				childrenOnOahu++;
    				boatLocation = OAHU;
    				//send adult to Molokai
    				adultWaitingOnOahu.wake();
    				oahu.release();
    			} else {
    				//wait for adult to send child back to Oahu
    				childWaitingOnMolo.sleep();
    				//carry out return to Oahu
    				passengers++;
    				childrenOnMolo--;
    				bg.ChildRowToOahu();
    				molo.release();
    				
    				oahu.acquire();
    				passengers--;
    				childrenOnOahu++;
    				boatLocation = OAHU;
    				oahu.release();
    			}
    			//needed to sync transport correctly. Otherwise, with large thread counts some groups of children could get ahead of returning child
    			//slows down loop speed to mantain stability, could be reduced if working with smaller # of threads
    			ThreadedKernel.alarm.waitUntil(20000);
    			//SYNC TESTING
    			//System.out.println("ALoop: Oahu: Children: " + childrenOnOahu + " Adults: " + adultsOnOahu);
    			//System.out.println("ALoop: Molo: Children: " + childrenOnMolo + " Adults: " + adultsOnMolo);
    		}
    		//while there are more than 2 children on Oahu
    		while (childrenOnOahu > 2) {
    			//send 2 children to Molokai
    			oahu.acquire();
    			//keeps extra threads from looping unpredictably while 2 children do their thing
    			while(passengers > 2 || boatLocation) {
    				childWaitingOnOahu.sleep();
    			}
    			//when boat is empty
    			if (passengers == 0) {
    				passengers++;
    				//wake a partner to ride with
    				childWaitingOnOahu.wake();
    				//wait for partner to get on boat and row
    				childWaitingForPartner.sleep();
    				//leave Oahu and ride to Molokai
    				childrenOnOahu--;
    				bg.ChildRideToMolokai();
    			} else {
    				//fill empty pilot seat
    				passengers++;
    				//wake partner waiting for pilot
    				childWaitingForPartner.wake();
    				//leave Oahu and row to Molokai
    				childrenOnOahu--;
    				bg.ChildRowToMolokai();
    				//Pilot changes boat location to destination
    				boatLocation = MOLO;
    			}
    			oahu.release();
    			
    			//send 1 child to Oahu
    			molo.acquire();
    			//both are now on Molokai and off the boat so inc/dec necessary counters
    			childrenOnMolo++;
    			passengers--;
    			//always the case since both get off
    			if (passengers == 0) {
    				//1 child prepares to row back
    				passengers++;
    			} else {
    				//other child waits on Molokai to be woken up
    				childWaitingOnMolo.sleep();
    				//if woken up they'll be rowing back to Oahu - should not be needed in child loop but just in case
    				passengers++;
    			}
    			//initiate trip back to Oahu
    			childrenOnMolo--;
				bg.ChildRowToOahu();
				molo.release();
				
				oahu.acquire();
				//get off boat and increment island population
				passengers--;
				childrenOnOahu++;
				//change boat location to destination
				boatLocation = OAHU;
				oahu.release();
				//SYNC TESTING
				//System.out.println("CLoop: Oahu: Children: " + childrenOnOahu + " Adults: " + adultsOnOahu);
    			//System.out.println("CLoop: Molo: Children: " + childrenOnMolo + " Adults: " + adultsOnMolo);
    		}
    		//should always be left with 2 children on Oahu
    		oahu.acquire();
    		//these are the same steps as the child loop above without the return to Oahu
    		if (passengers == 0) {
				passengers++;
				childWaitingOnOahu.wake();
				childWaitingForPartner.sleep();
				childrenOnOahu--;
				bg.ChildRideToMolokai();
			} else {
				passengers++;
				childWaitingForPartner.wake();
				childrenOnOahu--;
				bg.ChildRowToMolokai();
				boatLocation = MOLO;
			}
			oahu.release();
			
			molo.acquire();
			childrenOnMolo++;
			passengers--;
			molo.release();
			//sim should be finished so mark it
    		finished = true;
    	}
    	//exit thread and let sim terminate
    	terminate.V();
    }

    static void SampleItinerary() {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
}
