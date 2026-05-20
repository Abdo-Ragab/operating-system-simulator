package os;

import java.util.*;

public class Mutex {
	public final String name;
	public boolean locked;
	public int holderPid;
	public final Queue<Integer> waitQueue;

	public Mutex(String name) {
		this.name = name;
		this.locked = false;
		this.holderPid = -1;
		this.waitQueue = new LinkedList<>();
	}

	public boolean tryAcquire(int pid) {
		if (!locked) {
			locked = true;
			holderPid = pid;
			return true;
		}
		if (!waitQueue.contains(pid))
			waitQueue.add(pid);
		return false;
	}

	public int release(int pid) {
		if (holderPid != pid)
			return -1;
		if (waitQueue.isEmpty()) {
			locked = false;
			holderPid = -1;
			return -1;
		}
		int next = waitQueue.poll();
		holderPid = next;
		return next;
	}

}
