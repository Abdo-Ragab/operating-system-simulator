# Operating System Simulator

A Java-based operating system simulator that models process management, scheduling, memory allocation, synchronization using mutexes, disk swapping, and program interpretation.

Developed for CSEN602 – Operating Systems at the German University in Cairo.

---

## Features

- Process creation and execution
- Custom interpreter for text-based programs
- Process Control Block (PCB) implementation
- Memory management and allocation
- Disk swapping simulation
- Mutual exclusion using semaphores/mutexes
- Round Robin (RR) scheduling
- Highest Response Ratio Next (HRRN) scheduling
- Multi-Level Feedback Queue (MLFQ) scheduling
- Ready and blocked queue management
- File I/O system calls
- GUI visualization of OS execution
- Clock-cycle based simulation

---

## System Architecture

The simulator models core operating system components including:

- Process Scheduler
- Memory Manager
- Process Control Blocks (PCB)
- Mutex/Semaphore Synchronization
- System Calls
- Program Interpreter
- Disk Swapping
- GUI Interface

---

## Scheduling Algorithms

### Round Robin (RR)

- Preemptive scheduling
- Configurable quantum
- Processes execute for a fixed number of instructions per time slice

### Highest Response Ratio Next (HRRN)

- Non-preemptive scheduling
- Response Ratio formula:

```text
(Waiting Time + Burst Time) / Burst Time
```

### Multi-Level Feedback Queue (MLFQ)

- Multiple priority queues
- Dynamic priority adjustment
- Different quantum lengths per queue level

---

## Memory Management

- Fixed-size memory consisting of 40 memory words
- Each process stores:
  - Instructions
  - Variables
  - PCB data
- Processes can be swapped to disk when memory becomes full
- Memory and disk contents are visualized during execution

---

## Mutexes and Synchronization

The simulator implements mutexes for:

- File access
- User input
- User output

Processes requesting busy resources are:
- Blocked
- Added to the blocked queue
- Resumed when the resource becomes available

---

## Supported Instructions

| Instruction | Description |
|---|---|
| `print` | Print to screen |
| `assign` | Assign variable value |
| `writeFile` | Write data to file |
| `readFile` | Read file contents |
| `printFromTo` | Print range of numbers |
| `semWait` | Acquire mutex |
| `semSignal` | Release mutex |

---

## Process States

Processes transition between:

- Ready
- Running
- Blocked
- Finished

The GUI visualizes state transitions in real time.

---

## GUI Features

The project includes a graphical interface that displays:

- Ready queue
- Blocked queue
- Running process
- Memory contents
- Disk swapping operations
- Clock cycle progression
- Process state changes

---

## Project Structure

```text
src/
└── OS/
    ├── GUI.java
    ├── Interpreter.java
    ├── Logger.java
    ├── Main.java
    ├── Memory.java
    ├── MemoryWord.java
    ├── Mutex.java
    ├── OS.java
    ├── Process.java
    ├── ProcessState.java
    ├── SchedAlgorithm.java
    └── Scheduler.java
```

---

## Running the Project

### Requirements

- Java 8 or later

### Compile

```bash
javac src/OS/*.java
```

### Run

```bash
java OS.Main
```

Or run using the provided:

```text
run.bat
```

---

## Example Programs

The simulator executes text-based programs such as:

### Program 1
- Prints numbers between two values

### Program 2
- Writes data to a file

### Program 3
- Reads and prints file contents

---

## Example Output

```text
Clock Cycle: 5
Running Process: P2
Instruction: semWait userOutput

Ready Queue: P1 P3
Blocked Queue: P2

Memory Updated
Process 1 swapped to disk
```

---

## Educational Concepts Demonstrated

This project demonstrates practical implementations of:

- CPU scheduling
- Concurrency
- Synchronization
- Process management
- Memory allocation
- Swapping
- Interpreters
- Operating system architecture

---

## Future Improvements

Potential future enhancements include:

- Virtual memory paging
- Deadlock detection
- Multi-core scheduling
- Priority inheritance
- Improved GUI visualization
- Interactive debugging tools
- Process statistics dashboard

---

## Technologies Used

- Java
- Java Swing (GUI)
- Object-Oriented Programming
- File I/O
- Data Structures & Algorithms

---

## License

This project is licensed under the MIT License.

---

## Authors

- Abdelrahman Ragab
- Aly Maher
- Mohamed Khalifa
- Hussein Sonbol
