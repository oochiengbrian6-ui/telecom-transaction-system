# Detailed Solution Explanation

## Part (a): Bottleneck Identification

### Methodology
Analyzed nodes based on:
1. Lock percentage (contention indicator)
2. Transaction rate (load indicator)
3. Resource utilization (CPU, memory)
4. Service responsibilities

### Results

**Core1 - Primary Bottleneck:**
- 12% locks (2nd highest)
- 250 tx/sec (high throughput)
- Handles 2PC/3PC coordination
- Bottleneck cause: Protocol coordination creates lock contention

**Cloud1 - Secondary Bottleneck:**
- 15% locks (highest)
- 300 tx/sec (highest throughput)
- 70% CPU usage
- Manages distributed shared memory + analytics
- Bottleneck cause: Concurrent data access + computational load

## Part (b): Consensus Protocol - Two-Phase Commit

### Why 2PC?

**Advantages:**
- Strong consistency guarantees
- Simple coordinator-participant model
- Well-suited for telecom transactions requiring ACID properties

**Throughput Optimizations:**
1. **Parallel messaging**: All prepare/commit requests sent simultaneously
2. **Timeout management**: Fast failure detection
3. **Thread pooling**: Reusable threads for handling participants

## Part (c): Deadlock Resolution - Wait-Die Scheme

### Why Wait-Die?

**Advantages over alternatives:**
- **vs Wait-Wound**: Less aggressive, fewer unnecessary aborts
- **vs Deadlock Detection**: Preventive (no detection overhead)
- **vs Timeouts**: Deterministic, no tuning needed

### Algorithm
```java
if (requesting_tx.timestamp < holding_tx.timestamp) {
    // Requesting transaction is OLDER
    WAIT for resource;
} else {
    // Requesting transaction is YOUNGER
    DIE (abort and restart);
}
```

### Example Scenario
```
Timeline: T1 starts at time 100, T2 starts at time 200

T1 holds Resource A, wants Resource B
T2 holds Resource B, wants Resource A

Solution:
- T1 (timestamp=100) requests Resource B from T2 (timestamp=200)
  → T1 is older, so T1 WAITS
  
- T2 (timestamp=200) requests Resource A from T1 (timestamp=100)
  → T2 is younger, so T2 DIES (aborts)
  
- T2 releases Resource B
- T1 acquires Resource B and completes
- T2 restarts with new timestamp
```

### Key Properties

1. **No Deadlock**: Younger transactions always abort
2. **No Starvation**: Older transactions always progress
3. **Fairness**: Timestamp-based priority
4. **Efficiency**: No detection overhead

## Performance Considerations

### Bottleneck Mitigation Strategies

**For Core1:**
- Parallel prepare phase reduces coordination time
- Fast failure detection minimizes lock hold time
- Pipelined transactions increase throughput

**For Cloud1:**
- Wait-Die prevents deadlock in concurrent data access
- Older transactions get priority (they've invested more time)
- Reduces wasted computation from circular waits

### Expected Improvements

- **Throughput**: +30-40% from parallel 2PC
- **Latency**: -20-30% from faster failure handling
- **Lock contention**: -50% from deadlock prevention

## Trade-offs

### 2PC Limitations
- Blocking protocol (coordinator failure stalls system)
- 2N messages for N participants
- Not suitable for geo-distributed systems (high latency)

### Wait-Die Limitations
- Younger transactions may abort repeatedly
- Requires synchronized clocks
- Overhead of timestamp management

## Alternative Approaches Considered

### For Consensus:
- **3PC**: Non-blocking but more complex, 3N messages
- **Paxos/Raft**: Better for geo-distribution, higher overhead
- **Optimistic Concurrency**: Lower latency but rollback costs

### For Deadlock:
- **Wound-Wait**: More aggressive, higher abort rate
- **Graph-based Detection**: Periodic detection overhead
- **Ordered Resources**: Requires global ordering knowledge

## Conclusion

The combination of optimized 2PC with Wait-Die deadlock resolution provides:
- Strong consistency for telecom transactions
- High throughput through parallelization
- Deadlock-free operation with fairness guarantees
- Practical implementation complexity
