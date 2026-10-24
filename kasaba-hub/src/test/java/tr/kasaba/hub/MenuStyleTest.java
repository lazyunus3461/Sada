package tr.kasaba.hub;
import org.junit.jupiter.api.Test;
import tr.kasaba.hub.api.MenuStyle;
import net.kyori.adventure.text.format.NamedTextColor;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class MenuStyleTest {
 @Test void exitsActionsAndInformationAreDistinct(){for(String s:List.of("Merkeze dön","Kapat","Ana spawna dön","Odadan ayrıl","Vazgeç"))assertEquals(NamedTextColor.RED,MenuStyle.buttonColor(s));for(String s:List.of("Maçı başlat","Oda oluştur","Katıl"))assertEquals(NamedTextColor.GREEN,MenuStyle.buttonColor(s));assertEquals(NamedTextColor.YELLOW,MenuStyle.buttonColor("Nasıl oynanır?"));assertEquals(NamedTextColor.GOLD,MenuStyle.buttonColor("VIP satın al"));assertEquals(NamedTextColor.AQUA,MenuStyle.buttonColor("Açık odalar"));}
 @Test void eightGamesHaveEightColors(){var colors=new HashSet<>();for(String s:List.of("Adanı Yap","Arena","At Yarışı","Tekne Yarışı","Battle Royale","Şans Yolu","Renk Kaçışı","Vampir Köylü"))colors.add(MenuStyle.buttonColor(s));assertEquals(8,colors.size());}
}
