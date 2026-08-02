# DANA Notif Gateway

APK sederhana untuk membaca notifikasi DANA masuk dan mengirim data pembayaran ke webhook bot/VPS.

## Cara kerja

1. DANA menampilkan notifikasi uang masuk.
2. App membaca notifikasi dari package `id.dana`.
3. App mencari nominal Rupiah dari isi notifikasi.
4. App mengirim JSON ke webhook yang kamu isi.

## JSON webhook

```json
{
  "source": "dana_notification",
  "test": false,
  "amount": 10047,
  "title": "DANA",
  "text": "Uang masuk Rp10.047",
  "big_text": "Isi lengkap notifikasi",
  "post_time": 1785660000000
}
```

Jika `Secret` diisi di aplikasi, request akan membawa header:

```text
X-Webhook-Secret: secret-kamu
```

## Cara build APK

Buka folder ini di Android Studio, lalu pilih:

```text
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

Hasil APK biasanya ada di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Cara pakai

1. Install APK.
2. Buka aplikasi.
3. Isi Webhook URL dari bot/VPS kamu.
4. Isi Secret jika webhook kamu butuh proteksi.
5. Tap `Simpan Setting`.
6. Tap `Buka Akses Notifikasi`.
7. Aktifkan akses untuk `DANA Notif Gateway`.
8. Tap `Test Webhook`.

## Catatan

- HP harus menyala dan aplikasi DANA harus menampilkan notifikasi.
- Gunakan nominal unik di invoice, misalnya Rp10.047, agar bot mudah mencocokkan pembayaran.
- Jangan pakai ini untuk membaca notifikasi akun orang lain.
