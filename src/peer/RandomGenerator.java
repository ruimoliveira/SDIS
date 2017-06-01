package peer;

import java.util.Random;

class RandomGenerator {
	private static Random rng = new Random();
	
	public static void waitUpTo(int miliseconds){
		try {
			Thread.sleep(rng.nextInt(miliseconds + 1));
		} catch (InterruptedException e) {}
	}
	
	public static double newFraction(){
		return Math.random();
	}
}
