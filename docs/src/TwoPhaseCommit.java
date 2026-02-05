package com.telecom.consensus;

import java.util.*;
import java.util.concurrent.*;

/**
 * Two-Phase Commit Protocol Implementation
 * Optimized for maximizing throughput in distributed telecom transactions
 */
public class TwoPhaseCommit {
    
    private final String coordinatorId;
    private final ExecutorService executor;
    private final int timeout;
    
    public TwoPhaseCommit(String coordinatorId, int timeoutMs) {
        this.coordinatorId = coordinatorId;
        this.timeout = timeoutMs;
        this.executor = Executors.newCachedThreadPool();
    }
    
    /**
     * Execute 2PC protocol for a transaction
     * @param transactionId Unique transaction identifier
     * @param participants List of participating nodes
     * @param transactionData Data to be committed
     * @return true if commit successful, false otherwise
     */
    public boolean executeTransaction(String transactionId, 
                                     List<Participant> participants, 
                                     TransactionData transactionData) {
        
        System.out.println("[2PC-" + transactionId + "] Starting transaction with " 
                          + participants.size() + " participants");
        
        // PHASE 1: PREPARE
        if (!preparePhase(transactionId, participants, transactionData)) {
            rollback(transactionId, participants);
            return false;
        }
        
        // PHASE 2: COMMIT
        return commitPhase(transactionId, participants);
    }
    
    /**
     * Phase 1: Send PREPARE request to all participants
     */
    private boolean preparePhase(String transactionId, 
                                 List<Participant> participants, 
                                 TransactionData data) {
        
        System.out.println("[2PC-" + transactionId + "] PHASE 1: PREPARE");
        
        List<Future<Boolean>> futures = new ArrayList<>();
        
        // Send prepare requests in parallel for throughput optimization
        for (Participant participant : participants) {
            Future<Boolean> future = executor.submit(() -> {
                try {
                    return participant.prepare(transactionId, data);
                } catch (Exception e) {
                    System.err.println("[2PC-" + transactionId + "] Prepare failed for " 
                                      + participant.getNodeId() + ": " + e.getMessage());
                    return false;
                }
            });
            futures.add(future);
        }
        
        // Wait for all responses with timeout
        try {
            for (Future<Boolean> future : futures) {
                Boolean vote = future.get(timeout, TimeUnit.MILLISECONDS);
                if (vote == null || !vote) {
                    System.out.println("[2PC-" + transactionId + "] ABORT: Participant voted NO");
                    return false;
                }
            }
            System.out.println("[2PC-" + transactionId + "] All participants voted YES");
            return true;
            
        } catch (TimeoutException e) {
            System.err.println("[2PC-" + transactionId + "] ABORT: Prepare timeout");
            return false;
        } catch (Exception e) {
            System.err.println("[2PC-" + transactionId + "] ABORT: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Phase 2: Send COMMIT request to all participants
     */
    private boolean commitPhase(String transactionId, List<Participant> participants) {
        
        System.out.println("[2PC-" + transactionId + "] PHASE 2: COMMIT");
        
        List<Future<Boolean>> futures = new ArrayList<>();
        
        // Send commit requests in parallel
        for (Participant participant : participants) {
            Future<Boolean> future = executor.submit(() -> {
                try {
                    return participant.commit(transactionId);
                } catch (Exception e) {
                    System.err.println("[2PC-" + transactionId + "] Commit failed for " 
                                      + participant.getNodeId() + ": " + e.getMessage());
                    return false;
                }
            });
            futures.add(future);
        }
        
        // Wait for all commits to complete
        boolean allCommitted = true;
        try {
            for (Future<Boolean> future : futures) {
                if (!future.get(timeout, TimeUnit.MILLISECONDS)) {
                    allCommitted = false;
                }
            }
        } catch (Exception e) {
            System.err.println("[2PC-" + transactionId + "] Commit phase error: " + e.getMessage());
            allCommitted = false;
        }
        
        if (allCommitted) {
            System.out.println("[2PC-" + transactionId + "] Transaction COMMITTED successfully");
        } else {
            System.err.println("[2PC-" + transactionId + "] Transaction commit incomplete");
        }
        
        return allCommitted;
    }
    
    /**
     * Rollback transaction on all participants
     */
    private void rollback(String transactionId, List<Participant> participants) {
        System.out.println("[2PC-" + transactionId + "] ROLLBACK initiated");
        
        for (Participant participant : participants) {
            executor.submit(() -> {
                try {
                    participant.rollback(transactionId);
                } catch (Exception e) {
                    System.err.println("[2PC-" + transactionId + "] Rollback failed for " 
                                      + participant.getNodeId());
                }
            });
        }
    }
    
    public void shutdown() {
        executor.shutdown();
    }
    
    /**
     * Participant interface for 2PC protocol
     */
    public interface Participant {
        String getNodeId();
        boolean prepare(String transactionId, TransactionData data) throws Exception;
        boolean commit(String transactionId) throws Exception;
        void rollback(String transactionId) throws Exception;
    }
    
    /**
     * Transaction data holder
     */
    public static class TransactionData {
        private final Map<String, Object> data;
        
        public TransactionData() {
            this.data = new ConcurrentHashMap<>();
        }
        
        public void put(String key, Object value) {
            data.put(key, value);
        }
        
        public Object get(String key) {
            return data.get(key);
        }
        
        public Map<String, Object> getData() {
            return new HashMap<>(data);
        }
    }
                           }
