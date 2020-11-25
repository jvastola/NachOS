package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;

// Added imports
// import java.util.LinkedList;
import java.util.HashMap;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */

	final int MAX_NAME_LENGTH = 256;
	final int fdStandardInput = 0;
	final int fdStandardOutput = 1;
	final int MAX_PROCESSES = 16;
	private OpenFile[] fdTable;

    public UserProcess(){
        int numPhysPages = Machine.processor().getNumPhysPages();
        pageTable = new TranslationEntry[numPhysPages];
        for(int i = 0; i < numPhysPages; i++)
            pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
        // Added variable declarations
        pid = pidTracker++; // Maybe problem?
        // children = new LinkedList<Integer>();
        children = new HashMap<Integer, UserProcess>();
        parent = null;
        fdTable = new OpenFile[MAX_PROCESSES];	//might need to be changed
        fdTable[fdStandardInput] = UserKernel.console.openForReading();
        fdTable[fdStandardOutput] = UserKernel.console.openForWriting();
    }
    
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess(){
        return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args){
        if (!load(name, args))
            return false;
        
        thread = new UThread(this);
        thread.setName(name).fork();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState(){
        
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState(){
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength){
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength+1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length=0; length<bytesRead; length++){
            if (bytes[length] == 0)
                return new String(bytes, 0, length);
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
        return readVirtualMemory(vaddr, data, 0, data.length);
    var

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length){
	    Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        int vpn = vaddr/pageSize;
        TranslationEntry te= pageTable[vpn];
        te.read = true; 
        // for now, just assume that virtual addresses equal physical addresses
        // will return true if our translation entry read only isn't valid
	    if (vaddr < 0 || vaddr >= memory.length || !te.valid)
	    return 0;
        // for now, just assume that virtual addresses equal physical addresses
        //if (vaddr < 0 || vaddr >= memory.length)
        //    return 0;

        int amount = Math.min(length, memory.length-vaddr);
        System.arraycopy(memory, vaddr, data, offset, amount);

        return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length){
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

        byte[] memory = Machine.processor().getMemory();
        
        int vpn = vaddr/pageSize;
        TranslationEntry te= pageTable[vpn];
        te.read = true;
        // for now, just assume that virtual addresses equal physical addresses
        // we have to make sure nothing is copied if the table entry is not valid and in read only
	    if (vaddr < 0 || vaddr >= memory.length || !te.valid || te.readOnly)
	    return 0;
        // for now, just assume that virtual addresses equal physical addresses
        //if (vaddr < 0 || vaddr >= memory.length)
        //    return 0;

        int amount = Math.min(length, memory.length-vaddr);
        System.arraycopy(data, offset, memory, vaddr, amount);

        return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args){
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
        
        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if(executable == null){
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        }
        catch (EOFException e){
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s=0; s<coff.getNumSections(); s++){
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages){
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i=0; i<args.length; i++){
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize){
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();	

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages*pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages-1)*pageSize;
        int stringOffset = entryOffset + args.length*4;

        this.argc = args.length;
        this.argv = entryOffset;
        
        for (int i=0; i<argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections(){
        if (numPages > Machine.processor().getNumPhysPages()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }

        // load sections
        for (int s=0; s<coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            
            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                  + " section (" + section.getLength() + " pages)");

            for (int i=0; i<section.getLength(); i++) {
                int vpn = section.getFirstVPN()+i;
                //int vpn = vaddr/pageSize;
                TranslationEntry te= pageTable[vpn];
                te.read = true;
                te.readOnly = secyion.isReadOnly();
                // for now, just assume virtual addresses=physical addresses
                section.loadPage(i, vpn);
            }
        }
        
        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i=0; i<processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }
    private int getFD() {
    	int i, flag = -1;
    	//start at 2 bc 0 and 1 are reserved
    	for (i = 2; i < MAX_PROCESSES; i++) {
    		if (fdTable[i] == null && flag == -1) {
    			flag = i;
    		}
    	}
    	return flag;
    }
    /**
     * Handle the halt() system call. 
     */
    private int handleHalt(){
        if(pid == 0){
            Machine.halt();
            
            Lib.assertNotReached("Machine.halt() did not halt machine!");
            return 0;
        }
        else{
            Lib.debug(dbgProcess, " - Halt was not executed on root");
            return -1;
        }
    }

    private int exec(int file, int argc, int argv){
        //return -1 if any of these are true

        if(argc < 0)
            return -1;
        
        String nameOfFile = readVirtualMemoryString(file, 256);
        if(nameOfFile == null)
            return -1;

        String suffix = nameOfFile.substring(nameOfFile.length() - 4, nameOfFile.length());
        if(suffix.equals(".coff"))
            return -1;
        
        //getting args from argv address
        String args[] = new String[argc];
        byte temp[] = new byte[4];

        for(int i = 0; i < argc; i++){
            
            int count = readVirtualMemory(argv + i*4, temp);

            if(count != 4)
                return -1;

            int addr = Lib.bytesToInt(temp, 0);
            args[i] = readVirtualMemoryString(addr, 256);
        }
        
        //new child process
        UserProcess child = UserProcess.newUserProcess();
        child.parent = this;
        this.children.put(child.pid, child);
        
        
        //load executable and create new user thread
        boolean checkValue = child.execute(nameOfFile, args);
        
        if(checkValue) 
            return child.pid;
        else
            return -1;
        
    }
    
    private int creat(String name) {
    	int flag = -1, fd;
    	//find available file descriptor
    	fd = getFD();
    	//returns -1 if no file descriptors are available
    	if (fd >= 2 && fd < MAX_PROCESSES) {
    		OpenFile file = ThreadedKernel.fileSystem.open(name, true);
    		//checks for invalid file name
    		if (file == null) {
    			//do nothing - will return -1
    		} else {
    			fdTable[fd] = file; 	//binds file descriptor to file
    			flag = fd;
    		}
    	} else {
    		//do nothing - will return -1
    	}
    	return flag;
    }
    
    private int open(String name) {
    	int flag = -1, fd;
    	//find available file descriptor
    	fd = getFD();
    	//returns -1 if no file descriptors are available
    	if (fd >= 2 && fd < MAX_PROCESSES) {
    		OpenFile file = ThreadedKernel.fileSystem.open(name, false);	//only difference from creat
    		//checks for invalid file name
    		if (file == null) {
    			//do nothing - will return -1
    		} else {
    			fdTable[fd] = file; 	//binds file descriptor to file
    			flag = fd;
    		}
    	} else {
    		//do nothing - will return -1
    	}
    	return flag;
    }

    private int read(int fd, int address, int count) {
		if(fd >= MAX_PROCESSES || fd < 0) {
			return -1;
		} 

		OpenFile file = fdTable[fd];
		if(file == null) {
			return -1;
		}

		byte[] toRead = new byte[count];
		
		int readCount = file.read(toRead, 0, count);
		
		if(readCount == -1) {
			return -1;
		}
		writeVirtualMemory(address, toRead);

		return readCount;
    }

    private void exit(int status) {
        //Closes open files that belong to the process
        for(int i = 0; i < MAX_PROCESSES; i++) {
            if(fdTable[i] != null) {
                close(i);
            }
        }
        
        //Any children of the processes don't have any parents process -> null
        for(int i : children.keySet())
            children.get(i).parent = null; // Cuts off head
        children.clear();
        
        //Changes process status to the user entered one for normal exiting or -1 for errors
        this.status = status;
        
        //Unload sections and release the memory pages
        this.unloadSections();
        
        if(pid == 0){
            //Only allows the root to terminate the machine
            Kernel.kernel.terminate();
        }
    }

    private int join(int pid, int statusAddr){
        if(statusAddr < 0) return 0;

        /*
        // Checks if pid process is child
        UserProcess pidProc = null;
        boolean isChild = false;
        for(int i = 0; i < children.size(); i++){
            if(children.get(i).contains(pid)){
                pidProc = children.get(i);
                isChild = true;
                break;
            }
        }
        if(!isChild) return -1;
        */

        // Checks if pid process is child
        UserProcess pidProc = children.get(pid);
        if(pidProc == null) return -1; // pid is not a child of current process

        pidProc.thread.join();

        // From here we assume the above thread.join() is done
        children.remove(pid);
        
        byte[] statusBytes = new byte[Integer.SIZE/8]; // Bytes of integer
        
        Lib.bytesFromInt(statusBytes, 0, pidProc.status); // Take status of child and translate to bytes for statusBytes
        int numBytes = writeVirtualMemory(statusAddr, statusBytes); // Write child status to statusAddr
        if(numBytes == Integer.SIZE/8)
            return 1; // Exited normally
        else
            return 0; // Exited with an unhandled exception
    }
    
    private int write(int fd, int address, int count) {
		if(fd >= MAX_PROCESSES || fd < 0) {
			return -1;
		}

		OpenFile file = fdTable[fd];
		if(file == null) {
			return -1;
		}

		byte[] buffer = new byte[count];
		int bufferWriteCount = readVirtualMemory(address, buffer);
		
		if(bufferWriteCount == -1 || bufferWriteCount != count) {
			return -1;
		}

		int writeCount = file.write(buffer, 0, count);

		if(writeCount != count) {
			return -1;
		} 
		
		return writeCount;
    }

    private int close(int fd) {
    	//check if fd is in bounds
    	if (fd < MAX_PROCESSES && fd >= 0 && fdTable[fd] != null) {
    		fdTable[fd].close();
    		fdTable[fd] = null;
    		return 0;
    	} else {
    		return -1;
    	}
    
    private int unlink(String name) {
    	boolean flag;
    	flag = ThreadedKernel.fileSystem.remove(name);
    	if (flag) {
    		return 0;
    	} else {
    		return -1;
    	}
    }
    
    private static final int
        syscallHalt = 0,
        syscallExit = 1,
        syscallExec = 2,
        syscallJoin = 3,
        syscallCreate = 4,
        syscallOpen = 5,
        syscallRead = 6,
        syscallWrite = 7,
        syscallClose = 8,
        syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
    	String name;
        switch (syscall) {
            case syscallHalt:
                return handleHalt();
            case syscallExec:
                return exec(a0, a1, a2);
            case syscallJoin:
                return join(a0, a1);
            case syscallExit:
                exit(a0);
                return 0;			//will most likely be replaced by a return
            case syscallCreate:
                name = readVirtualMemoryString(a0, MAX_NAME_LENGTH);
                //check for invalid file name
                if (name == null) {
                    return -1;
                } else {
                    return creat(name);
                }
                
            case syscallOpen:
                name = readVirtualMemoryString(a0, MAX_NAME_LENGTH);
                //check for invalid file name
                if (name == null) {
                    return -1;
                } else {
                    return open(name);
                }
            case syscallRead:
                return read(a0, a1, a2);	//unsure where to get buffer from
            case syscallWrite:
                return write(a0, a1, a2);	//unsure where to get buffer from
            case syscallClose:
                return close(a0);
            case syscallUnlink:
                name = readVirtualMemoryString(a0, MAX_NAME_LENGTH);
                //check for invalid file name
                //*****not checking if name is null might cause errors but think this makes sense logically
        //		if (name == null) {
        //			return -1;
        //		} else {
                    return unlink(name);
        //		}
            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                               processor.readRegister(Processor.regA0),
                               processor.readRegister(Processor.regA1),
                               processor.readRegister(Processor.regA2),
                               processor.readRegister(Processor.regA3)
                               );
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;				       
                               
            default:
                Lib.debug(dbgProcess, "Unexpected exception: " +
                      Processor.exceptionNames[cause]);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    // Added class variables
    private int pid;
    private int status;
    // private LinkedList<Integer> children;
    private HashMap<Integer, UserProcess> children;
    private UserProcess parent;
    private static int pidTracker = 0;
    private UThread thread;
}
