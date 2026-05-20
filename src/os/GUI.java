package os;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;

public class GUI extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final OS os;
	private JLabel clockLabel = new JLabel("Clock: 0");
	private JLabel runningLabel = new JLabel("Running: None");
	private JTextArea logArea = new JTextArea();
	private JTable memTable;
	private DefaultTableModel memModel;
	private JButton stepBtn = new JButton("Step");
	private JButton runBtn = new JButton("Run");
	private JButton pauseBtn = new JButton("Pause");
	private Timer timer;

	public GUI(OS os) {
		this.os = os;
		setTitle("OS Simulator");
		setSize(1300, 820);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		getContentPane().setBackground(Color.WHITE);
		setLayout(new BorderLayout(10, 10));

		add(buildHeader(), BorderLayout.NORTH);
		add(buildCenter(), BorderLayout.CENTER);
		add(buildBottom(), BorderLayout.SOUTH);

		timer = new Timer(700, e -> tick());
		refreshUI();
	}

	private JPanel buildHeader() {
		JPanel p = new JPanel(new GridLayout(1, 2, 10, 10));
		p.setBackground(Color.WHITE);
		p.setBorder(new EmptyBorder(10, 10, 0, 10));
		styleLabel(clockLabel, new Color(0, 102, 204));
		styleLabel(runningLabel, new Color(0, 153, 51));
		p.add(card(clockLabel));
		p.add(card(runningLabel));
		return p;
	}

	private JSplitPane buildCenter() {
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				buildLeft(), buildRight());
		split.setDividerLocation(650);
		return split;
	}

	private JPanel buildLeft() {
		JPanel p = new JPanel(new BorderLayout(10, 10));
		p.setBackground(Color.WHITE);
		p.setBorder(new EmptyBorder(10, 10, 10, 5));

		memModel = new DefaultTableModel(
				new Object[] { "Addr", "Key", "Value" }, Memory.SIZE);
		memTable = new JTable(memModel);
		memTable.setRowHeight(24);
		JScrollPane sp = new JScrollPane(memTable);
		p.add(titled(sp, "Memory"), BorderLayout.CENTER);
		return p;
	}

	private JPanel buildRight() {
		JPanel p = new JPanel(new BorderLayout(10, 10));
		p.setBackground(Color.WHITE);
		p.setBorder(new EmptyBorder(10, 5, 10, 10));

		logArea.setEditable(false);
		logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
		logArea.setBackground(new Color(245, 250, 255));
		JScrollPane sp = new JScrollPane(logArea);
		p.add(titled(sp, "Live Output"), BorderLayout.CENTER);
		return p;
	}

	private JPanel buildBottom() {
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(Color.WHITE);
		p.setBorder(new EmptyBorder(0, 10, 10, 10));

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
		buttons.setBackground(Color.WHITE);
		styleButton(stepBtn, new Color(0, 123, 255));
		styleButton(runBtn, new Color(40, 167, 69));
		styleButton(pauseBtn, new Color(255, 193, 7));
		buttons.add(stepBtn);
		buttons.add(runBtn);
		buttons.add(pauseBtn);

		stepBtn.addActionListener(e -> tick());
		runBtn.addActionListener(e -> timer.start());
		pauseBtn.addActionListener(e -> timer.stop());

		p.add(buttons, BorderLayout.CENTER);
		return p;
	}

	private JPanel titled(Component c, String title) {
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(Color.WHITE);
		p.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(new Color(0, 123, 255), 2),
				title));
		p.add(c);
		return p;
	}

	private JPanel card(JLabel label) {
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(new Color(240, 248, 255));
		p.setBorder(new CompoundBorder(new LineBorder(new Color(0, 123, 255),
				2, true), new EmptyBorder(15, 15, 15, 15)));
		p.add(label);
		return p;
	}

	private void styleLabel(JLabel l, Color c) {
		l.setForeground(c);
		l.setFont(new Font("SansSerif", Font.BOLD, 22));
	}

	private void styleButton(JButton b, Color c) {
		b.setBackground(c);
		b.setForeground(Color.WHITE);
		b.setFont(new Font("SansSerif", Font.BOLD, 16));
		b.setFocusPainted(false);
		b.setPreferredSize(new Dimension(130, 42));
	}

	private void tick() {
		os.step();

		logArea.append(Logger.drain());

		refreshUI();

		if (os.isDone()) {
			timer.stop();

			logArea.append("\n=== Simulation Complete at clock " + os.clock
					+ " ===\n");
		}
	}

	private void refreshUI() {
		clockLabel.setText("Clock: " + os.clock);
		runningLabel.setText(os.runningIdx >= 0 ? "Running: P"
				+ os.processes[os.runningIdx].pid : "Running: None");
		MemoryWord[] words = os.memory.getAll();
		for (int i = 0; i < Memory.SIZE; i++) {
			memModel.setValueAt(i, i, 0);
			memModel.setValueAt(words[i].inUse ? words[i].key : "", i, 1);
			memModel.setValueAt(words[i].inUse ? words[i].value : "", i, 2);
		}
	}
}
