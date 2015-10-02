/*
 * Muddle: MUltiscale Decomposition by the DiLation of Extrema
 * 
 * Luke Hutchison, 2015
 * 
 * Available under MIT license.
 */
package io.github.lukehutch.muddle;

public class Extrema {
    int radius = 0;
    float[] data;
    boolean isMax;
    int[] liveExtremumDataIdx;
    int numLiveExtrema;
    int[] extent;

    public Extrema(float[] data, boolean isMax) {
        this.data = data;
        this.isMax = isMax;
        liveExtremumDataIdx = new int[data.length];
        for (int t = 0; t < data.length; t++) {
            // At initial radius 0, all data points are extrema
            liveExtremumDataIdx[t] = t;
        }
        numLiveExtrema = data.length;
        extent = new int[data.length]; // Initially all 0
    }

    /**
     * Dilate extrema, removing any extrema that were dominated by data points at the new radius.
     * 
     * Runs in O[numLiveExtrema], which is O[data.length / radius].
     */
    public void dilate() {
        radius++;
        for (int readIdx = 0, maxReadIdx = numLiveExtrema, writeIdx = 0; readIdx < maxReadIdx; readIdx++) {
            int t = liveExtremumDataIdx[readIdx], tl = t - radius, tr = t + radius;
            if (!( //
            (isMax && //
            ((tl >= 0 && data[tl] > data[t]) || (tr < data.length && data[tr] >= data[t]))) //
            || (!isMax && //
            ((tl >= 0 && data[tl] < data[t]) || (tr < data.length && data[tr] <= data[t]))))) {
                // Current extremum was not dominated by a data point at this radius
                extent[t] = radius;
                liveExtremumDataIdx[writeIdx++] = t;
            } else {
                numLiveExtrema--;
            }
        }
    }

    /**
     * Find the most dominant extremum between the two specified indices (both non-inclusive). Requires that the two
     * specified indices are at least 2 samples apart.
     */
    public int findMostDominantExtremumBetween(int idx0, int idx1) {
        if (idx1 - idx0 < 2) {
            throw new IllegalArgumentException("Specify two indices at least 2 samples apart");
        }
        int centerIdx = (idx0 + idx1) / 2;
        int searchRadius = (idx1 - idx0 - 1) / 2;
        int maxRIdx = centerIdx, maxR = extent[maxRIdx];
        for (int r = 1; r <= searchRadius; r++) {
            int r0 = extent[centerIdx - r];
            if (r0 > maxR) {
                maxR = r0;
                maxRIdx = centerIdx - r;
            }
            int r1 = extent[centerIdx + r];
            if (r1 > maxR) {
                maxR = r1;
                maxRIdx = centerIdx + r;
            }
        }
        return maxRIdx;
    }
}
