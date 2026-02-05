# Telecom Transaction & Concurrency Analysis

## Dataset Overview

| Node   | CPU (%) | Memory (GB) | Latency (ms) | Transactions/sec | Locks (%) | Services                           |
|--------|---------|-------------|--------------|------------------|-----------|-------------------------------------|
| Edge1  | 45      | 4           | 12           | 120              | 5         | RPC, EventOrdering                 |
| Edge2  | 50      | 4.5         | 15           | 100              | 8         | RPC, NodeFailureRecovery           |
| Core1  | 60      | 8           | 8            | 250              | 12        | 2PC/3PC, TransactionCommit         |
| Core2  | 55      | 7.5         | 10           | 230              | 10        | DeadlockDetection, LoadBalancing   |
| Cloud1 | 70      | 16          | 20           | 300              | 15        | DistributedSharedMemory, Analytics |

## (a) Nodes Causing Transaction Bottlenecks

### Identified Bottleneck Nodes:

**1. Core1**
- **Lock percentage**: 12% (highest among core processing nodes)
- **Transaction rate**: 250 tx/sec (very high)
- **Services**: Handles 2PC/3PC and TransactionCommit
- **Impact**: High contention due to coordination overhead

**2. Cloud1**
- **Lock percentage**: 15% (highest overall)
- **Transaction rate**: 300 tx/sec (highest in system)
- **CPU usage**: 70% (highest)
- **Services**: Manages DistributedSharedMemory and Analytics
- **Impact**: Resource contention from concurrent data access and heavy computational load

### Analysis:
The combination of high lock percentages with high transaction rates indicates these nodes are experiencing significant contention, causing delays in transaction processing and reducing overall system throughput.
