package os;

import java.util.*;

public class Scheduler {

	public static final int DEFAULT_QUANTUM = 2;
	public static final int MLFQ_LEVELS = 4;

	public final SchedAlgorithm algorithm;
	public final Process[] processes;
	public final LinkedList<Integer> readyQueue;
	public final LinkedList<Integer> blockedSet;

	@SuppressWarnings("unchecked")
	public final LinkedList<Integer>[] mlfqQueues = (LinkedList<Integer>[]) new LinkedList[MLFQ_LEVELS];
	public final int[] procLevel;

	public Scheduler(SchedAlgorithm alg, Process[] processes) {
		this.algorithm = alg;
		this.processes = processes;
		this.readyQueue = new LinkedList<>();
		this.blockedSet = new LinkedList<>();
		for (int i = 0; i < MLFQ_LEVELS; i++) {
			mlfqQueues[i] = new LinkedList<>();
		}
		this.procLevel = new int[processes.length];
	}

	public void addReady(int idx) {
		blockedSet.remove(Integer.valueOf(idx));
		if (!readyQueue.contains(idx))
			readyQueue.addLast(idx);
		if (algorithm == SchedAlgorithm.MLFQ) {
			int lvl = procLevel[idx];
			if (!mlfqQueues[lvl].contains(idx)) {
				mlfqQueues[lvl].addLast(idx);
			}
		}
	}

	public void addBlocked(int idx) {
		readyQueue.remove(Integer.valueOf(idx));
		if (algorithm == SchedAlgorithm.MLFQ)
			for (LinkedList<Integer> q : mlfqQueues) {
				q.remove(Integer.valueOf(idx));
			}
		if (!blockedSet.contains(idx)) {
			blockedSet.add(idx);
		}
	}

	public int next() {

		if (algorithm == SchedAlgorithm.RR) {
			return nextRR();
		} else if (algorithm == SchedAlgorithm.HRRN) {
			return nextHRRN();
		} else if (algorithm == SchedAlgorithm.MLFQ) {
			return nextMLFQ();
		} else {
			return -1;
		}
	}

	public int nextRR() {
		if (readyQueue.isEmpty()) {
			return -1;
		}
		return readyQueue.pollFirst();
	}

	public int nextHRRN() {
		if (readyQueue.isEmpty())
			return -1;
		double best = -1;
		int bestIdx = -1;
		for (int idx : readyQueue) {
			Process p = processes[idx];
			int remaining = Math.max(1, p.instrCount - p.pc);
			double ratio = (p.waitingTime + remaining) / (double) remaining;
			if (ratio > best) {
				best = ratio;
				bestIdx = idx;
			}
		}
		readyQueue.remove(Integer.valueOf(bestIdx));
		return bestIdx;
	}

	public int nextMLFQ() {
		for (int lvl = 0; lvl < MLFQ_LEVELS; lvl++) {
			if (!mlfqQueues[lvl].isEmpty()) {
				int idx = mlfqQueues[lvl].pollFirst();
				readyQueue.remove(Integer.valueOf(idx));
				return idx;
			}
		}
		return -1;
	}

	public void demoteMLFQ(int idx) {
		if (procLevel[idx] < MLFQ_LEVELS - 1) {
			procLevel[idx]++;
		}
	}

	public int getQuantumFor(int idx) {
		if (algorithm == SchedAlgorithm.MLFQ) {
			return (int) Math.pow(2, procLevel[idx]);
		}
		return DEFAULT_QUANTUM;
	}

	public void incrementWaiting() {
		for (int idx : readyQueue)
			processes[idx].waitingTime++;
	}

	public boolean isEmpty() {
		return readyQueue.isEmpty() && blockedSet.isEmpty();
	}
}
