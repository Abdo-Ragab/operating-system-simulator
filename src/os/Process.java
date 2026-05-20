package os;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import os.ProcessState;
import os.Logger;
import os.Memory;
import os.MemoryWord;

public class Process {

	public int pid;
	public ProcessState state;
	public int pc;
	public int memStart;
	public int memEnd;
	public int instrCount;
	public int arrivalTime;
	public int waitingTime;
	public String diskPath;

	public final Memory memory;

	public Process(int pid, int arrivalTime, Memory memory) {
		this.pid = pid;
		this.arrivalTime = arrivalTime;
		this.memory = memory;
		this.state = ProcessState.NEW;
		this.pc = 0;
		this.waitingTime = 0;
		this.memStart = -1;
		this.memEnd = -1;
		this.diskPath = "swap_p" + pid + ".dat";
	}

//	load program to memory
	public boolean load(String programPath) {
		List<String> instructions = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(programPath))) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty())
					instructions.add(line);
			}
		} catch (IOException e) {
			Logger.log("  ERROR: Cannot open " + programPath + ": "
					+ e.getMessage());
			return false;
		}

		instrCount = instructions.size();
		int size = Memory.PCB_WORDS + instrCount + Memory.VAR_SLOTS;

		int start = memory.allocate(size);
		if (start == -1) {
			return false;
		}

		memStart = start;
		memEnd = start + size - 1;
		state = ProcessState.READY;

		memory.set(start, "P" + pid + ":pid", String.valueOf(pid));
		memory.set(start + 1, "P" + pid + ":state", "READY");
		memory.set(start + 2, "P" + pid + ":pc", "0");
		memory.set(start + 3, "P" + pid + ":bounds", start + "-" + memEnd);

		for (int i = 0; i < instrCount; i++) {
			memory.set(start + Memory.PCB_WORDS + i, "P" + pid + ":i" + i,
					instructions.get(i));
		}

		for (int i = 0; i < Memory.VAR_SLOTS; i++) {
			memory.set(start + Memory.PCB_WORDS + instrCount + i, "P" + pid
					+ ":v" + i, "");
		}

		return true;
	}

//	update Process Control Block
	public void syncPCB() {
		if (memStart == -1)
			return;
		int a = memory.find(memStart, memEnd, "P" + pid + ":state");
		if (a >= 0) {
			memory.set(a, "P" + pid + ":state", state.name());
		}
		a = memory.find(memStart, memEnd, "P" + pid + ":pc");
		if (a >= 0) {
			memory.set(a, "P" + pid + ":pc", String.valueOf(pc));
		}
	}

	public String getInstruction(int i) {
		if ((memStart == -1) || i >= instrCount)
			return null;
		int a = memory.find(memStart, memEnd, "P" + pid + ":i" + i);
		if (a >= 0) {
			return memory.get(a).value;
		} else {
			return null;
		}
	}

	public String getVariable(String name) {
		if (memStart == -1)
			return null;
		int variables = memStart + Memory.PCB_WORDS + instrCount;
		for (int i = 0; i < Memory.VAR_SLOTS; i++) {
			MemoryWord w = memory.get(variables + i);
			if (w.inUse && w.key.equals(name))
				return w.value;
		}
		return null;
	}

	public boolean setVariable(String name, String value) {
		if (memStart == -1)
			return false;
		int varBase = memStart + Memory.PCB_WORDS + instrCount;
		int freeSlot = -1;
		for (int i = 0; i < Memory.VAR_SLOTS; i++) {
			MemoryWord w = memory.get(varBase + i);
			if (w.key.equals(name)) {
				memory.set(varBase + i, name, value);
				return true;
			}
			if (freeSlot < 0 && w.key.startsWith("P" + pid + ":v")
					&& w.value.equals("")) {
				freeSlot = varBase + i;
			}
		}
		if (freeSlot >= 0) {
			memory.set(freeSlot, name, value);
			return true;
		}
		Logger.log("  ERROR: No free variable slots for P" + pid);
		return false;
	}

//	sheel el process men el memory w create file feh el current process state
	public void swapOut() throws IOException {
		if (memStart == -1)
			return;
		try (PrintWriter pw = new PrintWriter(new FileWriter(diskPath))) {
			pw.println(instrCount);
			for (int i = memStart; i <= memEnd; i++) {
				MemoryWord w = memory.get(i);
				pw.println((w.inUse ? w.key : "") + "|"
						+ (w.inUse ? w.value : ""));
			}
		}
		int size = memEnd - memStart + 1;
		memory.free(memStart, size);
		Logger.log("  [SWAP OUT] P" + pid + " -> disk (" + diskPath
				+ "), freed words " + memStart + "-" + memEnd);
		memStart = -1;
		memEnd = -1;
	}

	// raga3 el process lel memory
	public boolean swapIn() throws IOException {
		if (memStart != -1)
			return true;

		int size = Memory.PCB_WORDS + instrCount + Memory.VAR_SLOTS;
		int newStart = memory.allocate(size);

		if (newStart < 0)
			return false;

		try (BufferedReader br = new BufferedReader(new FileReader(diskPath))) {
			String line = br.readLine();
			if (line != null) {
				try {
					instrCount = Integer.parseInt(line.trim());
				} catch (NumberFormatException ignored) {
				}
			}
			int addr = newStart;
			while ((line = br.readLine()) != null
					&& addr <= newStart + size - 1) {
				int pipe = line.indexOf('|');
				String key;
				String val;

				if (pipe >= 0) {
					key = line.substring(0, pipe);
					val = line.substring(pipe + 1);
				} else {
					key = line;
					val = "";
				}
				if (!key.isEmpty())
					memory.set(addr, key, val);
				addr++;
			}
		}

		memStart = newStart;
		memEnd = newStart + size - 1;

		int bAddr = memory.find(memStart, memEnd, "P" + pid + ":bounds");
		if (bAddr >= 0)
			memory.set(bAddr, "P" + pid + ":bounds", memStart + "-" + memEnd);

		int pcAddr = memory.find(memStart, memEnd, "P" + pid + ":pc");
		if (pcAddr >= 0)
			memory.set(pcAddr, "P" + pid + ":pc", String.valueOf(pc));
		int stateAddr = memory.find(memStart, memEnd, "P" + pid + ":state");
		if (stateAddr >= 0)
			memory.set(stateAddr, "P" + pid + ":state", state.name());

		Logger.log("  [SWAP IN]  P" + pid + " <- disk, loaded at words "
				+ memStart + "-" + memEnd);
		return true;
	}
}
