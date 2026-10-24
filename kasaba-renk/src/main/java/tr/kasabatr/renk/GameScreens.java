package tr.kasabatr.renk;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.util.*;

final class GameScreens {
    record Button(String label,Runnable action){}
    private final KasabaRenkPlugin plugin;private long sequence;private final Map<UUID,Long> pages=new HashMap<>();
    GameScreens(KasabaRenkPlugin plugin){this.plugin=plugin;}
    void clear(UUID id){pages.remove(id);}
    void show(Player p,String title,List<String> lines,List<Button> buttons,boolean lobbyOnly){
        plugin.ready(p);long token=++sequence;pages.put(p.getUniqueId(),token);var actions=new ArrayList<ActionButton>();
        for(Button button:buttons)actions.add(ActionButton.create(tr.kasaba.hub.api.MenuStyle.button(button.label),null,150,DialogAction.customClick((response,audience)->{
            if(!(audience instanceof Player actor)||!actor.getUniqueId().equals(p.getUniqueId())||!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{if(!p.isOnline()||!Objects.equals(pages.get(p.getUniqueId()),token))return;try{plugin.ready(p);if(lobbyOnly&&!plugin.lobby(p.getUniqueId()))throw new IllegalArgumentException("Bu işlem yalnız Renk Kaçışı lobisinde kullanılabilir.");pages.remove(p.getUniqueId());p.closeDialog();button.action.run();}catch(RuntimeException e){plugin.tell(p,e.getMessage()==null?"İşlem tamamlanamadı.":e.getMessage());}});
        },ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(2)).build())));
        p.showDialog(Dialog.create(builder->builder.empty().base(DialogBase.builder(tr.kasaba.hub.api.MenuStyle.title(title)).canCloseWithEscape(true).pause(false).afterAction(DialogBase.DialogAfterAction.NONE).body(lines.stream().<DialogBody>map(line->DialogBody.plainMessage(tr.kasaba.hub.api.MenuStyle.line(line),340)).toList()).build()).type(DialogType.multiAction(actions).columns(2).build())));
    }
}
