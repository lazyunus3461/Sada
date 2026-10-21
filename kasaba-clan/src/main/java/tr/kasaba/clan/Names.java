package tr.kasaba.clan;

import java.text.Normalizer;
import java.util.Locale;

final class Names {
    static String key(String name){
        if(name==null||!name.matches("[a-zA-Z0-9_]{1,16}"))throw new IllegalArgumentException("Oyuncu adı 1–16 harf, rakam veya _ olmalı.");
        return name.toLowerCase(Locale.ROOT);
    }
    static String command(String text){return Normalizer.normalize(text,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replace("\u0307","").replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');}
    static String tag(String value){
        if(value==null||!value.matches("[a-zA-Z0-9ÇĞİÖŞÜçğıöşü]{2,6}"))throw new IllegalArgumentException("Klan etiketi 2–6 harf/rakam olmalı; renk kodu veya boşluk içeremez.");
        return value.toUpperCase(Locale.forLanguageTag("tr"));
    }
    static String tagKey(String value){return command(tag(value));}
    static String label(String value){
        if(value!=null)value=value.strip();
        if(value==null||!value.matches("[\\p{L}\\p{N} _-]{3,32}"))throw new IllegalArgumentException("Klan adı 3–32 harf, rakam, boşluk, _ veya - içermeli.");
        return value;
    }
}
