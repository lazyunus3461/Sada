#!/usr/bin/env python3
"""Extend the immutable 1.0.0 network release with the two tested game JARs."""
from pathlib import Path
import argparse,hashlib,json,re,zipfile

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--tested-server',type=Path,required=True);args=parser.parse_args()
dist=root/'dist';stem='KasabaTR-Sunucu-Paketi-1.1.0';target=dist/(stem+'.zip');assert not target.exists(),'Published artifacts must not be overwritten'
with zipfile.ZipFile(dist/'KasabaTR-Sunucu-Paketi-1.0.0.zip') as old:
    assert old.testzip() is None
    files={name:old.read(name) for name in old.namelist() if name.startswith('plugins/')}
    manifest=json.loads(old.read('SURUMLER-VE-SHA256.json'))
    network_guide=old.read('KURULUM-VE-KOMUTLAR.txt');network_tests=old.read('TEST-SONUCLARI.txt')
    for entry in manifest.values():
        data=files['plugins/'+entry['jar']]
        assert hashlib.sha256(data).hexdigest()==entry['sha256']
        assert data==(args.tested_server/'plugins'/entry['jar']).read_bytes()
for module,name,version,package in [('ispanya','KasabaIspanya','0.4.0','tr/kasabatr/ispanya/'),('renk','KasabaRenk','1.0.0','tr/kasabatr/renk/')]:
    folder=root/('kasaba-'+module);proof=json.loads((folder/'validation/live-results.json').read_text())
    assert proof['restartRecovery'] and proof['restartWorldAndMultiverse'] and not proof['restartPending']
    jar=folder/'target'/f'{name}-{version}.jar';data=jar.read_bytes();assert data==(args.tested_server/'plugins'/jar.name).read_bytes()
    with zipfile.ZipFile(jar) as archive:
        assert archive.testzip() is None
        yml=archive.read('plugin.yml').decode();assert re.search(r'^name: '+name+r'$',yml,re.M);assert re.search(r'^version: '+re.escape(version)+r'$',yml,re.M)
        for member in archive.namelist():
            assert 'Probe' not in member and not member.startswith('tr/kasaba/test/')
            if member.endswith('.class'):assert member.startswith(package)
        if module=='ispanya':
            source=folder/'src/main/resources/map-template'
            for p in source.rglob('*'):
                if p.is_file():assert archive.read('map-template/'+str(p.relative_to(source)))==p.read_bytes()
    files['plugins/'+jar.name]=data
    manifest[name]={'version':version,'jar':jar.name,'sha256':hashlib.sha256(data).hexdigest(),'bytes':len(data)}
    files[f'BELGELER/{name}-KURULUM.txt']=(folder/'README.md').read_bytes()
    files[f'BELGELER/{name}-TESTLER.txt']=(folder/'validation/test-results.txt').read_bytes()
    (dist/jar.name).write_bytes(data)
summary='''KasabaTR Sunucu Paketi 1.1.0 — yapılanlar

Bu ZIP önceki 1.0.0 paketinin tamamını ve iki yeni oyunu içerir: toplam 11 eklenti.
Paper 1.21.10 / Java 21. Sunucuya yüklemeyi sen yapacaksın.

1. Sunucuyu durdur; plugins ve dünyalarının yedeğini al.
2. Aynı eklentilerin eski JAR'larını kaldır, ZIP'teki plugins klasörünü mevcut
   plugins ile birleştir. Veri klasörlerini ve dünyalarını silme.
3. Başlat. /oyunlar üzerinden İspanya Adası veya Renk Kaçışı'nı seç.

Önceki 1.0.0 güncellemesi kuruluysa yalnız KasabaIspanya-0.4.0.jar ve
KasabaRenk-1.0.0.jar eklemen yeterli. Eski İspanya JAR'ı kalmasın.
İki oyunun haritası otomatik kurulur; .mca taşıma veya /mv import gerekmez.

İSPANYA ADASI
- Ortak lobi, kendi pusulası, 2–4 kişilik odalar, 3 eşzamanlı maç.
- Gönderdiğin 135 kareli harita, renk etkileri ve üç köprü korunur.
- Sıra, otomatik zar, sıralama, eşya iadesi ve yeniden giriş kurtarması.
- /ispanya → odalar; /ispanya oluştur; /ispanya katıl 101; /ispanya başlat.
- /ispanya lobi oyun lobisine, /lobi ana spawna döner.

YENİ OYUN: RENK KAÇIŞI
- Ekrandaki renge koş; diğer zeminler kaybolur. İki hata hakkın vardır.
- İlk hatada sonraki tur geri dönersin; ikinci hatada seyirci olursun.
- Son kalan kazanır; süreler tur tur kısalır. 3 oda, 2–8 kişi.
- /renk → odalar; /renk oda 1; /renk başlat.
- Tek yönetici testi: odayı seçip /renk deneme. Çıkış: /lobi.

ÖNCEKİ İSTEKLER DE PAKETTE
- Arena pusulasında Market yerine Lobi; /vip bilgi ve ana pusulada VIP marketi.
- VIP: 3/7/15/30 gün = 40/75/145/250 Akçe; Arena netherit Korunma III,
  X becerileri -2 saniye, BR 3 yedek kalp/tam envanter/iniş eltrası,
  Vampir Köylü rol tercihi (rol sayıları korunur, talepte kura), VIP fısıltısı.
- Spawn/lobilerde oyuncu gizleme, arkadaşlar, gruplar ve mesajlar.
- Normal arsa 50 Akçe ve en fazla 1; VIP en fazla 2 ücretsiz.
- 15 gün giriş yapmayanın adası fiziksel temizlenip satışa çıkar.
  Eski son-giriş kaydı olmayan sahiplere ilk açılıştan itibaren 15 gün verilir.
- Adalarda redstone makineleri, çağırma yumurtaları ve portal gibi araçlar kapalı.
  Minecraft sekmeleri görünür; yasaklı eşyalar alınamaz/kullanılamaz.
- Aktif oyunda 15 dakikaya 0,20 Akçe; lobide/AFK/seyircide sayılmaz.
- /olta ücretsiz, saatte bir; /sat balık kendi balığını 0,05 Akçeye satar.
  Günlük 15. satışta 24 saat kilidi başlar, bu sürede gelen balık kaldırılır.
- BR: normal oyuncu 9 yuva, VIP tam envanter; aralıklı sandıklar,
  2+3 birleşen yedek kalp, son hasardan 3 saniye sonra tekrar yeme ve hızlı daralma.
- Roller renkli; Rehber uçuş komutu /fly.

Ayrıntılı kılavuzlar ve test kayıtları BELGELER klasöründe.
'''
files['KURULUM-VE-OZET.txt']=summary.encode()
files['BELGELER/ONCEKI-GUNCELLEME-KOMUTLAR.txt']=network_guide
files['BELGELER/ONCEKI-GUNCELLEME-TESTLER.txt']=network_tests
files['SURUMLER-VE-SHA256.json']=(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode()
files['SHA256SUMS.txt']=''.join(hashlib.sha256(data).hexdigest()+'  '+name+'\n' for name,data in files.items()).encode()
assert len(manifest)==11
with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as archive:
    for name,data in files.items():archive.writestr(name,data)
with zipfile.ZipFile(target) as archive:assert archive.testzip() is None and len([n for n in archive.namelist() if n.endswith('.jar')])==11
digest=hashlib.sha256(target.read_bytes()).hexdigest()
(dist/(stem+'-SHA256.txt')).write_text(digest+'  '+target.name+'\n')
(dist/(stem+'-KURULUM.txt')).write_text(summary)
(dist/(stem+'-SURUMLER.json')).write_bytes(files['SURUMLER-VE-SHA256.json'])
print(json.dumps({'archive':str(target),'bytes':target.stat().st_size,'sha256':digest,'plugins':11},indent=2))
