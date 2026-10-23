package tr.kasabatr.vampir;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/** Consistent visual hierarchy for native dialogs and the personal task HUD. */
final class GameUi {
    private static final Map<String,NamedTextColor> COLORS=Map.of(
        "Kırmızı",NamedTextColor.RED,"Mavi",NamedTextColor.BLUE,
        "Yeşil",NamedTextColor.GREEN,"Sarı",NamedTextColor.YELLOW);
    private GameUi() {}

    static Component title(String text){return Component.text(text,NamedTextColor.GOLD).decorate(TextDecoration.BOLD);}

    static Component button(String label){
        NamedTextColor color=COLORS.get(label);
        if(color==null)color=label.equals("Kapat")||label.equals("Geri")?NamedTextColor.GRAY
            :label.startsWith("✓")?NamedTextColor.GREEN
            :label.contains("Diziyi")?NamedTextColor.LIGHT_PURPLE
            :label.equals("Pas")?NamedTextColor.YELLOW:NamedTextColor.AQUA;
        return Component.text(label,color);
    }

    private static Component sequence(String text){
        Component result=Component.empty();String[] parts=text.split(" → ",-1);
        for(int i=0;i<parts.length;i++){
            if(i>0)result=result.append(Component.text(" → ",NamedTextColor.DARK_GRAY));
            result=result.append(Component.text(parts[i],COLORS.getOrDefault(parts[i],NamedTextColor.YELLOW)));
        }
        return result;
    }

    static Component line(String text){
        for(String prefix:new String[]{"Hatırla: ","İstenen sıra: ","Şimdi yap: "})
            if(text.startsWith(prefix))return Component.text(prefix,NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD).append(sequence(text.substring(prefix.length())).decoration(TextDecoration.BOLD,false));
        int separator=text.indexOf(" • ");
        if(separator>=0){
            NamedTextColor heading=text.startsWith("Adım ")?NamedTextColor.GREEN
                :text.startsWith("Hafıza turu ")||text.startsWith("Dizi gizli")?NamedTextColor.LIGHT_PURPLE:NamedTextColor.GOLD;
            Component result=Component.text(text.substring(0,separator),heading);
            String rest=text.substring(separator+3);
            NamedTextColor detail=rest.equals("Kolay")?NamedTextColor.GREEN:rest.equals("Orta")?NamedTextColor.YELLOW:NamedTextColor.GRAY;
            return result.append(Component.text(" • ",NamedTextColor.DARK_GRAY)).append(Component.text(rest,detail));
        }
        return Component.text(text,NamedTextColor.GRAY);
    }

    static Component target(String name,String instruction){
        NamedTextColor color=instruction.startsWith("Görev burada")?NamedTextColor.GREEN
            :instruction.contains("kapıyı")?NamedTextColor.YELLOW:NamedTextColor.AQUA;
        return Component.text(name,NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
            .append(Component.text(" • ",NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD,false))
            .append(Component.text(instruction,color).decoration(TextDecoration.BOLD,false));
    }
}
