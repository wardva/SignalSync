package be.signalsync.app;

public class Test {
	public static void main(String[] args) {
		double a = 1;
		for(int i = 0; i<2000; i++) {
			a *= 2;
			int b = (int) a;
			System.out.println(a + "\t" + b);
		}
	}
}
