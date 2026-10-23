package tr.kasaba.social;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

final class Screens {
 record Button(String label,Consumer<io.papermc.paper.dialog.DialogResponseView> action){}
 private final KasabaSocial plugin;private long sequence;private final Map<UUID,Long> pages=new HashMap<>();
 Screens(KasabaSocial plugin){this.plugin=plugin;}
 static Button button(String text,Runnable action){return new Button(text,r->action.run());}
 static DialogInput input(String key,String label,int limit){return DialogInput.text(key,Component.text(label,NamedTextColor.AQUA)).width(280).maxLength(limit).build();}
 void show(Player p,String title,List<String> lines,List<DialogInput> inputs,List<Button> buttons,boolean social){
  plugin.ready(p,social);long token=++sequence;pages.put(p.getUniqueId(),token);var actions=new ArrayList<ActionButton>();
  for(var button:buttons)actions.add(ActionButton.create(Component.text(button.label,NamedTextColor.AQUA),null,150,DialogAction.customClick((response,audience)->{
   if(!(audience instanceof Player actor)||!actor.getUniqueId().equals(p.getUniqueId())||!plugin.isEnabled())return;
   Bukkit.getScheduler().runTask(plugin,()->{if(!p.isOnline()||!Objects.equals(pages.get(p.getUniqueId()),token))return;try{plugin.ready(p,social);pages.remove(p.getUniqueId());p.closeDialog();button.action.accept(response);}catch(RuntimeException e){plugin.tell(p,e.getMessage()==null?"İşlem tamamlanamadı.":e.getMessage());}});
  },ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(2)).build())));
  p.showDialog(Dialog.create(builder->builder.empty().base(DialogBase.builder(Component.text(title,NamedTextColor.GOLD)).canCloseWithEscape(true).pause(false).afterAction(DialogBase.DialogAfterAction.NONE).inputs(inputs).body(lines.stream().<DialogBody>map(line->DialogBody.plainMessage(Component.text(line,NamedTextColor.GRAY),340)).toList()).build()).type(DialogType.multiAction(actions).columns(2).build())));
 }
}
