package io.github.lukehutch.muddle;

import java.util.Arrays;

public class MUDDLEInverted {

    final float[] data;
    final int maxRadius;
    final int[] extent;

    private static final float ONE_OVER_LOG2 = (float) (1.0 / Math.log(2));
    private static final float SQRT2 = (float) Math.sqrt(2);

    /**
     * Perform a MUDDLE decomposition on the input data. On exit, extent[t] gives the maximum dilation radius for each data point.
     * 
     * For some scale > 0, a data point is a local maximum if (extent[t] >= scale && data[t] > data[t+1]), and a local minimum if (extent[t] >= scale
     * && data[t] < data[t+1]).
     */
    public MUDDLEInverted(float[] data, int maxRadius) {
        this.data = data;
        this.extent = new int[data.length];
        this.maxRadius = maxRadius;

        for (int t = 0; t < data.length; t++) {
            int r = 1;
            for (int extremumType = 0; r <= maxRadius; r++) {
                int t0 = t - r, t1 = t + r;
                if (t0 < 0 || t1 >= data.length) {
                    // Ran off end of data
                    break;
                }
                float d = data[t], d0 = data[t0], d1 = data[t1];
                // Find convexity type at radius r: 1 => local max; -1 => local min; 0 => neither
                int convexityType = d >= d0 && d > d1 ? 1 : d <= d0 && d < d1 ? -1 : 0;
                if (extremumType == 0) {
                    // Fix extremum type after first dilation (when r == 1) --
                    // a point can't be both a local maximum and a local minimum.
                    extremumType = convexityType;
                }
                if (convexityType == 0 || extremumType != convexityType) {
                    // Convexity type changed => extremum is not dominant at this radius
                    break;
                }
            }
            extent[t] = r - 1;
        }
    }

    public void produceAltFracHist() {
        float[][] altHist = new float[maxRadius + 1][2]; // TODO: make these ints if multiplying by 0.9999 doesn't work
        int[] prevExtremumType = new int[maxRadius + 1];
        int[] prevExtremumIdx = new int[maxRadius + 1];
        for (int t = 0, tEnd = data.length - 1; t < tEnd; t++) {
            // Optimization -- most data points are not extrema, even at r = 1
            if (extent[t] > 0) {
                float d = data[t], d1 = data[t + 1];
                int extremumType = d > d1 ? 1 : d < d1 ? -1 : 0;
                for (int r = 1; r <= extent[t]; r++) {
                    if (prevExtremumType[r] != 0) {
                        if (extremumType != prevExtremumType[r]) {
                            // Previous extremum was of the opposite type, increase alteration frac
                            altHist[r][0] += 1.0f; // numer
                        }
                    }
                    altHist[r][1] += 1.0f; // denom

                    prevExtremumType[r] = extremumType;
                    prevExtremumIdx[r] = t;
                }
            }
        }
        smoothAltFracHist(altHist, maxRadius);
    }

    private static void smoothAltFracHist(float[][] altHist, int maxRadius) {
        // Find alternation fraction for all radii with extent greater than or equal to a given radius
        float[] altFracHist = new float[maxRadius + 1];
        for (int r = 1; r <= maxRadius; r++) {
            altFracHist[r] = altHist[r][1] == 0.0f ? 0.0f : altHist[r][0] / altHist[r][1];
        }

        // Perform trapezoidal integration on hist, using log x-axis, then find average value
        // over interval [r, 2r] using the First Mean Value Theorem for Integrals:
        // Mean = (F(2x) - F(x)) / (ln(2x) - ln(x)) = (F(2x) - F(x)) / ln(2)
        float[] cumulLogHist = new float[altFracHist.length];
        float log_rMinus1 = 0.0f;
        for (int r = 2; r < altFracHist.length; r++) {
            float log_r = (float) Math.log(r);
            float trapArea = (log_r - log_rMinus1) * (altFracHist[r] + altFracHist[r - 1]) * 0.5f;
            cumulLogHist[r] = cumulLogHist[r - 1] + trapArea;
            log_rMinus1 = log_r;
        }
        float[] meanForHarmonicInterval = new float[altFracHist.length / 2];
        float maxMean = 0.0f;
        int maxMeanRadius = 0;
        for (int r = 1; r < meanForHarmonicInterval.length; r++) {
            // For radius r, signal period is 2r + 1
            // For radius 2r, signal period is 2(2r) + 1 = 4r + 1.
            // => If signal period increases by (2r / r) = r, period increases by (4r + 1)/(2r + 1) = (2r + 1/2)/(r +
            // 1/2).
            // But this is close enough to 2r.
            float mean = (cumulLogHist[r * 2] - cumulLogHist[r]) * ONE_OVER_LOG2;
            if (mean > maxMean) {
                maxMean = mean;
                maxMeanRadius = r;
            }
            // Radius at center of interval
            float rc = r * SQRT2;
            System.out.println(Math.log(r) + "\t" + altFracHist[r] + "\t" + Math.log(rc) + "\t" + mean);
        }
        // Halfway through the interval of width ln(2) in the log domain occurs at an offset of
        // e^(ln(2)/2) = e^(ln(2^(1/2))) = sqrt(2) from the start of the interval in the linear domain.
        int optimalRadius = (int) Math.round(maxMeanRadius * SQRT2);
        float natPeriod = 2 * maxMeanRadius + 2; // N.B. would be more accurate if we used parabolic fit for
                                                 // maxMeanRadius
        System.out.println("Max mean radius: " + maxMeanRadius + "; approx natural period: " + natPeriod + "; optimal radius: " + optimalRadius);
    }

    public void produceExtentHist() {
        int[] numExtremaAtRadius = new int[maxRadius + 1];
        for (int t = 0; t < data.length; t++) {
            numExtremaAtRadius[extent[t]]++; // TODO: unused?
        }
        int[] prevExtremumType = new int[maxRadius + 1];
        int[] prevExtremumIdx = new int[maxRadius + 1];

        double binPow = 0.2;
        int nBins = 4; // TODO: use 5 or 6
        float[] binRatio = new float[nBins];
        float[] binRatioInv = new float[nBins];
        for (int i = 0; i < nBins; i++) {
            double ratio = Math.pow(2, (i + 1) * binPow);
            binRatio[i] = (float) ratio;
            binRatioInv[i] = (float) (1.0 / ratio);
        }

        int[][] sepHist = new int[maxRadius + 1][nBins];
        for (int t = 0, tEnd = data.length - 1; t < tEnd; t++) {
            // Optimization -- most data points are not extrema, even at r = 1
            if (extent[t] > 0) {
                float d = data[t], d1 = data[t + 1];
                int extremumType = d > d1 ? 1 : d < d1 ? -1 : 0;
                for (int r = 1; r <= extent[t]; r++) {
                    if (prevExtremumType[r] != 0 && extremumType == prevExtremumType[r]) {
                        // Build hist of extremum separation at each scale
                        int sep = t - prevExtremumIdx[r];
                        if (sep <= maxRadius) {
                            float sepRatio = (float) sep / (float) r;
                            // Saves taking the log of sepRatio to find the bin:
                            for (int i = 0; i < nBins; i++) {
                                if (sepRatio < binRatio[i]) {
                                    sepHist[r][i]++;
                                    break;
                                }
                            }
                        }
                    }
                    prevExtremumType[r] = extremumType;
                    prevExtremumIdx[r] = t;
                }
            }
        }
        // for (int r = 1; r <= 100 /* maxRadius */; r++) {
        // for (int j = 0; j < nBins; j++) {
        // System.out.print((j == 0 ? "" : "\t") + sepHist[r][j]);
        // }
        // System.out.println();
        // }
        float[] resampledSepHist = new float[maxRadius + 1];
        for (int i = 1; i < resampledSepHist.length; i++) {
            // Accumulate linear interpolation of domain-scaled histogram for each bin
            for (int j = 0; j < nBins; j++) {
                float scaledIdx = i * binRatioInv[j];
                int scaledIdxInt = (int) scaledIdx;
                float weight = scaledIdx - scaledIdxInt;
                if (scaledIdxInt < maxRadius) {
                    resampledSepHist[i] += (1.0f - weight) * sepHist[scaledIdxInt][j] + weight * sepHist[scaledIdxInt + 1][j];
                }
            }
            // System.out.println(resampledSepHist[i]);
        }
        float[] harmonicCombinedHist = new float[maxRadius / 2];
        for (int i = 2; i < harmonicCombinedHist.length; i++) {
            int ii = 2 * i - 2;
            if (ii > 1 && ii < maxRadius - 1) {
                harmonicCombinedHist[i] =
                    resampledSepHist[i] + 0.25f * resampledSepHist[ii - 1] + 0.5f * resampledSepHist[ii] + 0.25f * resampledSepHist[ii + 1];
            }
            System.out.println(harmonicCombinedHist[i]);
        }
    }

    /**
     * Find the most dominant extremum between the two specified indices (both non-inclusive).
     * 
     * Requires that the two specified indices are at least 2 samples apart. If maxType is true, looks for a local maximum, else a local minimum.
     * 
     * N.B. the maximum extent of any same-type extremum between t0 and t1 is floor((max - min - 2) / 2), assuming that the extrema at t0 and t1 are
     * adjacent at some resolution, since we have to fit (2r + 1) samples between them for an intermediate (dominated) extremum to have extent r.
     * (Extrema that have the opposite type of idx0 and id1 are unbounded.)
     */
    public int findMostDominantExtremumBetween(int t0, int t1, boolean maxType) {
        if (t1 - t0 < 2) {
            throw new IllegalArgumentException("Specify two indices at least 2 samples apart");
        }
        int searchRadius = (t1 - t0 - 1) / 2;
        int tCenter = (t0 + t1) / 2;
        int tMaxExtent = tCenter, maxExtent = extent[tMaxExtent];
        for (int r = 1; r <= searchRadius; r++) {
            int tLeft = tCenter - r, tRight = tCenter + r;
            // In case there's an odd number of indices between idx0 and idx1, don't pick idx0
            if (tLeft > t0) {
                // Check extremum type matches
                if ((maxType && data[tLeft] > data[tLeft + 1]) || (!maxType && data[tLeft] < data[tLeft + 1])) {
                    int r0 = extent[tLeft];
                    if (r0 > maxExtent) {
                        // Found a more dominant extremum
                        maxExtent = r0;
                        tMaxExtent = tLeft;
                    }
                }
            }
            // Check extremum type matches
            if ((maxType && data[tRight] > data[tRight + 1]) || (!maxType && data[tRight] < data[tRight + 1])) {
                int r1 = extent[tRight];
                if (r1 > maxExtent) {
                    // Found a more dominant extremum
                    maxExtent = r1;
                    tMaxExtent = tRight;
                }
            }
        }
        return tMaxExtent;
    }

    public static void main(String[] args) {
        float[] data = Utils.loadMat("/home/luke/Downloads/Training_data/DATA_01_TYPE01.mat", 1);
        // for (float f : data) {
        // System.out.println(f);
        // }

        data = Arrays.copyOf(data, 20000); // TODO

        MUDDLEInverted muddle = new MUDDLEInverted(data, 300);

        // int scale = 36;
        // for (int i = 0, iMax = data.length - 1; i < iMax; i++) {
        // int extremumType = muddle.extent[i] >= scale && data[i] > data[i + 1] ? 1 : muddle.extent[i] >= scale && data[i] < data[i + 1] ? -1 : 0;
        // System.out.println(muddle.data[i] + "\t" + muddle.extent[i] + "\t" + (extremumType == 1 ? data[i] : 0.0f) + "\t"
        // + (extremumType == -1 ? data[i] : 0.0f));
        // }

        // muddle.produceAltFracHist();

        muddle.produceExtentHist();

    }
}