# Pengadegan Android V16

- Package ID `id.pengadegan.pegawai` dipertahankan dari proyek Pengadegan V10.
- Domain API: `https://pengadegan.datawebs.my.id`, hanya HTTPS domain tersebut yang dipercaya.
- Akses admin maupun pegawai mengikuti akun dan izin server; fungsi baru hanya aktif jika patch website telah dipasang.
- Pilihan Kamera dan Galeri pada laporan PPPK dibedakan; GPS adalah foreground-service dengan notifikasi aktif sesuai kebijakan Android.
- Aplikasi **tidak bisa dijamin terus tracking setelah Force Stop**, pencabutan izin, atau pembatasan baterai oleh produsen HP. Di iOS PWA tidak ada tracking background terus-menerus.
- APK belum dibangun/diteken dalam lingkungan ini. Build debug Actions untuk uji, release dengan key asli melalui Secrets.
- Jika ingin update menimpa APK lama: applicationId dan keystore penandatangan **harus sama**. Jika tidak, perlu pengujian instal ulang setelah memastikan seluruh laporan sudah tersinkron.
- Setelah build Android Pengadegan sukses, ubah nama APK yang telah diverifikasi menjadi `kelurahan-pengadegan.apk`, kemudian unggah HANYA file tersebut ke folder `downloads/` di root subdomain.
- Jangan unggah folder proyek GitHub, source, atau berkas keystore ke public_html cPanel.
