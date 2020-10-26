import static java.lang.Double.parseDouble;
import static util.LogUtils.log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import mpi.MPI;

public class MPIJacobiWorker {

  public static final int ROOT_ID = 0;

  private int vectorDimension;
  private int myId;

  private double[] B;
  private double[] localB;
  private int[] partCountsB;
  private int[] partOffsetsB;
  private int myPartCountB;
  private int myPartOffsetB;

  private double[] g;
  private double[] localG;
  private int[] partCountsG;
  private int[] partOffsetsG;
  private int myPartCountG;
  private int myPartOffsetG;

  private double[] startX;

  public void run(String inputMatrixPath, String inputXPath, double eps, String outputPath)
      throws Exception {
    B = new double[0];
    g = new double[0];

    if (MPI.COMM_WORLD.Rank() == ROOT_ID) {
      readMatrix(inputMatrixPath);
      readStartX(inputXPath);
      vectorDimension = g.length;
    }

    int[] bufWithVectorDimension = new int[]{vectorDimension};
    MPI.COMM_WORLD.Bcast(bufWithVectorDimension, 0, 1, MPI.INT, ROOT_ID);
    vectorDimension = bufWithVectorDimension[0];

    if (MPI.COMM_WORLD.Size() > vectorDimension) {
      throw new Exception("Too much processes!");
    }

    initValues();
    localB = new double[myPartCountB];
    localG = new double[myPartCountG];

    MPI.COMM_WORLD.Scatterv(B, 0, partCountsB, partOffsetsB, MPI.DOUBLE,
        localB, 0, myPartCountB, MPI.DOUBLE, ROOT_ID);

    MPI.COMM_WORLD.Scatterv(g, 0, partCountsG, partOffsetsG, MPI.DOUBLE,
        localG, 0, myPartCountG, MPI.DOUBLE, ROOT_ID);

    MPI.COMM_WORLD.Barrier();

    try {
      jacobiCalculate(eps, outputPath);
    } catch (Exception e) {
      log(e.getMessage());
      throw e;
    }

    log("Done");
  }

  public void jacobiCalculate(double eps, String outputPath) throws IOException {
    double[] previousX;
    double[] nextX = startX;
    do {
      if (myId != ROOT_ID) {
        nextX = new double[vectorDimension];
      }

      previousX = nextX;
      previousX = broadcast(previousX);
      nextX = collectNextX(makeIteration(previousX));
    } while (decideWhatToDo(previousX, nextX, eps));

    if (myId == ROOT_ID) {
      //log("Answer: ", nextX);
      saveResult(outputPath, nextX);
    }
  }

  private double[] broadcast(double[] buffer) {
    MPI.COMM_WORLD.Bcast(buffer, 0, buffer.length, MPI.DOUBLE, ROOT_ID);

    return buffer;
  }

  public double[] makeIteration(double[] previousX) {
    double[] nextX;
    int partSize = localG.length;
    nextX = new double[partSize];

    for (int i = 0; i < partSize; i++) {
      nextX[i] = localG[i];
      for (int j = 0; j < vectorDimension; j++) {
        if (i + myPartOffsetG != j) {
          nextX[i] -= localB[i * vectorDimension + j] * previousX[j];
        }
      }
      nextX[i] /= localB[i * vectorDimension + i + myPartOffsetG];
    }

    return nextX;
  }

  private boolean decideWhatToDo(double[] previousX, double[] nextX, double eps) {
    double[] norm = new double[1];

    if (myId == ROOT_ID) {
      double maxNorm = getNorm(previousX, nextX);
      norm = new double[]{maxNorm};
      log("Current norm = " + norm[0]);
    }

    norm = broadcast(norm);

    return norm[0] >= eps;
  }

  private void initValues() {
    myId = MPI.COMM_WORLD.Rank();

    initPartCounts(vectorDimension, MPI.COMM_WORLD.Size());
    initPartOffsets();

    myPartCountB = partCountsB[myId];
    myPartOffsetB = partOffsetsB[myId];
    myPartCountG = partCountsG[myId];
    myPartOffsetG = partOffsetsG[myId];
  }

  public void initPartOffsets() {
    int clusterSize = partCountsG.length;

    partOffsetsB = new int[vectorDimension];
    partOffsetsG = new int[vectorDimension];

    IntStream.range(1, clusterSize).forEach(i -> {
      partOffsetsB[i] += partOffsetsB[i - 1] + partCountsB[i - 1];
      partOffsetsG[i] += partOffsetsG[i - 1] + partCountsG[i - 1];
    });

  }

  public void initPartCounts(int vectorDimension, int clusterSize) {
    partCountsG = new int[vectorDimension];
    partCountsB = new int[vectorDimension];
    int localVectorDimension = vectorDimension;
    int localClusterSize = clusterSize;
    for (int i = 0; i < clusterSize; i++) {
      partCountsG[i] = localVectorDimension / localClusterSize;
      localVectorDimension -= partCountsG[i];
      localClusterSize--;
      partCountsB[i] = partCountsG[i] * vectorDimension;
    }
  }

  private double[] collectNextX(double[] buffToSend) {
    double[] receiveBuffer = myId == ROOT_ID ? new double[vectorDimension] : new double[0];
    MPI.COMM_WORLD.Gatherv(buffToSend, 0, myPartCountG, MPI.DOUBLE,
        receiveBuffer, 0, partCountsG, partOffsetsG, MPI.DOUBLE, ROOT_ID);

    return receiveBuffer;
  }

  public void readMatrix(String inputMatrixPath) throws IOException {
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(inputMatrixPath))) {
      String[] size = bufferedReader.readLine().split(" ");
      int rows = Integer.parseInt(size[0]);
      int cols = Integer.parseInt(size[0]);
      B = new double[rows * rows];
      g = new double[rows];

      for (int i = 0; i < rows; i++) {
        String[] next = bufferedReader.readLine().split(" ");
        for (int j = 0; j < rows; j++) {
          B[i * rows + j] = parseDouble(next[j]);
        }
        g[i] = parseDouble(next[cols]);
      }
    }
  }

  public void readStartX(String inputXPath) throws IOException {
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(inputXPath))) {
      int size = Integer.parseInt(bufferedReader.readLine());
      startX = new double[size];

      for (int i = 0; i < size; i++) {
        startX[i] = parseDouble(bufferedReader.readLine());
      }
    }
  }

  public void saveResult(String outputPath, double[] result) throws IOException {
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputPath))) {
      bufferedWriter.write(result.length + "\n");

      for (double value : result) {
        bufferedWriter.write(value + "\n");
      }
    }
  }

  public double getNorm(double[] previousX, double[] nextX) {
    if (previousX.length != nextX.length) {
      throw new AssertionError();
    }

    List<Double> differenceX = new ArrayList<>();
    for (int i = 0; i < previousX.length; i++) {
      differenceX.add(Math.abs(nextX[i] - previousX[i]));
    }

    return differenceX.stream()
        .max(Double::compareTo)
        .orElseThrow(RuntimeException::new);
  }
}
