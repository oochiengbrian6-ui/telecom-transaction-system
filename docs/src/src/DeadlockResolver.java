package com.telecom.deadlock;

import java.util.*;
import java.util.concurrent.*;

/**
 * Deadlock Resolution using Wait-Die Scheme
 * Timestamp-based approach to prevent deadlocks in distributed system
 */
public class DeadlockResolver {
    
    private final ConcurrentHashMap<String, Transaction> activeTransactions;
    private final ConcurrentHashMap<String, ResourceLock> resourceLocks;
    private final Object lockMonitor = new Object();
    
    public DeadlockResolver() {
        this.activeTransactions = new ConcurrentHashMap<>();
        this.resourceLocks = new ConcurrentHashMap<>();
    }
    
    /**
     * Register a new transaction
     */
    public Transaction registerTransaction(String transactionId, String nodeId) {
        Transaction tx = new Transaction(transactionId, nodeId, System.currentTimeMillis());
        activeTransactions.put(transactionId, tx);
        System.out.println("[DeadlockResolver] Registered transaction: " + transactionId 
                          + " on node " + nodeId + " with timestamp " + tx.getTimestamp());
        return tx;
    }
    
    /**
     * Acquire lock on resource using Wait-Die scheme
     * @return true if lock acquired, false if transaction should abort
     */
    public boolean acquireLock(String transactionId, String resourceId) {
        Transaction requestingTx = activeTransactions.get(transactionId);
        if (requestingTx == null) {
            throw new IllegalStateException("Transaction not registered: " + transactionId);
        }
        
        synchronized (lockMonitor) {
            ResourceLock lock = resourceLocks.computeIfAbsent(resourceId, 
                k -> new ResourceLock(resourceId));
            
            // If resource is not locked, grant immediately
            if (!lock.isLocked()) {
                lock.acquire(requestingTx);
                System.out.println("[DeadlockResolver] Lock GRANTED: " + transactionId 
                                  + " acquired " + resourceId);
                return true;
            }
            
            // Resource is locked - apply Wait-Die scheme
            Transaction holdingTx = lock.getHolder();
            
            // Compare timestamps: older transactions have smaller timestamps
            if (requestingTx.getTimestamp() < holdingTx.getTimestamp()) {
                // Requesting transaction is OLDER - it can WAIT
                System.out.println("[DeadlockResolver] WAIT: " + transactionId 
                                  + " (ts=" + requestingTx.getTimestamp() + ") waiting for " 
                                  + resourceId + " held by " + holdingTx.getTransactionId() 
                                  + " (ts=" + holdingTx.getTimestamp() + ")");
                
                lock.addWaiter(requestingTx);
                return waitForLock(requestingTx, lock);
                
            } else {
                // Requesting transaction is YOUNGER - it must DIE (abort)
                System.out.println("[DeadlockResolver] DIE: " + transactionId 
                                  + " (ts=" + requestingTx.getTimestamp() + ") aborted - " 
                                  + resourceId + " held by older transaction " 
                                  + holdingTx.getTransactionId() 
                                  + " (ts=" + holdingTx.getTimestamp() + ")");
                
                requestingTx.abort();
                return false;
            }
        }
    }
    
    /**
     * Wait for lock with timeout
     */
    private boolean waitForLock(Transaction tx, ResourceLock lock) {
        try {
            // Wait for lock to be released (with timeout)
            synchronized (lock) {
                while (lock.isLocked() && !tx.isAborted()) {
                    lock.wait(5000); // 5 second timeout
                }
            }
            
            if (tx.isAborted()) {
                return false;
            }
            
            // Try to acquire lock again
            synchronized (lockMonitor) {
                if (!lock.isLocked()) {
                    lock.acquire(tx);
                    System.out.println("[DeadlockResolver] Lock ACQUIRED after wait: " 
                                      + tx.getTransactionId() + " -> " + lock.getResourceId());
                    return true;
                }
            }
            
            return false;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            tx.abort();
            return false;
        }
    }
    
    /**
     * Release lock on resource
     */
    public void releaseLock(String transactionId, String resourceId) {
        synchronized (lockMonitor) {
            ResourceLock lock = resourceLocks.get(resourceId);
            if (lock != null && lock.isHeldBy(transactionId)) {
                Transaction releasedBy = lock.getHolder();
                lock.release();
                
                System.out.println("[DeadlockResolver] Lock RELEASED: " + transactionId 
                                  + " released " + resourceId);
                
                // Notify waiting transactions
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        }
    }
    
    /**
     * Complete transaction and release all locks
     */
    public void completeTransaction(String transactionId) {
        Transaction tx = activeTransactions.remove(transactionId);
        if (tx != null) {
            // Release all locks held by this transaction
            synchronized (lockMonitor) {
                for (ResourceLock lock : resourceLocks.values()) {
                    if (lock.isHeldBy(transactionId)) {
                        lock.release();
                        synchronized (lock) {
                            lock.notifyAll();
                        }
                    }
                }
            }
            System.out.println("[DeadlockResolver] Transaction completed: " + transactionId);
        }
    }
    
    /**
     * Transaction class with timestamp
     */
    public static class Transaction {
        private final String transactionId;
        private final String nodeId;
        private final long timestamp;
        private volatile boolean aborted;
        
        public Transaction(String transactionId, String nodeId, long timestamp) {
            this.transactionId = transactionId;
            this.nodeId = nodeId;
            this.timestamp = timestamp;
            this.aborted = false;
        }
        
        public String getTransactionId() { return transactionId; }
        public String getNodeId() { return nodeId; }
        public long getTimestamp() { return timestamp; }
        public boolean isAborted() { return aborted; }
        public void abort() { this.aborted = true; }
    }
    
    /**
     * Resource lock class
     */
    private static class ResourceLock {
        private final String resourceId;
        private Transaction holder;
        private final Queue<Transaction> waiters;
        
        public ResourceLock(String resourceId) {
            this.resourceId = resourceId;
            this.waiters = new LinkedList<>();
        }
        
        public String getResourceId() { return resourceId; }
        public boolean isLocked() { return holder != null; }
        public Transaction getHolder() { return holder; }
        public boolean isHeldBy(String transactionId) {
            return holder != null && holder.getTransactionId().equals(transactionId);
        }
        
        public void acquire(Transaction tx) {
            this.holder = tx;
            waiters.remove(tx);
        }
        
        public void release() {
            this.holder = null;
        }
        
        public void addWaiter(Transaction tx) {
            waiters.add(tx);
        }
    }
            }
