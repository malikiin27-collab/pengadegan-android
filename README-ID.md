# Kelurahan Pengadegan — Android V16

**Paket ini adalah kode sumber aplikasi Android untuk GitHub Actions, BUKAN APK yang sudah selesai dibangun.**

- Nama aplikasi: **Pengadegan**
- Package/application ID tetap: `id.pengadegan.pegawai`
- Domain HTTPS: `https://pengadegan.datawebs.my.id`
- `versionCode 16` / `versionName 16.0.0`
- Minimum Android 8 (API 26)

## Yang diperbarui dari Android V10

1. Membaca langsung website Pengadegan aktif, sehingga tombol admin **Hapus Permanen PPPK** dari patch website Pengadegan V16 akan muncul di aplikasi ketika server telah diperbarui dan akun admin berhasil login.
2. PDF PPPK di aplikasi Android diambil langsung melalui koneksi HTTPS dengan cookie login WebView, dicek header PDF, kemudian disimpan / dibuka menggunakan aplikasi pembaca PDF. Hal ini menghindari keterbatasan unduhan `blob:` WebView untuk tautan PPPK yang dikenal.
3. Progress unduhan PDF tampil dari byte yang benar-benar diterima saat `Content-Length` diketahui; tanpa `Content-Length` ditampilkan proses menunggu/ukuran diterima, bukan persen rekaan.
4. Pilihan **Kamera** dan **Galeri** pada PPPK tetap mengikuti formulir web. Upload foto PJLP yang disyaratkan kamera tetap mengikuti atribut `capture` website.
5. Mempertahankan tracking GPS dengan layanan foreground, antrean GPS offline, penyaringan titik tidak akurat/lompatan, dan interval target 5 detik. Layanan dapat dijalankan ulang Android jika memungkinkan, tetapi **tidak dapat dijamin aktif sesudah pengguna melakukan Force Stop, saat kebijakan produsen mematikannya, atau ketika izin lokasi dicabut**.
6. Cache browser lama dibersihkan sekali pada update ke V16 tanpa menghapus cookie login dan data akun.

## Cara build APK lewat GitHub (tidak perlu Android Studio)

1. Di komputer, ekstrak ZIP ini. Upload **semua isi folder hasil ekstrak**, termasuk `.github/`, ke root repo GitHub Pengadegan Anda (pertahankan package ID dan domain).
2. GitHub → **Actions** → **APK Pengadegan V16** → **Run workflow**, tunggu status hijau.
3. Unduh artefak **Pengadegan-APK-Uji-V16**. Ekstrak; file `app-debug.apk` di dalamnya untuk pengujian.
4. Uji login admin, akun PJLP/PPPK, kamera, galeri, upload, tracking saat layar mati, Lihat/Unduh PDF, dan Hapus Permanen pada *data uji*.
5. Untuk mengunggah ke website Pengadegan, gunakan APK rilis yang **sudah ditandatangani keystore asli** dan sudah diuji. Letakkan di `downloads/kelurahan-pengadegan.apk` pada cPanel; file master website, database, dan SQL tidak perlu diimpor ulang.

## Build rilis yang dapat memperbarui aplikasi terpasang

Untuk pembaruan di perangkat tanpa uninstall, rilis baru **harus** memakai package ID dan **kunci tanda tangan yang sama** dengan APK sebelumnya. Build debug dari runner GitHub yang berbeda belum tentu mempunyai kunci yang sama, sehingga mungkin tidak dapat menimpa aplikasi lama.

Simpan keystore rilis asli dalam GitHub Repository Secrets (JANGAN unggah ke repo/chat) dan siapkan:

- `PENGADEGAN_KEYSTORE_BASE64` — seluruh isi keystore dalam Base64
- `PENGADEGAN_STORE_PASSWORD`
- `PENGADEGAN_KEY_ALIAS`
- `PENGADEGAN_KEY_PASSWORD`

Jalankan workflow **APK Pengadegan V16 (Release signed)**, unduh `Pengadegan-APK-Release-V16` dan lakukan uji sebelum distribusi.

> Bila keystore rilis lama hilang, Android tidak mengizinkan update langsung dengan kunci berbeda. Backup data yang diperlukan dan instal ulang mungkin diperlukan. Jangan uninstall sebelum dipastikan data di server tersimpan.

## Penting terkait kode website

- Tombol **Hapus Permanen** dan fitur laporan PDF berasal dari website: paket Android ini **tidak memasang patch cPanel V16** dan tidak mengubah database.
- Jika file foto benar-benar hilang di `storage/pegawai`, Android tidak bisa mengembalikannya. Pulihkan dari backup cPanel.
- Untuk iPhone, proyek ini bukan aplikasi iOS; iOS memerlukan PWA atau proyek iOS terpisah.
- Android SDK dan Gradle tidak tersedia di lingkungan penyiapan paket, jadi **APK belum dikompilasi dan belum diuji di perangkat**.
