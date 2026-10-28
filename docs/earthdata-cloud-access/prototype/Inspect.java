import ucar.nc2.*;
public class Inspect {
  public static void main(String[] a) throws Exception {
    long t0 = System.nanoTime();
    NetcdfFile nc = NetcdfFiles.open(new BlockRaf(a[0], 262144, 4096), "remote.nc", null, null);
    BlockRaf.lap("open + header", t0);
    for (Variable v : nc.getVariables()) if (v.getSize() > 100000) {
      Attribute c = v.findAttribute("_ChunkSizes");
      System.out.println(v.getFullName() + " " + java.util.Arrays.toString(v.getShape()) + " " + v.getDataType() + " chunks=" + (c == null ? "-" : c.getValues()));
    }
    System.exit(0);
  }
}
