package be.signalsync.app;

import be.tarsos.dsp.util.PitchConverter;

public class Converter {
	public static void main(final String[] args) {
		final double c1 = 6700;
		final double c2 = 7500;
		final double hz1 = PitchConverter.absoluteCentToHertz(c1);
		final double hz2 = PitchConverter.absoluteCentToHertz(c2);
		System.out.printf("Frequentie: van %f tot %f", hz1, hz2);
	}
}
