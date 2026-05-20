package os;

import java.io.*;

import javax.swing.JOptionPane;

public class Interpreter {

	public static final int EXEC_OK = 0;
	public static final int EXEC_BLOCKED = 1;
	public static final int EXEC_FINISHED = 2;
	public static final int EXEC_ERROR = -1;

	public final Process[] processes;
	public final Mutex[] mutexes;
	public final Scheduler scheduler;

	public Interpreter(Process[] processes, Mutex[] mutexes, Scheduler scheduler) {
		this.processes = processes;
		this.mutexes = mutexes;
		this.scheduler = scheduler;
	}

	public Mutex getMutex(String name) {
		for (Mutex m : mutexes) {
			if (m.name.equals(name))
				return m;
		}
		return null;
	}

	public int idxByPid(int pid) {
		for (int i = 0; i < processes.length; i++) {
			if (processes[i].pid == pid)
				return i;
		}
		return -1;
	}

	public String resolve(Process proc, String name) {
		String v = proc.getVariable(name);
		if (v != null)
			return v;
		return name;
	}

	public int execute(int procIdx) {
		Process proc = processes[procIdx];
		if (proc.memStart == -1)
			return EXEC_ERROR;
		if (proc.pc >= proc.instrCount)
			return EXEC_FINISHED;

		String instr = proc.getInstruction(proc.pc);

		if (instr == null || instr.trim().isEmpty()) {
			Logger.log("     ERROR: Missing instruction at PC=" + proc.pc);
			return EXEC_ERROR;
		}

		String[] parts = instr.split("\\s+", 3);
		String command = parts[0];

		Logger.log("  [P" + proc.pid + " | PC=" + proc.pc + "] >> " + instr);

		switch (command) {

		case "assign": {
			String varName = parts[1];
			String expr = "";
			if (parts.length > 2)
				expr = parts[2];
			String val;

			if (expr.equals("input")) {
				val = JOptionPane.showInputDialog(null,
						"Please enter a value for '" + varName + "':",
						"User Input", JOptionPane.QUESTION_MESSAGE);

				if (val == null)
					val = "";

				val = val.trim();
				Logger.log("     User input -> " + varName + " = \"" + val
						+ "\"");
			} else if (expr.startsWith("readFile ")) {
				String fileArg = expr.substring(9).trim();
				String filename = resolve(proc, fileArg);
				val = readFile(filename);
				Logger.log("     readFile(\"" + filename + "\") -> " + val);
			} else {
				val = resolve(proc, expr);
				Logger.log("     assign " + varName + " = \"" + val + "\"");
			}

			proc.setVariable(varName, val);
			break;
		}

		case "print": {
			if (parts.length < 2) {
				Logger.log("     ERROR: argument to print is missing");
				break;
			}
			String val = resolve(proc, parts[1]);
			Logger.log("     OUTPUT: " + val);
			break;
		}

		case "writeFile": {
			String filename = resolve(proc, parts[1]);
			String data = resolve(proc, parts[2]);
			writeFile(filename, data);
			Logger.log("     writeFile(\"" + filename + "\", \"" + data
					+ "\") done");
			break;
		}

		case "readFile": {
			// readFile only is removed
			String filename = resolve(proc, parts[1]);
			String content = readFile(filename);
			Logger.log("     readFile(\"" + filename + "\") = " + content);
			break;
		}

		case "printFromTo": {
			int from, to;
			try {
				from = Integer.parseInt(resolve(proc, parts[1]));
				to = Integer.parseInt(resolve(proc, parts[2]));
			} catch (NumberFormatException e) {
				Logger.log("     ERROR: printFromTo requires integer arguments");
				break;
			}
			StringBuilder sb = new StringBuilder("     OUTPUT: ");
			for (int i = from; i <= to; i++)
				sb.append(i).append(" ");
			Logger.log(sb.toString().trim());
			break;
		}

		case "semWait": {
			String resource = parts[1];
			Mutex mutex = getMutex(resource);
			if (mutex == null) {
				Logger.log("     ERROR: unknown mutex \"" + resource + "\"");
				break;
			}
			boolean acquired = mutex.tryAcquire(proc.pid);
			if (acquired) {
				Logger.log("     semWait(" + resource + ") -> ACQUIRED by P"
						+ proc.pid);
			} else {
				Logger.log("     semWait(" + resource + ") -> BLOCKED P"
						+ proc.pid + " (held by P" + mutex.holderPid + ")");
				proc.state = ProcessState.BLOCKED;
				proc.syncPCB();
				return EXEC_BLOCKED;
			}
			break;
		}

		case "semSignal": {
			String resource = parts[1];
			Mutex mutex = getMutex(resource);
			if (mutex == null) {
				Logger.log("     ERROR: unknown mutex \"" + resource + "\"");
				break;
			}
			int unblocked = mutex.release(proc.pid);
			Logger.log("     semSignal(" + resource + ") -> released by P"
					+ proc.pid);
			if (unblocked != -1) {
				int ubIdx = idxByPid(unblocked);
				if (ubIdx >= 0) {
					processes[ubIdx].pc++;
					processes[ubIdx].state = ProcessState.READY;
					processes[ubIdx].syncPCB();
					scheduler.addReady(ubIdx);
					Logger.log("     P" + unblocked
							+ " unblocked -> Ready Queue (PC advanced to "
							+ processes[ubIdx].pc + ")");
				}
			}
			break;
		}

		default:
			Logger.log("     ERROR: unknown instruction \"" + command + "\"");
			break;
		}

		proc.pc++;
		proc.syncPCB();

		if (proc.pc >= proc.instrCount) {
			proc.state = ProcessState.FINISHED;
			proc.syncPCB();
			return EXEC_FINISHED;
		}

		return EXEC_OK;
	}

	public String readFile(String filename) {
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
			return sb.toString().trim();
		} catch (IOException e) {
			return "ERROR: cannot read \"" + filename + "\"";
		}
	}

	public void writeFile(String filename, String data) {
		try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
			pw.println(data);
		} catch (IOException e) {
			Logger.log("     ERROR: cannot write \"" + filename + "\": "
					+ e.getMessage());
		}
	}
}
