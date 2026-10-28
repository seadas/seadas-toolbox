import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.atomic.AtomicLong;
import ucar.nc2.*;
import ucar.ma2.*;

/** Proof of concept: range-reading RandomAccessFile over a signed Earthdata Cloud URL. */
public class EdlRaf extends ucar.unidata.io.RemoteRandomAccessFile {
  static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
  static final AtomicLong REQUESTS = new AtomicLong(), BYTES = new AtomicLong();
  private final long length;

  EdlRaf(String url, int bufferSize, long cacheBytes) throws IOException {
    super(url, bufferSize, cacheBytes);
    String cr = get(0, 0).headers().firstValue("Content-Range").orElseThrow(() -> new IOException("no ranges"));
    length = Long.parseLong(cr.substring(cr.indexOf('/') + 1));
  }

  private HttpResponse<byte[]> get(long start, long end) throws IOException {
    try {
      HttpRequest req = HttpRequest.newBuilder(URI.create(url)).header("Range", "bytes=" + start + "-" + end).build();
      HttpResponse<byte[]> r = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
      if (r.statusCode() != 206) throw new IOException("HTTP " + r.statusCode());
      REQUESTS.incrementAndGet(); BYTES.addAndGet(r.body().length);
      return r;
    } catch (InterruptedException e) { throw new IOException(e); }
  }

  @Override public int readRemote(long pos, byte[] buf, int off, int len) throws IOException {
    long end = Math.min(pos + len, length) - 1;
    if (end < pos) return 0;
    byte[] b = get(pos, end).body();
    System.arraycopy(b, 0, buf, off, b.length);
    return b.length;
  }
  @Override public long length() { return length; }
  @Override public void closeRemote() {}
  @Override public long getLastModified() { return 0; }

  static void lap(String what, long t0) {
    System.out.printf("%-38s %6.1f s  requests=%-5d MB=%.1f%n", what, (System.nanoTime() - t0) / 1e9, REQUESTS.get(), BYTES.get() / 1e6);
  }

  public static void main(String[] a) throws Exception {
    int buf = Integer.parseInt(a[1]);
    long t0 = System.nanoTime();
    EdlRaf raf = new EdlRaf(a[0], buf, Long.parseLong(a[2]));
    NetcdfFile nc = NetcdfFiles.open(raf, "remote.nc", null, null);
    lap("open + header (buffer " + buf / 1024 + " KB)", t0);
    nc.findVariable("geophysical_data/aot_865").read(); lap("+ aot_865 full 2-D band", t0);
    nc.findVariable("navigation_data/latitude").read(); nc.findVariable("navigation_data/longitude").read(); lap("+ latitude & longitude", t0);
    Array t = nc.findVariable("geophysical_data/Rrs").read(new Section(new int[]{0,0,40}, new int[]{256,1272,1})); lap("+ Rrs one wavelength, first 256 lines", t0);
    nc.findVariable("geophysical_data/Rrs").read(new Section(new int[]{0,0,40}, new int[]{1710,1272,1})); lap("+ Rrs one wavelength, full scene", t0);
    nc.close();
  }
}
