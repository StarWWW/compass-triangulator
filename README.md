# Compass Triangulator

Bir lodestone pusulasının gösterdiği yeri bul: pusulanın yönünü onda bir derece hassasiyetle gösterir, farklı yerlerden yön ölçümleri alır ve bunları kesiştirerek hedefin yaklaşık konumunu hata payıyla birlikte hesaplar. Pusulayı eline alman gerekmez: bir sunucunun açık artırma ekranında (`/ah`) ya da bir sandıkta duran pusuladan da ölçüm alınır, ör. her `/rtp` sonrası ekranı açıp pusulanın üstüne gelerek.

*English: A client-side Fabric mod for Minecraft 26.1.2, 26.2 and 26.3 that finds where a lodestone compass points. It shows the compass direction to a tenth of a degree, takes readings from different places and crosses them into an estimated position with an error margin. The compass does not have to be held: readings can also be taken from a compass shown in a screen, such as a server's auction house, after each random teleport.*

## Nasıl çalışıyor

Oyun, lodestone pusulasının iğnesini **istemcide** çevirir: hedefin koordinatı pusula eşyasının içinde (`lodestone_tracker`) gelir ve iğne o noktaya döndürülür. Yani iğne bir yeri gösteriyorsa o yer istemcide zaten bilinir. Mod bu yüzden iki şeyi birden yapar:

- **Pusula verisi:** Pusulada kayıtlı hedefi ve oraya olan tam yönü gösterir. Ekrandaki iğne 32 kareden oluştuğu için 11,25°'lik adımlarla döner; buradaki yön ise tamdır.
- **Üçgenleme:** Farklı yerlerden alınan yön ölçümlerini kesiştirir. Her ölçüm, durduğun yerden pusulanın gösterdiği yöne giden bir çizgidir; tahmin bu çizgilere en yakın noktadır (ağırlıklı en küçük kareler). Uzak ve kaba ölçümler daha az sayılır, aynı hesap tahminin ne kadar kesin olduğunu da verir.

Üçgenleme, sunucu gerçek hedef yerine **doğru yönde sahte bir nokta** gönderdiğinde de (ör. hep 1000 blok ötede, sen yürüdükçe güncellenen) gerçek yeri bulur: her ölçüm yine gerçek hedefe bakar. Mod, ölçümler sırasında pusulanın hedefinin değiştiğini fark ederse bunu panelde söyler ve bir ölçüm almadan önce bir an durmanı ve hedefin sabitlenmesini bekler; böylece başka bir konum için gönderilmiş eski bir nokta kullanılmaz.

Sunucu iğneyi kendi kaynak paketiyle çiziyorsa ve pusulada hedef yoksa **bakarak ölçüm** kullanılır: iğne tam yukarıyı (tam önünü) gösterene kadar dön, sonra ölçümü kaydet; bakış yönün ölçüm olur.

## Kullanım

### Ekrandaki bir pusuladan (ör. `/ah` ve `/rtp`)

1. Kontroller > Compass Triangulator'dan **Ölçüm kaydet** tuşunu ata.
2. `/ah` ekranını aç, fareyle pusulanın üstüne gel. İpucunda pusulanın hedefi (`Lodestone: X, Y, Z`), bulunduğun yerden oraya olan tam yön ve uzaklık görünür.
3. Tuşa bas: bulunduğun yerden bir ölçüm alınır. Ekran açıkken diğer tuşlar ekrana gitmeye devam eder; sadece bu modun tuşları alınır.
4. `/rtp` ile uzağa ışınlan, `/ah`'ı tekrar aç, **aynı** pusulanın üstüne gelip tekrar tuşa bas. İki ölçümden sonra tahmin ipucunda, panelde ve sohbette görünür; ölçüm arttıkça hata payı küçülür.

Ölçümler sadece tuşla alınır (fareyle üstüne gelmek yetmez), böylece ekrandaki başka pusulalar ölçümlere karışmaz. Başka bir pusulaya geçmeden önce `/compasstri clear` ile ölçümleri temizle.

Bu sadece sandık benzeri ekranlarda değil, pusula gösteren her ekranda çalışır (sohbet hariç), sunucunun açtığı onay pencereleri de buna dahil. Böyle ekranlarda modun tuşları sadece fare bir pusulanın üstündeyken alınır; fare başka yerdeyken tuşlar ekrana gider. Ekrandaki bir yazı kutusuna yazarken de tuşlar ekrana gider.

### Xaero haritasına waypoint olarak

Xaero's Minimap kuruluysa (Xaero's World Map da aynı waypoint'leri gösterir), Kontroller'den **Xaero waypoint'i olarak ekle** tuşunu ata:

- **Ekranda**, fareyle bir pusulanın üstündeyken: pusulanın hedefi (`Lodestone: X, Y, Z`) waypoint olur. Adı ilanın satırlarından okunur: `<satıcı> Lodestone <fiyat>`, ör. `Notch Lodestone $1.5K`. İpucunda waypoint'in adı önceden görünür, böylece satıcı ve fiyatın doğru okunduğunu basmadan görürsün.
- **Oyunda** (ya da `/compasstri waypoint`): ölçümlerden bir tahmin varsa tahmin, yoksa elindeki pusulanın hedefi waypoint olur. Tahmin için ad, son ölçüm alınan ilandan gelir.

Waypoint, bulunduğun dünya ve boyutun o an seçili waypoint setine, mor renkte ve `L` kısaltmasıyla eklenir ve Xaero'nun kendi dosyasına kaydedilir. Aynı koordinatta zaten bir waypoint varsa ikincisi eklenmez, adı güncellenir.

**DonutSMP'de** `/ah` listesindeki ilanlarda sadece fiyat yazar (`$ 100M`), satıcı yazmaz; listeden eklenen waypoint'in adı bu yüzden `Lodestone $100M` olur. Satıcı, ilana **sağ tıklayınca** açılan onay penceresinde ("Are you sure you want to buy this?") yazar: waypoint'i oradan, fareyle pusulanın üstündeyken eklersen adı `_Restrict Lodestone 100M` gibi olur. Mod hiçbir şeye tıklamaz; pencerede **Yes**'e basmadıkça bir şey satın alınmaz.

Satıcı ve fiyat, satırlardaki `Seller`/`Satıcı`/`Owner` ve `Price`/`Fiyat` gibi kelimelerden okunur; satıcı için o kelimeden sonraki ilk oyuncu adı alınır, `[VIP]` gibi rütbeler atlanır. Fiyat kelimesi yoksa ilk `$` ile başlayan tutar alınır. Sunucunun yazımı farklıysa `config/compasstri.properties` içine, ilk grubu satıcıyı ya da fiyatı veren kendi düzenli ifadeni yazabilirsin:

```
sellerPattern=Satan\: (\S+)
pricePattern=Fiyat\: (.+)
```

İpucundaki `Lodestone` satırı, sunucunun istemcine gönderdiği hedeftir. `/rtp` sonrası aynı kalıyorsa gerçek yer odur; her seferinde değişiyorsa sunucu doğru yönde sahte bir nokta gönderiyordur ve gerçek yeri ölçümlerin kesişimi verir.

### Elindeki pusuladan

1. Pusulayı eline al. Panel (varsayılan olarak ekranın sağında) pusulanın yönünü, pusula verisini ve ölçümleri gösterir.
2. **Pusula verisi varsa:** Sadece yürü. Her 32 blokta bir ölçüm kendiliğinden alınır. İki ölçüm yeter; panel "Pusula verisiyle aynı" diyorsa pusula gerçek yeri gösteriyordur.
3. **Bakarak:** Kontroller'den **Ölçüm kaydet** tuşunu ata (ya da `/compasstri record` yaz). İğne tam yukarıyı gösterince kaydet, sonra yana doğru yürüyüp tekrarla.
4. Tahmin panelde ve sohbette `X, Z ±hata` olarak çıkar; sohbetteki **[Kopyala]** koordinatları panoya kopyalar.

İpuçları:
- Ölçümleri birbirinden uzak ve hedefe göre **yandan** al. Hedefe doğru düz yürürken alınan ölçümler aynı çizgi üzerindedir ve kesişmez.
- Hata payı yaklaşık %95 güvenle verilir. Bakarak ±1° ile 3000 blok ötedeki bir hedefe birbirine yakın birkaç ölçüm, yüzlerce blokluk bir hata payı verir; ölçümler yayıldıkça ya da hedefe yaklaştıkça küçülür.
- Başka bir pusulaya geçince ölçümleri temizle (`/compasstri clear`). Boyut değişince ölçümler kendiliğinden silinir.
- Panel "Ölçümler birbirini tutmuyor" derse ölçümlerden biri ters yöne bakıyordur: hedef değişmiş ya da başka bir pusulanın ölçümü karışmış olabilir.

### Komutlar

Hepsi sadece senin bilgisayarında çalışır, sunucuya bir şey gönderilmez.

```
/compasstri            tahmin (ya da neden henüz yok)
/compasstri record     buradan bir ölçüm al
/compasstri undo       son ölçümü sil
/compasstri clear      bütün ölçümleri sil
/compasstri waypoint   tahmini (yoksa elindeki pusulanın hedefini) Xaero waypoint'i yap
/compasstri item       eldeki eşyanın verisi: lodestone hedefi, model, özel model verisi
```

`/compasstri item`, bir sunucunun pusulasının nasıl çalıştığını görmek için: pusulada hedef var mı, iğne özel bir modelle mi çiziliyor.

### Tuşlar

Hepsi varsayılan olarak boştur (başka modlarla çakışmasın), Kontroller > Compass Triangulator'dan atanır: ölçüm kaydet, son ölçümü sil, ölçümleri temizle, Xaero waypoint'i olarak ekle, ayarları aç. Ayarlar Mod Menu'den de açılır.

### Ayarlar

- **Ekranda göster**, **Panel** konumu (sol üst, sağ üst, sol, sağ).
- **Otomatik ölçüm** ve **Aralık** (8-256 blok): pusula verisi varken kaç blokta bir ölçüm alınacağı.
- **Yön:** *Pusula verisi* (varsa veriden, yoksa bakıştan) ya da her zaman *Baktığım yön*. Sunucu iğneyi kendisi çiziyor ve pusuladaki veri iğneyle uyuşmuyorsa ikincisini seç.
- **Kendi ölçümlerin** (±0,2-5°): bakarak alınan ölçümlerin hassasiyeti; ölçümleri tartmak ve hata payı için.

## Sunucularda

Mod sadece sunucunun zaten senin istemcine gönderdiği veriyi okur ve hesap yapar; sunucuya hiçbir şey göndermez. Yine de bazı sunucular avantaj sağlayan istemci modlarını kurallarıyla yasaklar; oynadığın sunucunun (ör. DonutSMP) kurallarına bak.

## Gereksinimler

- Minecraft 26.1.2 (26.1.x), 26.2 veya 26.3, Fabric Loader 0.19+, Java 25
- Fabric API (26.1.2 için 0.155.3+, 26.2 için 0.161.0+, 26.3 için 0.163.0+)
- İsteğe bağlı: Mod Menu, Xaero's Minimap (waypoint eklemek için; 26.6.x)

## Derleme

Java 25 gerekir. Prism Launcher'ın indirdiği Java 25'i de kullanabilirsin:

```
set JAVA_HOME=%APPDATA%\PrismLauncher\java\java-runtime-epsilon
gradlew build
```

Bu Minecraft 26.1.2 jar'ını üretir. 26.2 ve 26.3 için:

```
gradlew build -Pmc=26.2
gradlew build -Pmc=26.3
```

Hazır jar'lar `build/libs/` klasörüne çıkar (`-sources` olmayan dosya), adında Minecraft sürümü yazar: `compasstri-<sürüm>+26.1.2.jar`, `compasstri-<sürüm>+26.2.jar` ve `compasstri-<sürüm>+26.3.jar`. Her derleme jar'ı ayrıca ana klasördeki `builds/<minecraft sürümü>/` klasörüne kopyalar ve oradaki eski sürümünü siler. `gradlew build` ayrıca `src/test` altındaki JUnit testlerini de çalıştırır.

Her sürümün bağımlılık sürümleri `versions/<minecraft>.properties` dosyasındadır. Minecraft sürümleri arasında farklı olan birkaç çağrı (26.2'den itibaren ekranların `Minecraft.gui` üzerinden açılması gibi) `src/mc26.1/java` ve `src/mc26.2/java` altındaki `Compat` sınıfındadır; 26.3 26.2'nin kodunu kullanır. Boş tuş atamaları sabit `-1` yerine oyunun `InputConstants.UNKNOWN` değerini kullanır (26.3'te "tuş yok" değeri 0 oldu).

## Proje yapısı

```
src/main/java/compasstri/
  CompassTriMod.java            giriş noktası, tuşlar, panel ve komut kayıtları
  Triangulator.java             ölçümleri kesiştirir, hata payını hesaplar (oyundan bağımsız, test edilebilir)
  Readings.java                 ölçümler; otomatik ölçüm, hedefi değişen pusulalar için bekleme
  HeldCompass.java              eldeki pusula ve hedefi
  CompassTriConfig.java         ayarlar (config/compasstri.properties)
  client/CompassHud             ekrandaki panel
  client/ScreenCompass          ekranlardaki pusulalar: ipucu satırları ve ekran açıkken ölçüm ve waypoint tuşları
  client/Waypoints              waypoint'in yeri ve adı (satıcı Lodestone fiyat)
  client/XaeroWaypoints         Xaero's Minimap'e waypoint ekler (yalnızca o kuruluysa yüklenir)
  Listing.java                  ilan satırlarından satıcı ve fiyatı okur (oyundan bağımsız, test edilebilir)
  client/CompassTriCommands     /compasstri komutları
  client/CompassTriScreen       ayar ekranı
  client/Texts                  sayı ve mesaj biçimleri
  client/ui/                    ayar ekranı iskeleti; dört modda aynıdır
  compat/ModMenuIntegration
src/mc26.1/java, src/mc26.2/java   Minecraft sürümüne göre değişen birkaç çağrı (Compat)
versions/                      her Minecraft sürümü için bağımlılık sürümleri
src/main/resources/            fabric.mod.json, dil dosyaları (19 dil), ikon
src/test/java/compasstri/      JUnit testleri (üçgenleme, ilan satırları, dil dosyaları)
tools/MakeIcon.java            logoyu üretir (AWT'li bir Java ile: java tools/MakeIcon.java)
```

## Ayar dosyası

`config/compasstri.properties`:

```
hud=true
hudPosition=middle_right
autoRecord=true
minDistance=32
source=auto
angleError=1.0
sellerPattern=
pricePattern=
```

Xaero's Minimap sadece derlemek için kullanılır (`compileOnly`, Modrinth Maven'dan), jar'ın içine girmez; kurulu değilse mod waypoint dışında aynen çalışır.

## Lisans

MIT, `LICENSE` dosyasına bak.
