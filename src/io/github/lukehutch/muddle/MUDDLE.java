/*
 * Muddle: MUltiscale Decomposition by the DiLation of Extrema
 * 
 * Luke Hutchison, 2015
 * 
 * Available under MIT license.
 */
package io.github.lukehutch.muddle;

import java.util.Arrays;

public class MUDDLE {
    /**
     * Returns an int result[2][], with the data indices of local minima in result[0] and the data indices of local
     * maxima in result[1].
     * 
     * N.B. It is possible that result[0].length != result[1].length . (If spanGaps is true, the max difference will be
     * 1.)
     */
    public static int[][] findPeaks(float[] data, int radius, boolean spanGaps) {
        Extrema minima = new Extrema(data, /* isMax = */false);
        Extrema maxima = new Extrema(data, /* isMax = */true);
        for (int r = 1; r <= radius; r++) {
            minima.dilate();
            maxima.dilate();
        }

        int[] maxIdxs = new int[data.length], minIdxs = new int[data.length];
        int numMaxIdxs = 0, numMinIdxs = 0;
        int idxMin = 0, idxMax = 0;
        int prevExtremumType = -1, prevDataIdx = 0;
        while (idxMin < minima.numLiveExtrema || idxMax < maxima.numLiveExtrema) {
            int dataIdxMin = idxMin < minima.numLiveExtrema ? minima.liveExtremumDataIdx[idxMin] : data.length;
            int dataIdxMax = idxMax < maxima.numLiveExtrema ? maxima.liveExtremumDataIdx[idxMax] : data.length;
            int currDataIdx, extremumType;
            if (dataIdxMax < dataIdxMin) {
                // Next extremum is a local maximum
                currDataIdx = dataIdxMax;
                idxMax++;
                extremumType = 1;
            } else {
                // Next extremum is a local minimum
                currDataIdx = dataIdxMin;
                idxMin++;
                extremumType = 0;
            }

            if (spanGaps && prevExtremumType != -1 && extremumType == prevExtremumType) {
                // Found two adjacent extrema of same type --
                // find the extremum of opposite type between the two with the maximum dilation radius.
                // Search outwards so ties are broken by picking the more central extremum.
                // TODO: test simply picking the largest/smallest value over the range between the pair of adjacent
                // extrema.
                // TODO: the difference is picking the widest extremum (dominant over the widest range) vs. the tallest.
                Extrema otherTypeExtrema = extremumType == 1 ? minima : maxima;
                int maxRadiusX = otherTypeExtrema.findMostDominantExtremumBetween(prevDataIdx, currDataIdx);
                // Add the opposite-type extremum into the output
                if (extremumType == 1) {
                    minIdxs[numMinIdxs++] = maxRadiusX;
                } else {
                    maxIdxs[numMaxIdxs++] = maxRadiusX;
                }
            }

            if (extremumType == 1) {
                maxIdxs[numMaxIdxs++] = currDataIdx;
            } else {
                minIdxs[numMinIdxs++] = currDataIdx;
            }

            prevExtremumType = extremumType;
            prevDataIdx = currDataIdx;
        }
        return new int[][] { Arrays.copyOf(minIdxs, numMinIdxs), Arrays.copyOf(maxIdxs, numMaxIdxs) };
    }

    public static float[] generateAlternatingExtremumTypeFractionHistogram(float[] data, int maxRadius) {

        float[] alternationHist = new float[maxRadius + 1];
        Extrema minima = new Extrema(data, /* isMax = */false);
        Extrema maxima = new Extrema(data, /* isMax = */true);
        for (int r = 1; r <= maxRadius; r++) {
            minima.dilate();
            maxima.dilate();

            int numSame = 0, numDiff = 0;
            int idxMin = 0, idxMax = 0;
            int prevExtremumType = -1;
            while (idxMin < minima.numLiveExtrema || idxMax < maxima.numLiveExtrema) {
                int dataIdxMin = idxMin < minima.numLiveExtrema ? minima.liveExtremumDataIdx[idxMin] : data.length;
                int dataIdxMax = idxMax < maxima.numLiveExtrema ? maxima.liveExtremumDataIdx[idxMax] : data.length;
                int extremumType;
                if (dataIdxMax < dataIdxMin) {
                    // Next extremum is a local maximum
                    idxMax++;
                    extremumType = 1;
                } else {
                    // Next extremum is a local minimum
                    idxMin++;
                    extremumType = 0;
                }
                if (prevExtremumType != -1) {
                    if (extremumType == prevExtremumType) {
                        numSame++;
                    } else {
                        numDiff++;
                    }
                }
                prevExtremumType = extremumType;
            }
            int denom = numSame + numDiff;
            alternationHist[r] = denom == 0 ? 0.0f : (float) numDiff / (float) denom;
            
            System.out.println((minima.numLiveExtrema + maxima.numLiveExtrema)*.5 + "\t" + alternationHist[r]);
        }
        return alternationHist;
    }
}
