package tr.kasaba.bilgi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Compact vanilla-font layout; no resource pack, flashing animation or latency field. */
final class PanelView {
    record NativeRow(Component name,int score) {}
    record GameRows(String id,Component title,List<NativeRow> rows) {}
    record Data(String player,Component rank,BigDecimal akce,String clan,long seconds) {}
    record Frame(boolean bright,Component title,List<Component> lines) {}
    private static final TextColor GOLD=rgb(0xE6B956),GRAY=rgb(0xA2ADB8),WHITE=rgb(0xE1E6EC);
    private static final TextColor SOFT=rgb(0x808995),FAINT=rgb(0x596471),DIM_TITLE=rgb(0x8A7D5D);
    private static final PlainTextComponentSerializer PLAIN=PlainTextComponentSerializer.plainText();
    private static TextColor rgb(int value){return TextColor.color(value);}
    static String plain(Component value){return PLAIN.serialize(value);}
    static String time(long seconds){seconds=Math.max(0,seconds);long days=seconds/86400,hours=seconds/3600%24,minutes=seconds/60%60;
        if(days>0)return days+" g "+hours+" sa "+minutes+" dk";
        if(hours>0)return hours+" sa "+minutes+" dk";
        return minutes+" dk "+seconds%60+" sn";
    }
    static String money(BigDecimal value){return value.setScale(2,RoundingMode.HALF_UP).toPlainString().replace('.',',');}
    static String shorten(String text,int max){int count=text.codePointCount(0,text.length());return count<=max?text:text.substring(0,text.offsetByCodePoints(0,max-1))+"…";}
    static Frame render(Data data,boolean bright,GameRows game){
        List<Component> lines=new ArrayList<>();
        if(game!=null&&!game.rows.isEmpty()){
            lines.add(Component.text(shorten(plain(game.title),32),FAINT));
            for(NativeRow row:game.rows.stream().limit(6).toList()){
                String text=plain(row.name);
                // X Arena uses the numeric score for kills. Şans Yolu already prints its own tile/point values.
                if(game.id.equals("kasaba_arena"))text+=" • "+row.score;
                lines.add(Component.text(shorten(text,38),SOFT));
            }
        }
        lines.add(row("▸","Oyuncu",Component.text(data.player,WHITE),0x55BAD4,bright));
        lines.add(row("♛","Rütbe",Component.text(shorten(plain(data.rank),24),data.rank.color()==null?WHITE:data.rank.color()),0xB084D6,bright));
        if(bright)lines.add(Component.empty());
        lines.add(row("◈","Akçe",Component.text(money(data.akce),GOLD),0xD7AD4E,bright));
        lines.add(row("⚑","Klan",Component.text(shorten(data.clan,24),WHITE),0x73C59B,bright));
        if(bright)lines.add(Component.empty());
        lines.add(row("⌛","Süre",Component.text(time(data.seconds),WHITE),0x71B8B8,bright));
        if(bright){lines.add(Component.empty());lines.add(Component.text("──────────────",rgb(0x77633F)));}
        lines.add(Component.text("play.kasabatr.com",bright?GOLD:FAINT));
        lines.add(Component.text("dc: kasabatr",bright?rgb(0xAA8FD1):FAINT));
        lines.add(Component.text("ig: kasabatrminecraft",bright?rgb(0xCD8BA8):FAINT));
        // Even the longest existing match sidebar (six game rows plus title) leaves room for all requested fields.
        if(lines.size()>15)throw new IllegalStateException("Bilgi tablosu 15 satırı geçti.");
        Component title=Component.text(bright?"◆ KASABATR ◆":"KasabaTR",bright?GOLD:DIM_TITLE);
        if(bright)title=title.decorate(TextDecoration.BOLD);
        return new Frame(bright,title,List.copyOf(lines));
    }
    private static Component row(String icon,String label,Component value,int accent,boolean bright){
        if(!bright)return Component.text(icon+" "+label+": ",FAINT).append(Component.text(plain(value),SOFT));
        return Component.text(icon+" ",rgb(accent)).append(Component.text(label+": ",GRAY)).append(value);
    }
}
