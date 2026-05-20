package os;

import java.io.*;

public class OS {

	public static final String[] PROG_PATHS = { "Program_1.txt",
			"Program_2.txt", "Program_3.txt" };
	public static final int[] ARRIVAL_TIMES = { 0, 1, 4 };
	public static final int NUM_PROCESSES = 3;

	public final Memory memory;
	public final Process[] processes;
	public final Mutex[] mutexes;
	public final Scheduler scheduler;
	public final Interpreter interpreter;

	public int clock;
	public int runningIdx;
	public int instrThisSlice;
	public boolean[] created;

	public OS(SchedAlgorithm alg) {
		memory = new Memory();
		processes = new Process[NUM_PROCESSES];
		for (int i = 0; i < NUM_PROCESSES; i++)
			processes[i] = new Process(i + 1, ARRIVAL_TIMES[i], memory);

		mutexes = new Mutex[] { new Mutex("userInput"),
				new Mutex("userOutput"), new Mutex("file") };

		scheduler = new Scheduler(alg, processes);
		interpreter = new Interpreter(processes, mutexes, scheduler);
		created = new boolean[NUM_PROCESSES];
		clock = 0;
		runningIdx = -1;
		instrThisSlice = 0;
	}

	private void tryCreate(int i) {
		boolean ok = processes[i].load(PROG_PATHS[i]);

		if (!ok) {
			for (int j = 0; j < NUM_PROCESSES; j++) {
				if (j == i || j == runningIdx)
					continue;
				if (processes[j].memStart == -1)
					continue;
				if (processes[j].state == ProcessState.FINISHED)
					continue;

				Logger.log("[SWAP OUT] Evicting P" + processes[j].pid
						+ " to make room for P" + (i + 1));
				try {
					processes[j].swapOut();
				} catch (IOException e) {
					Logger.log("  ERROR swapping out P" + processes[j].pid
							+ ": " + e.getMessage());
				}
				ok = processes[i].load(PROG_PATHS[i]);
				if (ok)
					break;
			}
		}

		if (ok) {
			created[i] = true;
			Logger.log("[CREATED] P" + (i + 1) + " arrived at clock " + clock
					+ ", loaded into memory [" + processes[i].memStart + "-"
					+ processes[i].memEnd + "]");
			scheduler.addReady(i);
			printQueues();
		} else {
			Logger.log("[ERROR] Could not create P" + (i + 1)
					+ " - not enough memory");
		}
	}

	// simulate 1 cycle
	public boolean step() {
		Logger.log("\n=== Clock cycle " + clock + " ===");

		for (int i = 0; i < NUM_PROCESSES; i++) {
			if (!created[i] && processes[i].arrivalTime == clock) {
				Logger.log("[ARRIVAL] Process " + (i + 1) + " arriving");
				tryCreate(i);
			}
		}

		if (runningIdx == -1) {
			runningIdx = scheduler.next();

			if (runningIdx >= 0) {
				if (processes[runningIdx].memStart == -1) {
					Logger.log("[SWAP IN] P" + processes[runningIdx].pid
							+ " needs to be loaded from disk");
					for (int j = 0; j < NUM_PROCESSES; j++) {
						if (j == runningIdx)
							continue;
						if (processes[j].memStart == -1)
							continue;
						if (processes[j].state == ProcessState.FINISHED)
							continue;
						try {
							processes[j].swapOut();
						} catch (IOException e) {
							Logger.log("  ERROR: " + e.getMessage());
						}
						break;
					}
					try {
						boolean si = processes[runningIdx].swapIn();
						if (!si) {
							Logger.log("[ERROR] Cannot swap in P"
									+ processes[runningIdx].pid);
							scheduler.addReady(runningIdx);
							runningIdx = -1;
						}
					} catch (IOException e) {
						Logger.log("[ERROR] swapIn: " + e.getMessage());
						runningIdx = -1;
					}
				}

				if (runningIdx >= 0) {
					processes[runningIdx].state = ProcessState.RUNNING;
					processes[runningIdx].syncPCB();
					Logger.log("[RUNNING] P" + processes[runningIdx].pid
							+ " selected  (alg=" + scheduler.algorithm + ", q="
							+ scheduler.getQuantumFor(runningIdx) + ")");
					printQueues();
				}
			} else {
				if (!scheduler.blockedSet.isEmpty()) {
					Logger.log("[IDLE] all processes blocked");
				} else if (!isDone()) {
					Logger.log("[IDLE] waiting for process arrivals");
				}
			}
		}

		if (runningIdx >= 0) {
			int result = interpreter.execute(runningIdx);
			instrThisSlice++;

			if (result == Interpreter.EXEC_FINISHED) {
				Logger.log("[FINISHED] P" + processes[runningIdx].pid
						+ " completed all instructions at clock cycle" + clock);
				if (processes[runningIdx].memStart != -1) {
					int size = processes[runningIdx].memEnd
							- processes[runningIdx].memStart + 1;
					memory.free(processes[runningIdx].memStart, size);
					processes[runningIdx].memStart = -1;
				}
				runningIdx = -1;
				instrThisSlice = 0;
				printQueues();

			} else if (result == Interpreter.EXEC_BLOCKED) {
				Logger.log("[BLOCKED] P" + processes[runningIdx].pid
						+ " added to blocked queue");
				scheduler.addBlocked(runningIdx);
				runningIdx = -1;
				instrThisSlice = 0;
				printQueues();

			} else {
				SchedAlgorithm alg = scheduler.algorithm;
				if (alg == SchedAlgorithm.RR || alg == SchedAlgorithm.MLFQ) {
					int q = scheduler.getQuantumFor(runningIdx);
					if (instrThisSlice >= q) {
						if (alg == SchedAlgorithm.MLFQ)
							scheduler.demoteMLFQ(runningIdx);
						processes[runningIdx].state = ProcessState.READY;
						processes[runningIdx].syncPCB();
						scheduler.addReady(runningIdx);
						Logger.log("[PREEMPT] P" + processes[runningIdx].pid
								+ " quantum expired (q=" + q
								+ ") -> back to Ready Queue");
						runningIdx = -1;
						instrThisSlice = 0;
						printQueues();
					}
				}
			}
		}

		scheduler.incrementWaiting();
		printMemory();
		clock++;
		return !isDone();
	}

	public boolean isDone() {
		for (int i = 0; i < NUM_PROCESSES; i++) {
			if (!created[i])
				return false;
		}
		for (Process p : processes) {
			if (p.state != ProcessState.FINISHED)
				return false;
		}
		return true;
	}

	public void printQueues() {
		StringBuilder sb = new StringBuilder("  Queues -> Ready: [");
		for (int idx : scheduler.readyQueue)
			sb.append("P").append(processes[idx].pid).append(" ");
		sb.append("] | Blocked: [");
		for (int idx : scheduler.blockedSet)
			sb.append("P").append(processes[idx].pid).append(" ");
		sb.append("]");
		Logger.log(sb.toString());

		for (Mutex m : mutexes) {
			if (!m.waitQueue.isEmpty()) {
				StringBuilder ms = new StringBuilder("  Mutex[" + m.name
						+ "] waiting: [");
				for (int pid : m.waitQueue)
					ms.append("P").append(pid).append(" ");
				ms.append("]");
				Logger.log(ms.toString());
			}
		}
	}

//	prints memory content every clock cycle
	public void printMemory() {
		Logger.log("  Memory:");
		MemoryWord[] words = memory.getAll();
		for (int i = 0; i < Memory.SIZE; i++) {
			MemoryWord w = words[i];
			if (w.inUse)
				Logger.log("    [" + i + "] " + w.key + " = " + w.value);
			else
				Logger.log("    [" + i + "] (free)");
		}
	}
}
