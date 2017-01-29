# SimpleDB

Things implemented were as follows-

Setup buffer pool manager and page eviction mechanism (LRU and MRU):

Functions implemented were: 

1. getPage() in BufferPool.java
2. getNumEmptySlots(), setSlot() and getSlot() methods in HeapPage.java
3. getPage(), pinPage(), unpinPage(), and evictPage() methods in BufferPool.java

Implemented Simple-nested loop, Page-nested loop and Sort-Merge joins.

Files updated were: 

1. JoinPredicate
2. Join.java
