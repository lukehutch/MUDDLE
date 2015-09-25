/*
 * Muddle: MUltiscale Decomposition by the DiLation of Extrema
 * 
 * Luke Hutchison, 2015
 * 
 * Available under MIT license.
 */
package io.github.lukehutch.muddle;

import java.util.Arrays;

public class MUDDLETest {

    public static void main(String[] args) {
        //                           0  1  2  3  4  5  6  7  8  9 10 11
        float[] data = new float[] { 0, 2, 8, 3, 4, 3, 2, 5, 3, 1, 3, 0 };
        int[][] extrema = MUDDLE.findPeaks(data, 2, true);
        System.out.println(Arrays.toString(extrema[0]));
        System.out.println(Arrays.toString(extrema[1]));
    }

}
