package util;

import java.util.Arrays;
import mpi.MPI;

public class LogUtils {

  public static boolean enabled = true;

  public static void log(String message) {
    if (enabled) {
      int me = MPI.COMM_WORLD.Rank();
      int size = MPI.COMM_WORLD.Size();
      System.out.println(String.format("[%d/%d] %s", me, size, message));
    }
  }

  public static void log(String name, double[] array) {
    LogUtils.log(name + " " + Arrays.toString(array));
  }
}
