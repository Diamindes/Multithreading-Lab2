
import generator.InputGenerator;
import mpi.MPI;

public class Lab2 {

  public static void main(String[] args) throws Exception {
    switch (Integer.parseInt(args[3])) {
      case 1:
        System.out.println("Run program");
        String inputMatrixPath = args[4];
        String inputXPath = args[5];
        double eps = Double.parseDouble(args[6]);
        String outputPath = args[7];

        MPI.Init(args);
        start(inputMatrixPath, inputXPath, eps, outputPath);
        MPI.Finalize();
        break;
      case 2:
        System.out.println("Generate matrix");
        String outputMatrixPath = args[4];
        int sizeM = Integer.parseInt(args[5]);
        InputGenerator.generateMatrix(outputMatrixPath, sizeM);
        System.out.println("Done...");
        break;
      case 3:
        System.out.println("Generate X");
        String outputStartX = args[4];
        int sizeX = Integer.parseInt(args[5]);
        double startXValue = Double.parseDouble(args[6]);
        InputGenerator.generateInitX(outputStartX, sizeX, startXValue);
        System.out.println("Done...");
        break;
      default:
    }
  }

  private static void start(String inputMatrixPath, String inputXPath, double eps, String outputPath) throws Exception {
    long start = System.currentTimeMillis();
    new MPIJacobiWorker().run(inputMatrixPath, inputXPath, eps, outputPath);
    long end = System.currentTimeMillis();

    if (MPI.COMM_WORLD.Rank() == MPIJacobiWorker.ROOT_ID) {
      System.out.printf("Execution time - %s", end - start);
      System.out.println();
    }
  }
}