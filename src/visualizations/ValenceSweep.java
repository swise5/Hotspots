package visualizations;

public class ValenceSweep {

	public static void main(String[] args) {

		for (int j = 0; j < 3; j++) {
			for (boolean stemmer : new Boolean[] { true, false })
				for (boolean negation : new Boolean[] { true, false }) {
					for (int i = 0; i < 3; i++) {
						ProcessValences pv = new ProcessValences(
								System.currentTimeMillis());
						pv.vm.use_negations = negation;
						pv.vm.use_stemmer = stemmer;

						boolean count = true;
						boolean freq = true;
						if (i == 0) { // count normalization
							count = true;
							freq = false;
						}

						if (i == 1) { // freq normalization
							count = false;
							freq = true;
						}

						if (i == 2) { // no normalization
							count = false;
							freq = false;
						}

						String lex = "";
						if (j == 0) {
							lex = pv.vm.afinnFile;
						}

						if (j == 1) {
							lex = pv.vm.redondo;
						}

						if (j == 2) {
							lex = pv.vm.sentiWordNetFile;
						}

						pv.vm = new ValenceMeasure(freq, count, stemmer,
								negation, lex);
						pv.start();
					}
				}
		}
	}
}