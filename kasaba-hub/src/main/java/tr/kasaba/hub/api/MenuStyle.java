package tr.kasaba.hub.api;

import java.util.Locale;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Shared visual vocabulary for all Kasaba native menus. */
public final class MenuStyle {
    private MenuStyle(){}
    private static final Pattern NUMBERS=Pattern.compile("(?<![\\p{L}\\d])\\d+(?:[.,:/×–-]\\d+)*(?: sn| dk| gün| Akçe)?");
    private static String norm(String s){return s.toLowerCase(Locale.ROOT).replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');}
    public static TextColor buttonColor(String label){
        String s=norm(label);
        if(s.startsWith("oyuna don")||s.startsWith("yarisa don"))return NamedTextColor.GREEN;
        if(s.contains("kapat")||s.contains("geri")||s.contains("don")||s.contains("vazgec")||s.contains("iptal")||s.contains("ayril")||s.startsWith("cik")||s.contains(" cik")||s.contains("dagit")||s.startsWith("sil ")||s.equals("sil")||s.contains("lobi")||s.equals("burada kal"))return NamedTextColor.RED;
        if(s.contains("bakim")||s.contains("kapali")||s.contains("dolu")||s.contains("kilit"))return NamedTextColor.DARK_GRAY;
        if(s.equals("adani yap"))return TextColor.fromHexString("#8EF0AF");
        if(s.equals("arena"))return TextColor.fromHexString("#FF8080");
        if(s.equals("at yarisi"))return TextColor.fromHexString("#B6F27D");
        if(s.equals("tekne yarisi"))return TextColor.fromHexString("#72DDFF");
        if(s.equals("battle royale"))return TextColor.fromHexString("#FFD166");
        if(s.equals("sans yolu"))return TextColor.fromHexString("#D6A2FF");
        if(s.equals("renk kacisi"))return TextColor.fromHexString("#9DBBFF");
        if(s.equals("vampir koylu"))return TextColor.fromHexString("#FF9CCC");
        if(s.matches("1v1|2v2|solo|ikili|dortlu"))return NamedTextColor.GREEN;
        if(s.equals("x arena")||s.startsWith("savasci")||s.equals("vampir"))return NamedTextColor.RED;
        if(s.startsWith("tank"))return NamedTextColor.BLUE;
        if(s.startsWith("okcu")||s.equals("avci"))return NamedTextColor.GREEN;
        if(s.startsWith("suikastci"))return NamedTextColor.DARK_PURPLE;
        if(s.startsWith("buyucu")||s.contains("karakter")||s.equals("kahin"))return NamedTextColor.LIGHT_PURPLE;
        if(s.startsWith("sifaci")||s.equals("koylu"))return NamedTextColor.YELLOW;
        if(s.contains("vip")||s.contains("akce")||s.contains("market"))return NamedTextColor.GOLD;
        if(s.contains("baslat")||s.contains("onayla")||s.contains("satin al")||s.contains("olustur")||s.contains("katil")||s.equals("oyna")||s.equals("evet")||s.contains("kabul")||s.startsWith("✓"))return NamedTextColor.GREEN;
        if(s.contains("bilgi")||s.contains("nasil")||s.contains("yardim")||s.contains("ozellik")||s.contains("rehber"))return NamedTextColor.YELLOW;
        if(s.contains("oda")||s.contains("liste")||s.contains("sonraki")||s.contains("onceki"))return NamedTextColor.AQUA;
        if(s.contains("arkadas")||s.contains("grup")||s.contains("mesaj"))return NamedTextColor.LIGHT_PURPLE;
        if(s.equals("pas"))return NamedTextColor.YELLOW;
        return NamedTextColor.WHITE;
    }
    public static Component button(String label){return Component.text(label,buttonColor(label));}
    public static Component title(String title){return Component.text(title,NamedTextColor.GOLD).decorate(TextDecoration.BOLD);}
    public static Component line(String text){
        if(text.indexOf('§')>=0)return LegacyComponentSerializer.legacySection().deserialize(text);
        String s=norm(text);NamedTextColor base=s.contains("dikkat")||s.contains("yasak")||s.contains("kaybed")||s.contains("kotu sans")?NamedTextColor.RED:s.contains("kazand")||s.contains("tamamlandi")?NamedTextColor.GREEN:NamedTextColor.GRAY;
        int split=text.indexOf(" • ");Component out=Component.empty();String rest=text;
        if(split>=0){out=out.append(Component.text(text.substring(0,split),NamedTextColor.YELLOW)).append(Component.text(" • ",NamedTextColor.DARK_GRAY));rest=text.substring(split+3);}
        var matcher=NUMBERS.matcher(rest);int end=0;while(matcher.find()){out=out.append(Component.text(rest.substring(end,matcher.start()),base)).append(Component.text(matcher.group(),NamedTextColor.AQUA));end=matcher.end();}
        return out.append(Component.text(rest.substring(end),base));
    }
}
