package tr.kasabatr.boya;
import org.bukkit.*;
enum PaintTeam {
 RED("§cKırmızı",Material.RED_CONCRETE,Color.RED),BLUE("§9Mavi",Material.BLUE_CONCRETE,Color.BLUE);
 final String label;final Material block;final Color color;
 PaintTeam(String label,Material block,Color color){this.label=label;this.block=block;this.color=color;}
}
