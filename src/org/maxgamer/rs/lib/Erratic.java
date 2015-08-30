package org.maxgamer.rs.lib;

import java.util.Random;

/**
 * @author netherfoam
 */
public class Erratic {
	private static Random r = new Random();
	
	public static int nextInt(int min, int max) {
		return r.nextInt(max - min + 1) + min;
	}
	
	public static boolean nextBoolean() {
		return r.nextBoolean();
	}
	
	/**
	 * Gets the next boolean but with a bias chance. If the given value is 1,
	 * this will always return true. If the given value is 2, this will return
	 * true 50% of the time if the given value is 3, this will return true 33.3%
	 * of the time If the given value is 4, this will return true 25% of the
	 * time etc.
	 * @param oneInXXX the chance for false + 1.
	 * @return a random boolean with bias
	 */
	public static boolean nextBoolean(int oneInXXX) {
		return r.nextInt(oneInXXX) == 0;
	}
	
	/**
	 * Gets a gaussian distributed randomized value between 0 and the
	 * {@code maximum} value. <br>
	 * The mean (average) is maximum / 2.
	 * @param meanModifier The modifier used to determine the mean.
	 * @param r The random instance.
	 * @param maximum The maximum value.
	 * @return The randomized value.
	 */
	public static double getGaussian(double meanModifier, double maximum) {
		//TODO: This may caused a crash at some stage (See crash/jstack_1.txt for stacktrace of threads)
		//Thus we are using r.nextDouble() * maximum instead.
		
		/*
		 * double mean = maximum * meanModifier; double deviation = mean * 1.79;
		 * double value = 0; do { value = Math.floor(mean + r.nextGaussian() *
		 * deviation); } while (value < 0 || value > maximum); return value;
		 */
		
		int chance = r.nextInt(3);
		double v = r.nextDouble();
		while(chance-- > 0){
			double n = r.nextDouble();
			if(n + v > 1){
				continue;
			}
			v = v + n;
		}
		
		return v * maximum;
	}
}