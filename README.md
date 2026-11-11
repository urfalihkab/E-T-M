# EĞİTİM SEBLAZİZ

Arçelik'in Android TV için yaptığı "Online Eğitim Portalı" uygulamasındaki düzenin telefon ve tablet için yazılmış hâli. Şimdilik yalnızca **tonguç AKADEMİ** içeriğini gösterir.

## Düzen (TV uygulamasıyla aynı)

- **Ana Sayfa:** "Sınırsızca Öğrenin" başlığı, *Kaldığın yerden devam et* ve kademeler: İlkokul (1–4), Ortaokul (5–8), Lise (9–11), Diğer (YKS / tonguç KAMPÜS, tonguç AKADEMİ).
- **Sınıf:** Üstte ders seçici, altında raflar halinde ders başına oynatma listeleri (Matematik, Türkçe, Fen, Yazılı Kampları, Rehberlik…).
- **Liste ve Oynatıcı:** Videolar uygulamanın içinde, YouTube'un resmî oynatıcısıyla oynar. Video bitince sıradaki başlar. Tam ekran desteklenir.
- **Ara / Favoriler / Hakkında:** TV'deki yan menünün karşılığı. Telefonda alt menü, tablette yan menü olarak görünür.

## İçerik nasıl güncellenir?

- `tools/build-catalog.js` dosyası Tonguç'un herkese açık sınıf kanallarını (`@tonguc1` … `@tonguc11`, `@kampus`, `@tongucakademi`) tarar ve sonucu `catalog/` klasörüne yazar.
- `.github/workflows/update-catalog.yml` bu betiği **her pazartesi** kendiliğinden çalıştırır.
- Uygulama açılışta `catalog/` dosyalarını GitHub'dan okur. Bu yüzden yeni APK kurmadan yeni dersler görünür. İnternet yoksa son indirilen kopya ya da uygulamaya gömülü kopya kullanılır.
- Elle güncellemek için GitHub'da **Actions → Ders kataloğunu güncelle → Run workflow** adımlarını izleyin.

## APK

Koda yapılan her değişiklikte **Actions → APK derle** iş akışı çalışır. APK, **Releases** sayfasında en son sürüm olarak yayınlanır.

Play Store'a yüklenecekse kalıcı bir imza anahtarı gerekir. Bunun için depo ayarlarına şu Secrets değerleri eklenir: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Bu değerler eklenmezse APK hata ayıklama anahtarıyla imzalanır; doğrudan kurulum için bu yeterlidir.
