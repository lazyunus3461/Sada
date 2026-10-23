package tr.kasaba.battle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

final class LootTable {
    enum Tier{YAYGIN,NADIR,EPIK,EFSANE}
    record Entry(String id,ItemStack item,double weight,int min,int max,Tier tier){Entry{if(id==null||!id.matches("[a-z0-9_-]{1,40}")||item==null||item.getType().isAir()||!Double.isFinite(weight)||weight<=0||min<1||max<min||max>item.getMaxStackSize())throw new IllegalArgumentException("Ganimet adı/ağırlığı/adedi geçersiz.");item=item.clone();}}
    final Map<String,Entry> entries=new LinkedHashMap<>();private final Path file;private final NamespacedKey heal,reserve;
    LootTable(Path file,NamespacedKey heal,NamespacedKey reserve)throws IOException{this.file=file;this.heal=heal;this.reserve=reserve;if(!java.nio.file.Files.exists(file)){defaults();save();}else load();}
    void load()throws IOException{YamlConfiguration d=AtomicYaml.read(file);Map<String,Entry> next=new LinkedHashMap<>();try{var section=d.getConfigurationSection("esyalar");if(section==null)throw new IllegalArgumentException("Ganimet havuzu boş.");for(String id:section.getKeys(false)){String k="esyalar."+id;next.put(id,new Entry(id,d.getItemStack(k+".esya"),d.getDouble(k+".agirlik"),d.getInt(k+".min",1),d.getInt(k+".max",1),Tier.valueOf(d.getString(k+".seviye","YAYGIN"))));}if(next.isEmpty())throw new IllegalArgumentException("Ganimet havuzu boş.");}catch(RuntimeException ex){throw new IOException("Ganimet dosyası bozuk; korundu.",ex);}entries.clear();entries.putAll(next);}
    void save()throws IOException{YamlConfiguration d=new YamlConfiguration();for(Entry e:entries.values()){String k="esyalar."+e.id;d.set(k+".esya",e.item);d.set(k+".agirlik",e.weight);d.set(k+".min",e.min);d.set(k+".max",e.max);d.set(k+".seviye",e.tier.name());}AtomicYaml.write(file,d);}
    void put(Entry e)throws IOException{Entry before=entries.put(e.id,e);try{save();}catch(IOException ex){if(before==null)entries.remove(e.id);else entries.put(e.id,before);throw ex;}}
    void remove(String id)throws IOException{if(entries.size()<=1)throw new IllegalArgumentException("En az bir ganimet kalmalı.");Entry e=entries.remove(id);if(e==null)throw new IllegalArgumentException("Bu ganimet yok.");try{save();}catch(IOException ex){entries.put(id,e);throw ex;}}
    ItemStack healing(int amount){ItemStack item=new ItemStack(Material.COOKIE,amount);ItemMeta meta=item.getItemMeta();meta.setDisplayName("§dŞifa Lokması §7(+2 kalp)");meta.setLore(List.of("§7Yiyince anında 2 kalp yeniler.","§7Yalnızca aktif BR maçında • 10 sn ara"));meta.getPersistentDataContainer().set(heal,PersistentDataType.BYTE,(byte)1);var food=meta.getFood();food.setNutrition(0);food.setSaturation(0);food.setCanAlwaysEat(true);meta.setFood(food);item.setItemMeta(meta);return item;}
    boolean isHealing(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(heal,PersistentDataType.BYTE);}
    ItemStack reserve(int hearts,int amount){
        if(hearts!=2&&hearts!=3)throw new IllegalArgumentException("Yedek kalp 2 veya 3 olmalı.");
        ItemStack item=new ItemStack(hearts==2?Material.DRIED_KELP:Material.SWEET_BERRIES,amount);ItemMeta meta=item.getItemMeta();
        meta.setDisplayName(hearts==2?"§eKoruma Lokması §7(+2 sarı kalp)":"§6Güçlü Koruma Lokması §7(+3 sarı kalp)");
        meta.setLore(List.of("§7Yedek can verir; süreyle kaybolmaz.","§7Bir normal + bir güçlü: en fazla 5 sarı kalp.","§7Darbe bittikten 3 sn sonra; kendi etkisi tükenince tekrar."));
        meta.getPersistentDataContainer().set(reserve,PersistentDataType.INTEGER,hearts);var food=meta.getFood();food.setNutrition(0);food.setSaturation(0);food.setCanAlwaysEat(true);meta.setFood(food);item.setItemMeta(meta);return item;
    }
    int reserveHearts(ItemStack item){if(item==null||!item.hasItemMeta())return 0;int value=item.getItemMeta().getPersistentDataContainer().getOrDefault(reserve,PersistentDataType.INTEGER,0);return value==2||value==3?value:0;}
    List<ItemStack> roll(Random random,boolean rare,int count){
        // More supply points, not three times as much strong equipment.
        List<Entry> pool=entries.values().stream().filter(e->reserveHearts(e.item)==0).collect(java.util.stream.Collectors.toCollection(ArrayList::new));List<ItemStack> result=new ArrayList<>();
        for(int n=0;n<count;n++){
            if((!rare&&random.nextDouble()<0.70)||pool.isEmpty()){
                Material[] basics={Material.ARROW,Material.ARROW,Material.BREAD,Material.BAKED_POTATO,Material.COOKED_BEEF};Material type=basics[random.nextInt(basics.length)];
                result.add(new ItemStack(type,1+random.nextInt(type==Material.ARROW?4:2)));continue;
            }
            double total=pool.stream().mapToDouble(e->weighted(e,rare)).sum(),r=random.nextDouble()*total;Entry chosen=pool.getLast();
            for(Entry e:pool)if((r-=weighted(e,rare))<0){chosen=e;break;}pool.remove(chosen);ItemStack item=chosen.item.clone();
            item.setAmount(chosen.min+random.nextInt(chosen.max-chosen.min+1));result.add(item);
        }
        // Common weapons are easy to find; diamond/netherite still use their rare tier weights.
        if(result.stream().noneMatch(LootTable::isWeapon)&&random.nextDouble()<0.60){
            List<Entry> weapons=entries.values().stream().filter(e->e.tier==Tier.YAYGIN&&isWeapon(e.item)).toList();
            if(!weapons.isEmpty()){ItemStack weapon=weapons.get(random.nextInt(weapons.size())).item.clone();weapon.setAmount(1);result.add(weapon);}
        }
        if(result.stream().anyMatch(i->i.getType()==Material.BOW||i.getType()==Material.CROSSBOW))result.add(new ItemStack(Material.ARROW,4+random.nextInt(5)));
        // One shared roll: 24% TOTAL, at most one reserve food per chest.
        if(random.nextDouble()<0.24)result.add(reserve(random.nextBoolean()?2:3,1));return result;
    }
    static boolean isWeapon(ItemStack item){if(item==null)return false;Material type=item.getType();return type.name().endsWith("_SWORD")||type.name().endsWith("_AXE")||Set.of(Material.BOW,Material.CROSSBOW,Material.TRIDENT,Material.MACE).contains(type);}
    private double weighted(Entry e,boolean rare){return e.weight*switch(e.tier){case YAYGIN->rare?0.25:1;case NADIR->rare?1.5:0.18;case EPIK->rare?1:0.012;case EFSANE->rare?0.15:0.0005;};}
    private void add(String id,Material material,double w,Tier tier,Enchantment ench,int level){ItemStack item=new ItemStack(material);if(ench!=null)item.addUnsafeEnchantment(ench,level);entries.put(id,new Entry(id,item,w,1,1,tier));}
    private void defaults(){
        add("altin-savurma",Material.GOLDEN_SWORD,14,Tier.YAYGIN,Enchantment.KNOCKBACK,1);add("demir-keskinlik",Material.IRON_SWORD,12,Tier.NADIR,Enchantment.SHARPNESS,2);add("elmas-kilic",Material.DIAMOND_SWORD,4,Tier.EPIK,Enchantment.SHARPNESS,3);
        add("yay",Material.BOW,12,Tier.YAYGIN,Enchantment.POWER,1);add("atesli-yay",Material.BOW,5,Tier.NADIR,Enchantment.FLAME,1);add("guclu-yay",Material.BOW,3,Tier.EPIK,Enchantment.POWER,3);
        for(Material m:List.of(Material.IRON_HELMET,Material.IRON_CHESTPLATE,Material.IRON_LEGGINGS,Material.IRON_BOOTS))add(m.name().toLowerCase(Locale.ROOT),m,10,Tier.YAYGIN,Enchantment.PROTECTION,2);
        for(Material m:List.of(Material.DIAMOND_HELMET,Material.DIAMOND_CHESTPLATE,Material.DIAMOND_LEGGINGS,Material.DIAMOND_BOOTS))add(m.name().toLowerCase(Locale.ROOT),m,4,Tier.EPIK,Enchantment.PROTECTION,4);
        add("efsane-zirh",Material.DIAMOND_CHESTPLATE,1,Tier.EFSANE,Enchantment.PROTECTION,5);add("kalkan",Material.SHIELD,8,Tier.NADIR,null,0);
        entries.put("ok",new Entry("ok",new ItemStack(Material.ARROW),18,8,20,Tier.YAYGIN));entries.put("yemek",new Entry("yemek",new ItemStack(Material.COOKED_BEEF),15,3,6,Tier.YAYGIN));entries.put("sifa",new Entry("sifa",healing(1),10,1,2,Tier.NADIR));
        add("tas-kilic",Material.STONE_SWORD,10,Tier.YAYGIN,Enchantment.SHARPNESS,1);add("demir-kilic",Material.IRON_SWORD,12,Tier.YAYGIN,null,0);add("demir-balta",Material.IRON_AXE,9,Tier.YAYGIN,null,0);add("elmas-balta",Material.DIAMOND_AXE,4,Tier.EPIK,Enchantment.SHARPNESS,2);
        add("netherit-kilic",Material.NETHERITE_SWORD,2,Tier.EFSANE,Enchantment.SHARPNESS,2);add("netherit-balta",Material.NETHERITE_AXE,1.5,Tier.EFSANE,Enchantment.SHARPNESS,1);
        for(Material m:List.of(Material.NETHERITE_HELMET,Material.NETHERITE_CHESTPLATE,Material.NETHERITE_LEGGINGS,Material.NETHERITE_BOOTS))add(m.name().toLowerCase(Locale.ROOT),m,2,Tier.EFSANE,Enchantment.PROTECTION,3);
        for(Material m:List.of(Material.CHAINMAIL_HELMET,Material.CHAINMAIL_CHESTPLATE,Material.CHAINMAIL_LEGGINGS,Material.CHAINMAIL_BOOTS))add(m.name().toLowerCase(Locale.ROOT),m,6,Tier.YAYGIN,Enchantment.PROTECTION,1);
        add("arbalet",Material.CROSSBOW,4,Tier.NADIR,Enchantment.QUICK_CHARGE,1);add("delici-arbalet",Material.CROSSBOW,2,Tier.EPIK,Enchantment.PIERCING,2);add("uc-disli-mizrak",Material.TRIDENT,1,Tier.EPIK,Enchantment.LOYALTY,1);add("gurz",Material.MACE,0.4,Tier.EFSANE,null,0);
        entries.put("parlayan-ok",new Entry("parlayan-ok",new ItemStack(Material.SPECTRAL_ARROW),4,3,6,Tier.NADIR));
        for(var effect:List.of(org.bukkit.potion.PotionEffectType.SLOWNESS,org.bukkit.potion.PotionEffectType.WEAKNESS,org.bukkit.potion.PotionEffectType.POISON)){ItemStack arrow=new ItemStack(Material.TIPPED_ARROW);var meta=(org.bukkit.inventory.meta.PotionMeta)arrow.getItemMeta();meta.addCustomEffect(new org.bukkit.potion.PotionEffect(effect,effect==org.bukkit.potion.PotionEffectType.POISON?80:120,0),true);arrow.setItemMeta(meta);arrow.setData(io.papermc.paper.datacomponent.DataComponentTypes.POTION_DURATION_SCALE,1.0f);String id=effect.getKey().getKey();entries.put(id+"-ok",new Entry(id+"-ok",arrow,2,1,3,Tier.EPIK));}
        entries.put("ekmek",new Entry("ekmek",new ItemStack(Material.BREAD),12,2,5,Tier.YAYGIN));entries.put("pismis-domuz",new Entry("pismis-domuz",new ItemStack(Material.COOKED_PORKCHOP),10,2,5,Tier.YAYGIN));entries.put("firin-patates",new Entry("firin-patates",new ItemStack(Material.BAKED_POTATO),10,3,6,Tier.YAYGIN));entries.put("altin-havuc",new Entry("altin-havuc",new ItemStack(Material.GOLDEN_CARROT),3,1,3,Tier.NADIR));add("altin-elma",Material.GOLDEN_APPLE,1,Tier.EPIK,null,0);
    }
}
