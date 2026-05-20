package os;

import javax.swing.*;

public class Main {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			String[] algOptions = { "Round Robin (RR)",
					"Highest Response Ratio Next (HRRN)",
					"Multi-Level Feedback Queue (MLFQ)" };
			int algChoice = JOptionPane.showOptionDialog(null,
					"Choose a scheduling algorithm:", "OS Simulator",
					JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, algOptions, algOptions[0]);

			if (algChoice < 0)
				System.exit(0);

			SchedAlgorithm alg;
			switch (algChoice) {
			case 1:
				alg = SchedAlgorithm.HRRN;
				break;
			case 2:
				alg = SchedAlgorithm.MLFQ;
				break;
			default:
				alg = SchedAlgorithm.RR;
				break;
			}

			OS os = new OS(alg);
			GUI gui = new GUI(os);
			gui.setVisible(true);
		});
	}
}
