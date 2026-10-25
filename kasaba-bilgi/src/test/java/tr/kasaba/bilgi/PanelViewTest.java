package tr.kasaba.bilgi;
import java.math.BigDecimal;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PanelViewTest {
 private PanelView.Data data(){return new PanelView.Data("Redkit",Component.text("Kurucu"),new BigDecimal("125.40"),"Yok",45240);}
 @Test void formatsPersistedTimeAndExactFractionalCurrency(){assertEquals("0dk 0sn",PanelView.time(-1));assertEquals("1dk 5sn",PanelView.time(65));assertEquals("12sa 34dk",PanelView.time(45240));assertEquals("2g 1sa",PanelView.time(176640));assertEquals("0,20",PanelView.money(new BigDecimal("0.2")));assertEquals("123456789,99",PanelView.money(new BigDecimal("123456789.99")));}
 @Test void quietModeKeepsEveryRequestedFieldAndAllThreeLinks(){var lobby=PanelView.render(data(),true,null);var game=PanelView.render(data(),false,null);assertTrue(game.lines().size()<lobby.lines().size());String text=String.join("\n",game.lines().stream().map(PanelView::plain).toList());for(String required:List.of("Oyuncu: Redkit","Rütbe: Kurucu","Akçe: 125,40","Klan: Yok","Süre: 12sa 34dk","play.kasabatr.com","dc: kasabatr","ig:kasabatrminecraft"))assertTrue(text.contains(required));assertFalse(text.contains("ms"));assertNotEquals(lobby.title(),game.title());}
 @Test void narrowDefaultFontLayoutShortensLongValues(){var data=new PanelView.Data("WWWWWWWWWWWWWWWW",Component.text("En Uzun Yönetici Rolü"),new BigDecimal("5000.60"),"Uzun Klan Adı",234567);for(boolean bright:List.of(true,false)){var f=PanelView.render(data,bright,null);for(var line:f.lines())assertTrue(PixelWidth.width(PanelView.plain(line))<=104,PanelView.plain(line));}assertEquals("2g 17sa",PanelView.time(234567));var game=new PanelView.GameRows("kasaba_arena",Component.text("X Arena"),List.of(new PanelView.NativeRow(Component.text("1. WWWWWWWWWWWWWWWW"),999)));assertTrue(PanelView.plain(PanelView.render(data,false,game).lines().get(1)).endsWith(" • 999"));}
 @Test void sixMatchRowsFitWithoutDroppingPersonalDataAndKillsStayVisible(){var rows=new ArrayList<PanelView.NativeRow>();for(int i=0;i<6;i++)rows.add(new PanelView.NativeRow(Component.text((i+1)+". Test"+i),10-i));var frame=PanelView.render(data(),false,new PanelView.GameRows("kasaba_arena",Component.text("X Arena • Öldürme"),rows));assertEquals(15,frame.lines().size());assertEquals("1. Test0 • 10",PanelView.plain(frame.lines().get(1)));assertTrue(frame.lines().stream().map(PanelView::plain).anyMatch(x->x.contains("ig:kasabatrminecraft")));}
}
