package generator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class InputGenerator {

  public static void generateInitX(String outputPath, int size, double value) throws IOException {
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputPath))) {
      bufferedWriter.write(size + "\n");

      for (int i = 0; i < size; i++) {
        bufferedWriter.write(value + "\n");
      }
    }

  }

  public static void generateMatrix(String outputPath, int size) throws IOException {
    double[][] matrix = new double[size][size + 1];
    Random rand = new Random();

    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size + 1; j++) {
        matrix[i][j] = rand.nextDouble();
        matrix[i][i] += matrix[i][j];
      }
      matrix[i][i] = matrix[i][i] + 1;
    }

    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputPath))) {
      bufferedWriter.write(size + " " + (size + 1));
      bufferedWriter.newLine();
      for (int i = 0; i < size; i++) {
        for (int j = 0; j < size + 1; j++) {
          bufferedWriter.write(matrix[i][j] + " ");
        }
        bufferedWriter.newLine();
      }
    }
  }
}
