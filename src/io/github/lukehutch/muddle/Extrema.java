package io.github.lukehutch.muddle;

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
    }

    /**
     * Dilate extrema, removing any extrema that were dominated by others that they overlap with at the new radius.
     * 
     * Runs in O[numLiveExtrema], which is O[data.length / radius].
     */
    public void dilate() {
        radius++;
        int prevX = -1;
        for (int readIdx = 0, maxReadIdx = numLiveExtrema, writeIdx = 0; readIdx < maxReadIdx; readIdx++) {
            int x = liveExtremumDataIdx[readIdx];
            int nextX = readIdx + 1 < maxReadIdx ? liveExtremumDataIdx[readIdx + 1] : -1;
            // if current extremum is dominated by previous or next extremum at this radius
            if ((prevX >= 0 && prevX + radius >= x - radius && ((isMax && data[x] < data[prevX]) || (!isMax && data[x] > data[prevX])))
                || ((nextX >= 0 && x + radius >= nextX - radius && ((isMax && data[x] <= data[nextX]) || (!isMax && data[x] >= data[nextX]))))) {
                // This extremum did not survive this dilation, remove it
                numLiveExtrema--;
            } else {
                // This extremum survived (keep array packed)
                maxRadius[x] = radius;
                liveExtremumDataIdx[writeIdx++] = x;
            }
            prevX = x;
        }
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
        // Check if extremum is dominated by a data value at the final dilation radius
        for (int readIdx = 0, maxReadIdx = numLiveExtrema, writeIdx = 0; readIdx < maxReadIdx; readIdx++) {
            int x = liveExtremumDataIdx[readIdx];
            boolean extremumRemoved = false;
            for (int r = 1; r <= radius; r++) {
                // If extremum is dominated by a data value at x +/- r
                // (or co-dominant with a data value at x + r)
                if ((isMax && ((x - r >= 0 && data[x - r] > data[x]) || (x + r < data.length && data[x + r] >= data[x])))
                    || (!isMax && ((x - r >= 0 && data[x - r] < data[x]) || (x + r < data.length && data[x + r] <= data[x])))) {
                    // This extremum was dominated by a data point at radius r,
                    // update max radius to the smaller value just found, and remove the exremum.
                    maxRadius[x] = r - 1;
                    numLiveExtrema--;
                    // Stop searching once max dilation radius has been found
                    extremumRemoved = true;
                    break;
                }
            }
            if (!extremumRemoved) {
                // This extremum survived, i.e. is not dominated by a data point within x +/- radius
                // (keep array packed)
                liveExtremumDataIdx[writeIdx++] = x;
            }
        }

    }
}
