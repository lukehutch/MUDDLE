/*
 * Muddle: MUltiscale Decomposition by the DiLation of Extrema
 * 
 * Luke Hutchison, 2015
 * 
 * Available under MIT license.
 */
package io.github.lukehutch.muddle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

public class Utils {

    public static float[] createRandomData(int len) {
        double period = 50;
        double periodRandomSkewFactor = 0.05;
        double amplitudeNoiseFactor = 1.5;

        float[] data = new float[len];
        double phase = 0;
        for (int i = 0; i < len; i++) {
            phase += (1 + (Math.random() - 0.5) * periodRandomSkewFactor) / period;
            data[i] = (float) (Math.sin(2 * Math.PI * phase) + (Math.random() - 0.5) * amplitudeNoiseFactor);
            // System.out.println(data[i]);
        }
        return data;
    }

    public static float[] loadData(String filename) {
        try {
            ArrayList<Float> vals = new ArrayList<>();
            for (String line : Files.readAllLines(Paths.get(filename))) {
                vals.add(Float.parseFloat(line));
            }
            float[] valsArr = new float[vals.size()];
            for (int i = 0; i < vals.size(); i++) {
                valsArr[i] = vals.get(i);
            }
            return valsArr;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static float[] loadMat(String filename, int row) {
        MatFileReader reader;
        try {
            reader = new MatFileReader(filename);
            Map<String, MLArray> content = reader.getContent();
            if (content.size() != 1) {
                throw new RuntimeException("Expected one array; found " + content.size());
            }
            for (MLArray matArray : content.values()) {
                double[][] arr = ((MLDouble) matArray).getArray();
                // for (int i = 0; i < arr[0].length; i++) {
                // for (int j = 0; j < arr.length; j++) {
                // System.out.print((j > 0 ? "\t" : "") + arr[j][i]);
                // }
                // System.out.println();
                // }
                float[] data = new float[arr[0].length];
                for (int i = 0; i < arr[0].length; i++) {
                    data[i] = (float) arr[row][i];
                }
                return data;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        // float[] data = loadData("filename");

        // PPG data available at: http://www.signalprocessingsociety.org/spcup2015/index.html
        // Rows 1 and 2 are PPG signals
        float[] data = loadMat("/home/luke/Downloads/Training_data/DATA_01_TYPE01.mat", 1);
        float[] hist = MUDDLE.generateAlternatingExtremumTypeFractionHistogram(data, 300);

        // Perform trapezoidal integration on function, using log x-axis, then find average value
        // over interval x to 2x using the First Mean Value Theorem for Integrals:
        // Mean = (F(2x) - F(x)) / (ln(2x) - ln(x)) = (F(2x) - F(x)) / ln(2)
        float[] cumulLogHist = new float[hist.length];
        float log_rMinus1 = 0.0f;
        for (int r = 2; r < hist.length; r++) {
            float log_r = (float) Math.log(r);
            float trapArea = (log_r - log_rMinus1) * (hist[r] + hist[r - 1]) * 0.5f;
            cumulLogHist[r] = cumulLogHist[r - 1] + trapArea;
            log_rMinus1 = log_r;
        }
        float[] meanForHarmonic = new float[hist.length / 2];
        float maxMean = 0.0f;
        int maxMeanRadius = 0;
        float oneOverLog2 = (float) (1.0 / Math.log(2)), sqrt2 = (float) Math.sqrt(2);
        for (int r = 1; r < meanForHarmonic.length; r++) {
            // For radius r, signal period is 2(2r + 1) = 4r + 2.
            // For radius 2r, signal period is 2(2(2r) + 1) = 8r + 2.
            // => If signal period increases by (2r / r) = r, period increases by (8r + 2)/(4r + 2) = (2r + 1/2)/(r + 1/2).
            // But this is close enough to 2r.
            float mean = (cumulLogHist[r * 2] - cumulLogHist[r]) * oneOverLog2;
            if (mean > maxMean) {
                maxMean = mean;
                maxMeanRadius = r;
            }
            float rc = r * sqrt2;
            System.out.println(r + "\t" + Math.log(r) + "\t" + hist[r] + "\t" + rc + "\t" + Math.log(rc) + "\t" + mean);
        }
        // Halfway through the interval of width ln(2) in the log domain occurs at an offset of
        // e^(ln(2)/2) = e^(ln(2^(1/2))) = sqrt(2) from the start of the interval in the linear domain.
        int optimalRadius = (int) Math.round(maxMeanRadius * sqrt2);
        float natPeriod = 4 * maxMeanRadius + 2;  // N.B. would be more accurate if we used parabolic fit for maxMeanRadius
        System.out.println("Max mean radius: " + maxMeanRadius + "; approx natural period: " + natPeriod + "; optimal radius: " + optimalRadius);
        System.exit(1);

        // // float[] data = createRandomData(10000);
        // float[] hist = MUDDLE.generateAlternatingExtremumTypeFractionHistogram(data, 60);
        // float[] cumulHist = new float[hist.length];
        // for (int i = 0; i < hist.length; i++) {
        // cumulHist[i] = (i > 0 ? cumulHist[i - 1] : 0.0f) + hist[i];
        // }
        // // radius r => period 2r + 1 => 2 * period = 4r + 2, which has its own radius: 2r' + 1 = 4r + 2 => r' = 2r +
        // // 1/2. Round down => 2r.
        // for (int i = 1, n = hist.length / 2; i < n; i++) {
        // int i2 = Math.min(hist.length, i * 2 - 1);
        // float avg = (cumulHist[i2] - cumulHist[i - 1]) / (i2 - i + 1); // Average hist value over [i..i*2-1]
        // // inclusive
        // System.out.println(i + "\t" + hist[i] + "\t" + avg);
        // }

        int[][] peaks = MUDDLE.findPeaks(data, /* radius = */15, /* spanGaps = */true);
        float[][] graph = new float[data.length][3];
        for (int i = 0; i < data.length; i++) {
            graph[i][0] = data[i];
        }
        for (int i = 0; i < peaks[0].length; i++) {
            int idx = peaks[0][i];
            graph[idx][1] = data[idx];
        }
        for (int i = 0; i < peaks[1].length; i++) {
            int idx = peaks[1][i];
            graph[idx][2] = data[idx];
        }
        for (int i = 0; i < graph.length; i++) {
            for (int j = 0; j < 3; j++) {
                System.out.print((j > 0 ? "\t" : "") + graph[i][j]);
            }
            System.out.println();
        }

    }

}
