# Telecom Transaction & Concurrency System

Distributed transaction processing system with consensus protocol and deadlock resolution for telecom infrastructure.

## Assignment Solution

### (a) Bottleneck Analysis
**Identified nodes:** Core1 and Cloud1

- **Core1**: 12% locks, 250 tx/sec - coordination overhead
- **Cloud1**: 15% locks, 300 tx/sec - resource contention

See [detailed analysis](docs/analysis.md)

### (b) Consensus Protocol
**Implementation:** Two-Phase Commit (2PC)

Features:
- Parallel prepare/commit for throughput optimization
- Timeout-based failure handling
- Automatic rollback on failures

### (c) Deadlock Resolution
**Strategy:** Wait-Die Scheme (Timestamp-based)

Rules:
- Older transactions (smaller timestamp) → WAIT
- Younger transactions (larger timestamp) → DIE (abort)
- Prevents circular wait conditions

## Project Structure
```
├── docs/
│   └── analysis.md          # Bottleneck analysis
├── src/
│   ├── TwoPhaseCommit.java  # Consensus protocol
│   ├── DeadlockResolver.java # Deadlock resolution
│   └── TelecomSystemDemo.java # Demo application
└── README.md
```

## Running the Demo
```bash
# Compile
javac -d bin src/*.java src/**/*.java

# Run
java -cp bin com.telecom.TelecomSystemDemo
```

## Key Features

✅ Two-Phase Commit with parallel processing  
✅ Wait-Die deadlock prevention  
✅ Timeout-based failure detection  
✅ Automatic transaction rollback  
✅ Lock management with timestamp ordering  

## Author

Assignment solution for Telecom Transaction & Concurrency Dataset
