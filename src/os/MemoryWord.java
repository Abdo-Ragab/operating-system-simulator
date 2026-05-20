package os;

public class MemoryWord {
	public String key;
	public String value;
	public boolean inUse;

	public MemoryWord() {
		this.key = "";
		this.value = "";
		this.inUse = false;
	}

	public MemoryWord(String key, String value) {
		this.key = key;
		this.value = value;
		this.inUse = true;
	}
}