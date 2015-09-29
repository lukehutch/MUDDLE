/*
 * Muddle: MUltiscale Decomposition by the DiLation of Extrema
 * 
 * Luke Hutchison, 2015
 * 
 * Available under MIT license.
 */
package io.github.lukehutch.muddle;

import java.util.Arrays;

public class Extrema {
    int radius = 0;
    float[] data;
    boolean isMax;
    int[] liveExtremumDataIdx;
    int numLiveExtrema;
    int[] maxRadius;

    public Extrema(float[] data, boolean isMax) {
        this.data = data;
        this.isMax = isMax;
        liveExtremumDataIdx = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            // At initial radius 0, all data points are extrema
            liveExtremumDataIdx[i] = i;
        }
        numLiveExtrema = data.length;
        maxRadius = new int[data.length];
        Arrays.fill(maxRadius, -1);
    }

    /**
     * Dilate extrema, removing any extrema that were dominated by others that they overlap with at the new radius.
     * 
     * Runs in O[numLiveExtrema], which is O[data.length / radius].
     */
    public void dilate() {
        radius++;
        int writeIdx = 0;
        for (int readIdx = 0, prevX = -1; readIdx < numLiveExtrema; readIdx++) {
            int x = liveExtremumDataIdx[readIdx];
            if (maxRadius[x] == -1
                && (isMax && ((x - radius >= 0 && //
                data[x - radius] > data[x]) || (x + radius < data.length && data[x + radius] >= data[x])))
                || (!isMax && ((x - radius >= 0 && //
                data[x - radius] < data[x]) || (x + radius < data.length && data[x + radius] <= data[x])))) {
                // Current extremum was dominated by a data point at this radius (and not any smaller radius)
                // -- however, don't remove the extremum until finalize(), so it can still dominate other extrema.
                maxRadius[x] = radius - 1;
            }
            int nextX = readIdx + 1 < numLiveExtrema ? liveExtremumDataIdx[readIdx + 1] : -1;
            if ((prevX >= 0 && prevX + radius >= x - radius && //
                ((isMax && data[x] < data[prevX]) || (!isMax && data[x] > data[prevX])))
                || ((nextX >= 0 && x + radius >= nextX - radius && //
                ((isMax && data[x] <= data[nextX]) || (!isMax && data[x] >= data[nextX]))))) {
                // Current extremum is dominated by previous or next extremum at this radius, remove it.
                // Update maxRadius[x], but only if extremum wasn't dominated by a data point already.
                if (maxRadius[x] == -1) {
                    maxRadius[x] = radius - 1;
                }
            } else {
                // This extremum was not dominated by another extremum at this radius, keep it.
                liveExtremumDataIdx[writeIdx++] = x;
            }
            prevX = x;
        }
        numLiveExtrema = writeIdx;
    }

    public void removeExtremaDominatedByDataPoints() {
        int writeIdx = 0;
        for (int readIdx = 0; readIdx < numLiveExtrema; readIdx++) {
            int x = liveExtremumDataIdx[readIdx];
            if (maxRadius[x] == -1) {
                // This extremum was not dominated by any data points within the max radius, keep it.
                liveExtremumDataIdx[writeIdx++] = x;
            }
        }
        numLiveExtrema = writeIdx;
    }

    /**
     * Check if extrema that survive are dominated by any data points within their radius, and if so, remove them. This
     * is run as a processing step after the final dilation step, and is needed because it's possible for an extremum A
     * to be dominated by an extremum B to its left, causing A to be removed before it has a chance to dominate an
     * extremum C to its right at a wider radius.
     * 
     * Runs in O[data.length].
     */
    public void finalize() {
        // Remove all extrema dominated by a data point at a smaller radius.
        // The max dilation radius for all remaining extremum will be -1, indicating the extremum has not been dominated. 
        removeExtremaDominatedByDataPoints();
        // Update radius to max dilation radius. (This prevents further dilation, because it will appear the extremum is dominated.)
        for (int i = 0; i < numLiveExtrema; i++) {
            int x = liveExtremumDataIdx[i];
            maxRadius[x] = radius;
        }
    }
}
