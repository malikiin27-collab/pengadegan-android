package id.pengadegan.pegawai;
final class AppConfig {
 static final String ORIGIN="https://pengadegan.datawebs.my.id";
 static final String BASE=ORIGIN;
 static boolean trusted(android.net.Uri u){return "https".equals(u.getScheme()) && "pengadegan.datawebs.my.id".equals(u.getHost()) && (u.getPort()==-1 || u.getPort()==443) && (u.getPath()!=null && u.getPath().startsWith("/"));}
}
