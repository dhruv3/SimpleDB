package simpledb;

import java.io.*;
import java.util.LinkedList;


/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool which check that the transaction has the appropriate
 * locks to read/write the page.
 */
public class BufferPool {
    /** Bytes per page, excluding header. */
    public static final int PAGE_SIZE = 4096;
    public static final int DEFAULT_PAGES = 100;
    public static final int DEFAULT_POLICY = 0;
    public static final int LRU_POLICY = 1;
    public static final int MRU_POLICY = 2;

    int replace_policy = DEFAULT_POLICY;

    int _numhits=0;
    int _nummisses=0;

    private int maxPages = 0;
    
    Page[] buffer;
    
    private LinkedList<PageId> mruList = new LinkedList<PageId>();
    
    private LinkedList<PageId> lruList = new LinkedList<PageId>();
    
    /**
     * Constructor.
     *
     * @param numPages number of pages in this buffer pool
     */
    public BufferPool(int numPages) {
        //IMPLEMENT THIS
    	this.maxPages = numPages;
    	this.buffer = new Page[numPages];
    }

  
    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, an page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public synchronized Page getPage(TransactionId tid, PageId pid, Permissions perm)
        throws TransactionAbortedException, DbException, IOException {
    	//IMPLEMENT THIS
    	int firstFreeIndex = getFirstFreeIndex() ;
    	
    	//check if page is present
    	for (int i = 0; i < (firstFreeIndex); i++) {
    		if(pid.equals(this.buffer[i].id())){
    			this._numhits++;
	            this.pinPage(i);
	            return this.buffer[i];
    		}
    	}
    	
    	//page is not present > (add) or (evict and add)
    	this._nummisses++;
    	//check if eviction required
    	int evictedPageIndex = 0;
    	if (firstFreeIndex >= maxPages){
    		evictedPageIndex = this.evictPage();
		}
		// add page
		DbFile myFile = Database.getCatalog().getDbFile(pid.tableid());
		Page newPage = myFile.readPage(pid);
		
		if(firstFreeIndex >= maxPages){
			this.buffer[evictedPageIndex] = newPage;
			this.pinPage(evictedPageIndex);
		}
		else{
			this.buffer[firstFreeIndex] = newPage;
			this.pinPage(firstFreeIndex);
		}
		return newPage;
	}

	private int getFirstFreeIndex() {
    	int i; 
    	for(i = 0; i < this.buffer.length; i++){
    		if(this.buffer[i] == null)
    			return i;
    	}
		return i;
	}


	/**
     * Pin page.
     * Increment pin_count. If the pin_count was 0 before the call, the page
     * was a candidate for replacement, but is no longer a candidate.  
     *
     * @param index the index of the page in the buffer pool
     */      
    public void pinPage(int index) {
    	//IMPLEMENT THIS
    	Page selPage = this.buffer[index];
    	
    	if(selPage.pin_count() == 0){
    		//the page was a candidate for replacement, but is no longer a candidate.     		
    	
    	}
    	
    	//Increment pin_count
    	selPage.pin();
    }
    
    /**
     * Unpin page.
     * Unpin the page in the buffer pool. Should be called with dirty==TRUE if 
     * the user has modified the page. If so, this call should set the dirty bit 
     * for this frame. Furthermore, if pin_count>0, should decrement it. If 
     * pin_count==0 before this call, throws DbException.
     *
     * @param index the index of the page in the buffer pool
     * @param tid the ID of the transaction unpinning the page
     * @param dirty the status of dirty_bit of the page (true or false)
     */          
    public void unpinPage(int index, TransactionId tid, boolean dirty) throws DbException, IOException {
    	//IMPLEMENT THIS
    	Page selPage = this.buffer[index];
    	if(dirty == true){ 
    		selPage.markDirty(dirty, tid);
    		flushPage(selPage.id());
		}

    	if(selPage.pin_count() == 0){
    		throw new DbException("(unpinPage) Error. We cannot unpin a page with pin_count equals to 0!");
    	}
    	else if(selPage.pin_count() > 0){
    		selPage.unpin();//decrease pin count
    	}
    	
    	if(selPage.pin_count() == 0){
    		lruList.add(selPage.id());
    		mruList.addFirst(selPage.id());
    	}
    	
//    	if the method unpinPage() is called with dirty_page == true, the method flushPage() should be called.
//		flushPage(selPage.id()); //this.buffer[index] - get page; id() - get page id    	
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public synchronized void releasePage(TransactionId tid, PageId pid) {
        // no need to implement this
       
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public synchronized void transactionComplete(TransactionId tid) throws IOException {
       // no need to implement this
     }

    /** Return true if the specified transaction has a lock on the specified page */
    public  synchronized boolean holdsLock(TransactionId tid, PageId p) {
       // no need to implement this
         return false;
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public  synchronized void transactionComplete(TransactionId tid, boolean commit)
        throws IOException {
       // no need to implement this
    }

    /**
     * Add a tuple to the specified table behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to. May block if
     * the lock cannot be acquired.
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public synchronized void insertTuple(TransactionId tid, int tableId, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
       // no need to implement this

    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is added to. May block if
     * the lock cannot be acquired.
     *
     * @param tid the transaction adding the tuple.
     * @param t the tuple to add
     */
    public synchronized void deleteTuple(TransactionId tid, Tuple t)
        throws DbException, TransactionAbortedException {
       // no need to implement this

    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
           // no need to implement this
    }

    /** Remove the specific page id from the buffer pool.
        Needed by the recovery manager to ensure that the
        buffer pool doesn't keep a rolled back page in its
        cache.
    */
    public synchronized void discardPage(PageId pid) {
     // no need to implement this
          }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private  synchronized void flushPage(PageId pid) throws IOException {
    	System.out.println("(flushPage()) Flushes page to disk");
    }

    /** Write all pages of the specified transaction to disk.
     */
    public synchronized void flushPages(TransactionId tid) throws IOException {
             // no need to implement this
    }

    /**
     * Discards a page from the buffer pool. Return index of discarded page
     */
    private synchronized int evictPage() throws DbException {
    	//IMPLEMENT THIS
    	if(this.replace_policy == LRU_POLICY){
            if(lruList.size() > 0){
            	PageId lruElem = lruList.getFirst();
                for (int i = 0; i < this.buffer.length; i++) {
            		if(lruElem.equals(this.buffer[i].id())){
            			lruList.remove(0);
            			return i;
            		}
            	}
            }
            else if(lruList.size() == 0)
            	throw new DbException("(getPage()) Buffer pool is full and list of LRU/MRU candidates is empty!");
    	}
    	else if(this.replace_policy == MRU_POLICY){
    		if(mruList.size() > 0){
            	PageId mruElem = mruList.getFirst();
                for (int i = 0; i < this.buffer.length; i++) {
            		if(mruElem.equals(this.buffer[i].id())){
            			mruList.remove(0);
            			return i;
            		}
            	}
            }
    		else if(mruList.size() == 0)
            	throw new DbException("(getPage()) Buffer pool is full and list of LRU/MRU candidates is empty!");
    	}
    	return 0;
    }
	
    public int getNumHits(){
	return _numhits;
    }
    public int getNumMisses(){
	return _nummisses;
    }
    
    public void setReplacePolicy(int replacement){
	this.replace_policy=replacement;
    }
    
    public int getBufferIndex(HeapPageId pid) {
    	for (int i=0;i<this.buffer.length;i++) {
    		if (this.buffer[i] == null) {
    			return -1;
    		}
    		if (pid.equals((this.buffer[i].id()))) {
    			return i;
    		}
    	}
    	return -1;
    }
    
    public void printBufferPool() {
    	for (int i = 0; i < this.buffer.length; i++) {
    		if (this.buffer[i] == null) {break; }
    		System.out.print(this.buffer[i].id() + ":" + this.buffer[i].pin_count() + " - ");
    	}
    	System.out.println();
    }
}
