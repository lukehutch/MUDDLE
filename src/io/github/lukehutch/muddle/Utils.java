/*
 * Muddle: MUltiscale Decomposition by the DiLation of Extrema
 * 
 * Luke Hutchison, 2015
 * 
 * Available under MIT license.
 */
package io.github.lukehutch.muddle;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import com.jmatio.io.MatFileHeader;
import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

public class Utils {

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
        // float[] hist = MUDDLE.generateAlternatingExtremumTypeFractionHistogram(data, 300);
        // for (float f : hist) {
        // System.out.println(f);
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
