package tr.kasaba.bilgi;
import java.math.BigDecimal;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PanelViewTest {
 private PanelView.Data data(){return new PanelView.Data("Redkit",Component.text("Kurucu"),new BigDecimal("125.40"),"Yok",45240);}
 @Test void formatsPersistedTimeAndExactFractionalCurrency(){assertEquals("0 dk 0 sn",PanelView.time(-1));assertEquals("1 dk 5 sn",PanelView.time(65));assertEquals("12 sa 34 dk",PanelView.time(45240));assertEquals("2 g 1 sa 4 dk",PanelView.time(176640));assertEquals("0,20",PanelView.money(new BigDecimal("0.2")));assertEquals("123456789,99",PanelView.money(new BigDecimal("123456789.99")));}
 @Test void quietModeKeepsEveryRequestedFieldAndAllThreeLinks(){var lobby=PanelView.render(data(),true,null);var game=PanelView.render(data(),false,null);assertTrue(game.lines().size()<lobby.lines().size());String text=String.join("\n",game.lines().stream().map(PanelView::plain).toList());for(String required:List.of("Oyuncu: Redkit","Rütbe: Kurucu","Akçe: 125,40","Klan: Yok","Süre: 12 sa 34 dk","play.kasabatr.com","dc: kasabatr","ig: kasabatrminecraft"))assertTrue(text.contains(required));assertFalse(text.contains("ms"));assertNotEquals(lobby.title(),game.title());}
 @Test void sixMatchRowsFitWithoutDroppingPersonalDataAndKillsStayVisible(){var rows=new ArrayList<PanelView.NativeRow>();for(int i=0;i<6;i++)rows.add(new PanelView.NativeRow(Component.text((i+1)+". Test"+i),10-i));var frame=PanelView.render(data(),false,new PanelView.GameRows("kasaba_arena",Component.text("X Arena • Öldürme"),rows));assertEquals(15,frame.lines().size());assertEquals("1. Test0 • 10",PanelView.plain(frame.lines().get(1)));assertTrue(frame.lines().stream().map(PanelView::plain).anyMatch(x->x.contains("ig: kasabatrminecraft")));}
}
