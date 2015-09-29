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

        // // Rows 1 and 2 are PPG signals
        float[] data = loadMat("/home/luke/Downloads/Training_data/DATA_01_TYPE01.mat", 1);
        float[] hist = MUDDLE.generateAlternatingExtremumTypeFractionHistogram(data, 300);
        // Rate of change of ln(x) is 1/x -- scale entries of hist by dividing by x,
        // so that taking the partial sum from x to 2x is a weighted average over a constant denominator.
        // Take cumulative hist of the result, then cumulHist[2x] - cumulHist[x] gives the mean area over the log hist
        // from log(x) to log(2x).
        float[] cumulHist = new float[hist.length];
        for (int i = 0; i < hist.length; i++) {
            cumulHist[i] = i > 0 ? cumulHist[i - 1] + hist[i] / (float) i : 0.0f;
        }

        // for (float f : cumulHist) {
        // System.out.println(f);
        // }

        // radius r => period 2r + 1 => 2 * period = 4r + 2, which has its own radius: 2r' + 1 = 4r + 2 => r' = 2r +
        // 1/2. Round down => 2r.
        for (int i = 1, n = hist.length / 2; i < n; i++) {
            int i2 = Math.min(hist.length, i * 2);
            float avg = (cumulHist[i2] - cumulHist[i - 1]); // / (i2 - i + 1); // Average hist value over [i..i*2-1]
            // inclusive
            System.out.println(i + "\t" + hist[i] + "\t" + avg);
        }
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
