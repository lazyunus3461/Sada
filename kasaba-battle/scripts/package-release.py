"""Package fresh generated world and tested production JARs; never copy live server data."""
from pathlib import Path
import hashlib,shutil,zipfile,json
root=Path(__file__).resolve().parents[2];module=root/'kasaba-battle';dist=root/'dist';dist.mkdir(exist_ok=True)
world=module/'target/KasabaBR'
assert (world/'kasaba-br-map.yml').is_file()
assert not any((world/x).exists() for x in ('uid.dat','session.lock','playerdata','stats','advancements'))
jars={'kasaba-battle':'KasabaBattleRoyale-1.0.2.jar','kasaba-hub':'KasabaHub-1.2.0.jar','kasaba-arena':'KasabaArena-1.2.1.jar','kasaba-akce':'KasabaAkce-1.0.0.jar'}
for folder,name in jars.items():
 source=root/folder/'target'/name
 with zipfile.ZipFile(source) as z:
  assert not any('Probe' in p or '/test/' in p for p in z.namelist())
 shutil.copyfile(source,dist/name)
for source,name in [(module/'README.md','KasabaBattleRoyale-KURULUM.txt'),(module/'target/KasabaBR-Sandik-Adaylari.png','KasabaBR-Sandik-Adaylari.png'),(module/'target/map-report.json','KasabaBR-Harita-Raporu.json'),(module/'target/loot-sample.json','KasabaBR-Ganimet-Testi.json')]:shutil.copyfile(source,dist/name)
world_files=sorted(p for p in world.rglob('*') if p.is_file())
with zipfile.ZipFile(dist/'KasabaBattleRoyale-Dunya.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in world_files:z.write(p,p.relative_to(world.parent).as_posix())
 z.write(dist/'KasabaBattleRoyale-KURULUM.txt','KURULUM.txt')
with zipfile.ZipFile(dist/'KasabaBattleRoyale-Kaynak.zip','w',zipfile.ZIP_DEFLATED) as z:
 for folder in ('kasaba-battle','kasaba-hub','src'):
  for p in sorted((root/folder).rglob('*')):
   if p.is_file() and not any(part in {'target','node_modules','__pycache__','.git'} for part in p.relative_to(root).parts):z.write(p,'Sada/'+p.relative_to(root).as_posix())
 z.write(root/'pom.xml','Sada/pom.xml')
with zipfile.ZipFile(dist/'KasabaBattleRoyale-Kurulum.zip','w',zipfile.ZIP_DEFLATED) as z:
 for p in world_files:z.write(p,p.relative_to(world.parent).as_posix())
 for name in (jars['kasaba-battle'],jars['kasaba-hub']):z.write(dist/name,'plugins/'+name)
 for name in ('KasabaBattleRoyale-KURULUM.txt','KasabaBattleRoyale-TEST-SONUCLARI.txt','KasabaBR-Sandik-Adaylari.png','KasabaBR-Harita-Raporu.json','KasabaBR-Ganimet-Testi.json'):z.write(dist/name,name)
combined='''KASABA BR + X ARENA GÜNCELLEMESİ — Paper 1.21.10 / Java 21

1. Sunucuyu durdur. Dünya ve plugins veri klasörlerini yedekle.
2. Eski BattleRoyale ... v0.3.1 veya KasabaBattleRoyale-1.0.0 / 1.0.1, KasabaArena ve KasabaHub JAR dosyalarını plugins dışına çıkar. Aynı eklentinin iki sürümünü bırakma. Veri klasörlerini silme.
3. ZIP içindeki plugins klasörünün JAR dosyalarını sunucunun plugins klasörüne koy.
   KasabaAkce-1.0.0 zaten varsa aynı sürüm; ikinci kopya koyma.
4. ZIP içindeki KasabaBR dünya klasörünü sunucunun ana klasörüne koy.
   server.properties ile yan yana olmalı; plugins içine konmaz.
   Mevcut KasabaBR dünyasının üzerine yazma. Bu paket ilk BR dünya kurulumudur.
5. Sunucuyu yeniden başlat. BR dünyası ve üç arena otomatik tanınır.
6. /br veya /oyunlar git battle ile gir. Lobide pusulaya sağ tıkla Solo/İkili/Dörtlü seç.

X Arena: Mevcut /arena komutların ve kayıtlı yerlerin korunur. Yeni alan kurman gerekmez.
Yetenekler hedef yokken de atılır. Iskalanınca bekleme süresi harcanır.
Alan saldırısının uyarısından çıkabilirsin; büyüler otomatik hedef izlemez.

BR: 10 kişiyle geri sayım, en fazla 20 kişi. Üç eşzamanlı maç.
Normal ve nadir ganimetli sandıklar her maç farklı yerlerde; sandıkların yarısı dış alanda.
Maçta konum göstergesi ve kafa üstü isimleri kapalı; daralan alan mavi/kırmızı sınır duvarıdır.
Şifa Lokması anında 2 kalp, 10 sn kullanım arası.

Ayrıntılar: KasabaBattleRoyale-KURULUM.txt ve KasabaArena-KURULUM.txt.
'''
with zipfile.ZipFile(dist/'Kasaba-BR-XArena-Guncelleme.zip','w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('ILK-BUNU-OKU.txt',combined)
 for p in world_files:z.write(p,p.relative_to(world.parent).as_posix())
 for name in jars.values():z.write(dist/name,'plugins/'+name)
 for name in ('KasabaBattleRoyale-KURULUM.txt','KasabaBattleRoyale-TEST-SONUCLARI.txt','KasabaArena-KURULUM.txt','KasabaArena-TEST-SONUCLARI.txt','KasabaBR-Sandik-Adaylari.png'):z.write(dist/name,name)
update="""KASABA BATTLE ROYALE 1.0.2 GÜNCELLEMESİ

1. Sunucuyu durdur.
2. plugins içindeki KasabaBattleRoyale-1.0.0.jar veya KasabaBattleRoyale-1.0.1.jar dosyasını dışarı çıkar.
3. Bu paketteki plugins/KasabaBattleRoyale-1.0.2.jar dosyasını plugins içine koy.
4. Sunucuyu yeniden başlat.

KasabaBR dünya klasörünü ve plugins/BattleRoyalePrototype veri klasörünü silme.
Harita değiştirme, lobi veya sandık noktası yeniden ayarlama gerekmez.
Diğer Kasaba eklentilerini yeniden kurma.

Sandıklar üç katına çıkarıldı: 2 kişilik testte 36, 10 kişide 45, 20 kişide 90.
İçerikler seyrektir; nadir güçlü ganimet sandıklarının sayısı üçe katlanmaz.
Her sandıkta toplam %30 ihtimalle 2 veya 3 sarı yedek kalp veren tek bir yiyecek çıkar.
Sarı kalpler süreyle kaybolmaz, hasarla tükenir; üst üste birikmez.
Ölümde ve BR'den çıkışta bonus temizlenir. Eski şifa yiyeceği 2 kırmızı kalp yenilemeye devam eder.
Mevcut dünya/iniş/lobi konumları korunur; hazır adalara güvenli dış sandık adayları eklenir.

Maç sırasında oyuncu konum çubuğu ve kafa üstü isimler gizlenir.
Boş elle/elde eşya varken sandık açılır; dışarıda da düzenli sandık çıkar.
Daralan alan mavi/kırmızı sınır duvarı ve üstte sayaç olarak görünür.

Deneme: iki oyuncu /br solo yazar; OP oyuncu /br başlat 1 yazar.
"""
with zipfile.ZipFile(dist/'KasabaBattleRoyale-Guncelleme.zip','w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('ILK-BUNU-OKU.txt',update)
 z.write(dist/jars['kasaba-battle'],'plugins/'+jars['kasaba-battle'])
 z.write(dist/'KasabaBattleRoyale-KURULUM.txt','KasabaBattleRoyale-KURULUM.txt')
artifacts=['KasabaBattleRoyale-Guncelleme.zip',jars['kasaba-battle'],'KasabaBattleRoyale-Dunya.zip','KasabaBattleRoyale-Kaynak.zip','KasabaBattleRoyale-Kurulum.zip','Kasaba-BR-XArena-Guncelleme.zip']
(dist/'KasabaBattleRoyale-SHA256SUMS.txt').write_text(''.join(hashlib.sha256((dist/name).read_bytes()).hexdigest()+'  '+name+'\n' for name in artifacts))
for name in artifacts:print(f'{name}: {(dist/name).stat().st_size:,} bytes')
