package os;

public class Memory {
	public static final int SIZE = 40;
	public static final int PCB_WORDS = 4;
	public static final int VAR_SLOTS = 3;
	public final MemoryWord[] words;

	public Memory() {
		words = new MemoryWord[SIZE];
		for (int i = 0; i < SIZE; i++)
			words[i] = new MemoryWord();
	}

	public int allocate(int n) {
		for (int i = 0; i + n <= SIZE; i++) {
			boolean free = true;
			for (int j = i; j < i + n; j++) {
				if (words[j].inUse) {
					free = false;
					break;
				}
			}
			if (!free) {
				continue;
			}
			for (int k = i; k < i + n; k++) {
				words[k].inUse = true;
			}
			return i;
		}
		return -1;
	}

	public void free(int start, int n) {
		for (int i = start; i < start + n && i < SIZE; i++) {
			words[i] = new MemoryWord();
		}
	}

	public void set(int addr, String key, String value) {
		words[addr] = new MemoryWord(key, value);
	}

	public MemoryWord get(int addr) {
		return words[addr];
	}

	public int find(int lo, int hi, String key) {
		for (int i = lo; i <= hi; i++) {
			if (words[i].inUse && words[i].key.equals(key))
				return i;
		}
		return -1;
	}

	public MemoryWord[] getAll() {
		return words;
	}

//	number of free words
	public int freeCount() {
		int count = 0;
		MemoryWord w = new MemoryWord();
		for (int i = 0; i < SIZE; i++) {
			w = words[i];
			if (!w.inUse)
				count++;
		}
		return count;
	}
}
