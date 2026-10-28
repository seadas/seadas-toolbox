import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import ucar.nc2.*;
import ucar.ma2.*;

/** PoC: RandomAccessFile with an aligned LRU block cache; misses spanning several blocks are fetched in parallel. */
public class BlockRaf extends ucar.unidata.io.RandomAccessFile {
  static final HttpClient CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
  static final ExecutorService POOL = Executors.newFixedThreadPool(8);
  static final AtomicLong REQUESTS = new AtomicLong(), BYTES = new AtomicLong();
  final String url; final int block; final long length;
  final Map<Long, byte[]> cache;

  BlockRaf(String url, int block, int maxBlocks) throws IOException {
    super(8192);
    this.url = url; this.block = block; this.location = url;
    this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
      protected boolean removeEldestEntry(Map.Entry<Long, byte[]> e) { return size() > maxBlocks; } });
    String cr = fetch(0, 0).headers().firstValue("Content-Range").orElseThrow();
    length = Long.parseLong(cr.substring(cr.indexOf('/') + 1));
  }

  HttpResponse<byte[]> fetch(long s, long e) throws IOException {
    try {
      HttpResponse<byte[]> r = CLIENT.send(HttpRequest.newBuilder(URI.create(url)).header("Range", "bytes=" + s + "-" + e).build(),
          HttpResponse.BodyHandlers.ofByteArray());
      if (r.statusCode() != 206) throw new IOException("HTTP " + r.statusCode());
      REQUESTS.incrementAndGet(); BYTES.addAndGet(r.body().length); return r;
    } catch (InterruptedException ex) { throw new IOException(ex); }
  }

  byte[] blockAt(long idx) throws IOException {
    byte[] b = cache.get(idx);
    if (b == null) { long s = idx * block; b = fetch(s, Math.min(s + block, length) - 1).body(); cache.put(idx, b); }
    return b;
  }

  @Override protected int read_(long pos, byte[] buf, int off, int len) throws IOException {
    if (pos >= length) return -1;
    len = (int) Math.min(len, length - pos);
    long first = pos / block, last = (pos + len - 1) / block;
    if (last > first) {  // prefetch missing blocks in parallel
      List<Future<?>> fs = new ArrayList<>();
      for (long i = first; i <= last; i++) { long k = i; if (!cache.containsKey(k)) fs.add(POOL.submit(() -> { blockAt(k); return null; })); }
      for (Future<?> f : fs) try { f.get(); } catch (Exception e) { throw new IOException(e); }
    }
    int done = 0;
    while (done < len) {
      long p = pos + done; byte[] b = blockAt(p / block); int o = (int) (p % block);
      int n = Math.min(len - done, b.length - o); System.arraycopy(b, o, buf, off + done, n); done += n;
    }
    return len;
  }
  @Override public long length() { return length; }
  @Override public void close() {}

  static void lap(String what, long t0) {
    System.out.printf("%-38s %6.1f s  requests=%-5d MB=%.1f%n", what, (System.nanoTime() - t0) / 1e9, REQUESTS.get(), BYTES.get() / 1e6);
  }
  public static void main(String[] a) throws Exception {
    int blk = Integer.parseInt(a[1]);
    long t0 = System.nanoTime();
    NetcdfFile nc = NetcdfFiles.open(new BlockRaf(a[0], blk, (int) ((1L << 30) / blk)), "remote.nc", null, null);
    lap("open + header (block " + blk / 1024 + " KB)", t0);
    nc.findVariable("geophysical_data/aot_865").read(); lap("+ aot_865 full 2-D band", t0);
    nc.findVariable("navigation_data/latitude").read(); nc.findVariable("navigation_data/longitude").read(); lap("+ latitude & longitude", t0);
    nc.findVariable("geophysical_data/Rrs").read(new Section(new int[]{0,0,40}, new int[]{256,1272,1})); lap("+ Rrs one wavelength, first 256 lines", t0);
    nc.findVariable("geophysical_data/Rrs").read(new Section(new int[]{0,0,40}, new int[]{1710,1272,1})); lap("+ Rrs one wavelength, full scene", t0);
    nc.findVariable("geophysical_data/Rrs").read(new Section(new int[]{0,0,41}, new int[]{1710,1272,1})); lap("+ neighbouring wavelength (cached)", t0);
    nc.close(); POOL.shutdown();
  }
}
