package tr.kasaba.spawn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Bell;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** Builds a fixed, symmetric 64 by 64 town plaza after checking the whole site. */
public final class KasabaSpawn extends JavaPlugin implements CommandExecutor {
    private final Set<UUID> busyWorlds = new HashSet<>();
    private final Map<UUID, BukkitTask> builds = new HashMap<>();

    private enum Shape { PLAIN, LEAF, NORTH_STAIR, SOUTH_STAIR, EAST_STAIR, WEST_STAIR, NORTH_DOOR_BOTTOM, NORTH_DOOR_TOP, SOUTH_DOOR_BOTTOM, SOUTH_DOOR_TOP, EAST_DOOR_BOTTOM, EAST_DOOR_TOP, WEST_DOOR_BOTTOM, WEST_DOOR_TOP, NORTH_SIGN, SOUTH_SIGN, EAST_SIGN, WEST_SIGN, CEILING_BELL }
    private enum Face { NORTH, SOUTH, EAST, WEST }
    private record Pos(int x, int y, int z) {}
    private record Placement(Pos pos, Material material, Shape shape, String signText) {}
    private record Road(double ax, double az, double bx, double bz) {
        double distance(double x,double z){double dx=bx-ax,dz=bz-az,denom=dx*dx+dz*dz;double t=denom==0?0:Math.clamp(((x-ax)*dx+(z-az)*dz)/denom,0,1);return Math.hypot(x-(ax+t*dx),z-(az+t*dz));}
    }
    private record House(int x,int z,int width,int depth,Face front,Material wall,Material wood,Material roof,Material stairs,Material window,Material awning,String name) {}

    @Override public void onEnable(){Objects.requireNonNull(getCommand("kasabaspawn")).setExecutor(this);}
    @Override public void onDisable(){builds.values().forEach(BukkitTask::cancel);builds.clear();busyWorlds.clear();}

    private void message(CommandSender sender,String value){sender.sendMessage(Component.text(value,NamedTextColor.GOLD));}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player player)){message(sender,"Bu komutu oyunda, kasaba kurulacak alanın merkezinde kullan.");return true;}
        if(!player.hasPermission("kasabaspawn.yonet")){message(player,"Bu işlem için OP yetkisi gerekir.");return true;}
        if(args.length==0||args[0].equalsIgnoreCase("bilgi")){
            message(player,"Kasaba Spawn 64×64 • /kasabaspawn yap (merkezinde durduğun düz, boş alan)");
            message(player,"Meydandaki çeşmenin güney kenarı spawn noktası olur. Kurulum mevcut zeminin üstünü düzenler.");return true;
        }
        if(!args[0].equalsIgnoreCase("yap")||args.length!=1){message(player,"Kullanım: /kasabaspawn yap | bilgi");return true;}
        begin(player);return true;
    }

    private void begin(Player player){
        World world=player.getWorld();UUID worldId=world.getUID();
        if(!busyWorlds.add(worldId)){message(player,"Bu dünyada bir kasaba kurulumu zaten sürüyor.");return;}
        int cx=player.getLocation().getBlockX(),cz=player.getLocation().getBlockZ(),groundY=player.getLocation().getBlockY()-1;
        int minX=cx-32,maxX=cx+31,minZ=cz-32,maxZ=cz+31;
        if(groundY<world.getMinHeight()+1||groundY+17>=world.getMaxHeight()){
            busyWorlds.remove(worldId);message(player,"Burası yapı için çok yüksek veya alçak. Güvenli, düz bir zemin seç.");return;
        }
        int[][] corners={{minX,minZ},{minX,maxZ},{maxX,minZ},{maxX,maxZ}};
        for(int[] c:corners)if(!world.getWorldBorder().isInside(new Location(world,c[0]+.5,groundY,c[1]+.5))){
            busyWorlds.remove(worldId);message(player,"64×64 kasaba dünya sınırının içine sığmıyor.");return;
        }
        message(player,"Alan hazırlanıyor. 64×64 zemini ve üstündeki 16 bloğu boş bırak; başka oyuncular da alandan çıksın.");

        List<CompletableFuture<Chunk>> loading=new ArrayList<>();
        try{
            int minChunkX=Math.floorDiv(minX,16),maxChunkX=Math.floorDiv(maxX,16),minChunkZ=Math.floorDiv(minZ,16),maxChunkZ=Math.floorDiv(maxZ,16);
            for(int x=minChunkX;x<=maxChunkX;x++)for(int z=minChunkZ;z<=maxChunkZ;z++)loading.add(world.getChunkAtAsync(x,z,true));
        }catch(RuntimeException ex){busyWorlds.remove(worldId);getLogger().warning("Dünya bölgesi hazırlanamadı: "+ex.getMessage());message(player,"Dünya bölgesi yüklenemedi. Alanı tekrar dene.");return;}

        CompletableFuture<Void> ready=CompletableFuture.allOf(loading.toArray(CompletableFuture[]::new));
        AtomicBoolean completed=new AtomicBoolean();
        Bukkit.getScheduler().runTaskLater(this,()->{
            if(completed.compareAndSet(false,true)){busyWorlds.remove(worldId);message(player,"Alan 15 saniyede hazırlanamadı. Tekrar deneyebilirsin.");}
        },300);
        ready.whenComplete((nothing,error)->{
            if(!isEnabled())return;
            Bukkit.getScheduler().runTask(this,()->{
                if(!completed.compareAndSet(false,true))return;
                if(error!=null){busyWorlds.remove(worldId);message(player,"Dünya bölgesi hazırlanamadı. Tekrar dene.");return;}
                try{
                    validateSite(player,world,cx,groundY,minX,maxX,minZ,maxZ);
                    TownPlan plan=new TownPlan(cx,groundY,cz);List<Placement> placements=plan.build();
                    message(player,"Başlıyoruz! Çeşme, 5 bina, bahçeler ve surlar kuruluyor (~"+placements.size()+" blok).");
                    build(player,world,worldId,plan.spawn(world),placements);
                }catch(IllegalArgumentException ex){busyWorlds.remove(worldId);message(player,ex.getMessage());}
            });
        });
    }

    private void validateSite(Player owner,World world,int cx,int y,int minX,int maxX,int minZ,int maxZ){
        for(Player other:world.getPlayers())if(!other.equals(owner)&&other.getLocation().getBlockX()>=minX&&other.getLocation().getBlockX()<=maxX&&other.getLocation().getBlockZ()>=minZ&&other.getLocation().getBlockZ()<=maxZ)
            throw new IllegalArgumentException("Diğer oyuncular da 64×64 alandan çıkmalı: "+other.getName());
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
            Material ground=world.getBlockAt(x,y,z).getType();
            if(!ground.isSolid()||ground==Material.BEDROCK||ground==Material.MAGMA_BLOCK||ground==Material.LAVA||ground==Material.WATER||ground==Material.POWDER_SNOW||ground==Material.CACTUS)
                throw new IllegalArgumentException("Düz, sağlam zemin bulunamadı ("+x+", "+y+", "+z+"). Zemini düzleyip tekrar dene.");
            for(int height=1;height<=16;height++){
                Block block=world.getBlockAt(x,y+height,z);
                if(!block.isPassable()||block.isLiquid())throw new IllegalArgumentException("64×64 alanın üstündeki 16 bloğu boşalt (ilk engel: "+x+", "+(y+height)+", "+z+"). Hiçbir blok değiştirilmedi.");
            }
        }
    }

    private void build(Player owner,World world,UUID worldId,Location spawn,List<Placement> placements){
        int[] index={0};
        BukkitTask task=Bukkit.getScheduler().runTaskTimer(this,()->{
            int stop=Math.min(index[0]+350,placements.size());
            try{
                while(index[0]<stop){Placement placement=placements.get(index[0]++);Block block=world.getBlockAt(placement.pos().x(),placement.pos().y(),placement.pos().z());block.setType(placement.material(),false);apply(block,placement.shape(),placement.signText());}
                if(index[0]>=placements.size()){
                    BukkitTask self=builds.remove(worldId);if(self!=null)self.cancel();busyWorlds.remove(worldId);
                    world.setSpawnLocation(spawn.getBlockX(),spawn.getBlockY(),spawn.getBlockZ());
                    if(owner.isOnline()&&owner.getWorld().equals(world))owner.teleport(spawn);
                    message(owner,"Kasaba meydanı hazır! Dünya doğma yeri çeşmenin güneyinde ayarlandı.");
                    message(owner,"KasabaHub kullanıyorsan girişte /merkez lobi ayarla; kayıt noktası için /kayityeri ayarla yaz.");
                }
            }catch(RuntimeException ex){
                BukkitTask self=builds.remove(worldId);if(self!=null)self.cancel();busyWorlds.remove(worldId);getLogger().severe("Kasaba yapımı "+index[0]+" blokta durdu: "+ex.getMessage());message(owner,"Kurulum yarıda kesildi; dünya/konsol günlüğünde sebep var. Kısmi haritanın üzerinden tekrar komut verme.");
            }
        },1,1);
        builds.put(worldId,task);
    }
    private void apply(Block block,Shape shape,String label){
        if(shape==Shape.PLAIN)return;
        BlockData data=block.getBlockData();
        if(shape.name().endsWith("_STAIR")){Stairs stairs=(Stairs)data;stairs.setFacing(switch(shape){case NORTH_STAIR->org.bukkit.block.BlockFace.NORTH;case SOUTH_STAIR->org.bukkit.block.BlockFace.SOUTH;case EAST_STAIR->org.bukkit.block.BlockFace.EAST;default->org.bukkit.block.BlockFace.WEST;});stairs.setShape(Stairs.Shape.STRAIGHT);block.setBlockData(stairs,false);return;}
        if(shape.name().endsWith("_DOOR_BOTTOM")||shape.name().endsWith("_DOOR_TOP")){
            Directional directional=(Directional)data;directional.setFacing(switch(shape){case NORTH_DOOR_BOTTOM,NORTH_DOOR_TOP->org.bukkit.block.BlockFace.NORTH;case SOUTH_DOOR_BOTTOM,SOUTH_DOOR_TOP->org.bukkit.block.BlockFace.SOUTH;case EAST_DOOR_BOTTOM,EAST_DOOR_TOP->org.bukkit.block.BlockFace.EAST;default->org.bukkit.block.BlockFace.WEST;});
            Bisected bisected=(Bisected)data;bisected.setHalf(shape.name().endsWith("_BOTTOM")?Bisected.Half.BOTTOM:Bisected.Half.TOP);if(data instanceof Openable openable)openable.setOpen(false);block.setBlockData(data,false);return;
        }
        if(shape.name().endsWith("_SIGN")&&data instanceof WallSign wallSign){
            wallSign.setFacing(switch(shape){case NORTH_SIGN->org.bukkit.block.BlockFace.NORTH;case SOUTH_SIGN->org.bukkit.block.BlockFace.SOUTH;case EAST_SIGN->org.bukkit.block.BlockFace.EAST;default->org.bukkit.block.BlockFace.WEST;});block.setBlockData(wallSign,false);
            if(label!=null&&block.getState() instanceof Sign sign){var front=sign.getSide(Side.FRONT);front.line(0,Component.text("✦ "+label+" ✦",NamedTextColor.GOLD));sign.setWaxed(true);sign.update(true,false);}return;
        }
        if(shape==Shape.CEILING_BELL&&data instanceof Bell bell){bell.setAttachment(Bell.Attachment.CEILING);block.setBlockData(bell,false);}
        if(shape==Shape.LEAF&&data instanceof org.bukkit.block.data.type.Leaves leaves){leaves.setPersistent(true);leaves.setDistance(1);block.setBlockData(leaves,false);}
    }

    private static final class TownPlan {
        private final int cx,y,cz;
        private final LinkedHashMap<Pos,Placement> blocks=new LinkedHashMap<>();
        private final List<Road> roads=List.of(new Road(0,0,0,-15),new Road(0,0,-13,-9),new Road(0,0,13,-9),new Road(0,0,-13,12),new Road(0,0,13,12),new Road(0,0,0,30));
        private TownPlan(int cx,int y,int cz){this.cx=cx;this.y=y;this.cz=cz;}
        private Location spawn(World world){return new Location(world,cx+.5,y+1,cz+9.5,180,0);}
        private void put(int x,int h,int z,Material type){put(x,h,z,type,Shape.PLAIN,null);}
        private void put(int x,int h,int z,Material type,Shape shape){put(x,h,z,type,shape,null);}
        private void put(int x,int h,int z,Material type,Shape shape,String text){Pos pos=new Pos(cx+x,y+h,cz+z);blocks.put(pos,new Placement(pos,type,shape,text));}
        private void fill(int x1,int y1,int z1,int x2,int y2,int z2,Material material){for(int x=x1;x<=x2;x++)for(int z=z1;z<=z2;z++)for(int h=y1;h<=y2;h++)put(x,h,z,material);}
        private void line(int x1,int z1,int x2,int z2,Material material){int steps=Math.max(Math.abs(x2-x1),Math.abs(z2-z1));for(int i=0;i<=steps;i++){int x=(int)Math.round(x1+(x2-x1)*(i/(double)Math.max(1,steps))),z=(int)Math.round(z1+(z2-z1)*(i/(double)Math.max(1,steps)));for(int ox=-1;ox<=1;ox++)for(int oz=-1;oz<=1;oz++)put(x+ox,0,z+oz,material);}}
        private boolean building(int x,int z){return inside(x,z,0,-20,15,11)||inside(x,z,-20,-9,11,11)||inside(x,z,20,-9,11,11)||inside(x,z,-20,12,13,11)||inside(x,z,20,12,13,11);}
        private boolean inside(int x,int z,int bx,int bz,int w,int d){return Math.abs(x-bx)<=w/2&&Math.abs(z-bz)<=d/2;}
        private boolean plaza(int x,int z){return Math.max(Math.abs(x),Math.abs(z))<=12;}
        private boolean road(int x,int z){if(Math.abs(x)<=2||Math.abs(z)<=2)return true;for(Road r:roads)if(r.distance(x,z)<=2.15)return true;return false;}
        private Material floor(int x,int z){
            if(building(x,z))return Material.DARK_OAK_PLANKS;
            if(plaza(x,z)){int cheb=Math.max(Math.abs(x),Math.abs(z));if(cheb==11)return Material.CUT_SANDSTONE;if(cheb==10)return Material.ORANGE_TERRACOTTA;if((Math.abs(x)==6&&Math.abs(z)<=9)||(Math.abs(z)==6&&Math.abs(x)<=9))return Material.POLISHED_DIORITE;return Material.SMOOTH_SANDSTONE;}
            if(road(x,z))return (Math.abs(x+z)%11==0)?Material.ANDESITE:Material.POLISHED_ANDESITE;
            if(Math.abs(x)==31||Math.abs(z)==31)return Material.STONE_BRICKS;
            if((Math.abs(x)>25||Math.abs(z)>25)&&((x+z)&3)==0)return Material.MOSS_BLOCK;
            return Material.GRASS_BLOCK;
        }
        private List<Placement> build(){
            for(int x=-32;x<32;x++)for(int z=-32;z<32;z++)put(x,0,z,floor(x,z));
            buildings();fountain();marketStalls();landscaping();wall();towers();lamps();
            return List.copyOf(blocks.values());
        }
        private void buildings(){
            house(new House(0,-20,15,11,Face.SOUTH,Material.SMOOTH_SANDSTONE,Material.STRIPPED_DARK_OAK_LOG,Material.DARK_PRISMARINE,Material.DARK_OAK_STAIRS,Material.LIGHT_BLUE_STAINED_GLASS,Material.RED_WOOL,"Kasaba Konağı"));
            house(new House(-20,-9,11,11,Face.EAST,Material.WHITE_TERRACOTTA,Material.STRIPPED_SPRUCE_LOG,Material.BRICKS,Material.BRICK_STAIRS,Material.BROWN_STAINED_GLASS,Material.RED_WOOL,"Fırın"));
            house(new House(20,-9,11,11,Face.WEST,Material.POLISHED_ANDESITE,Material.STRIPPED_DARK_OAK_LOG,Material.DEEPSLATE_BRICKS,Material.DEEPSLATE_BRICK_STAIRS,Material.ORANGE_STAINED_GLASS,Material.ORANGE_WOOL,"Demirci"));
            house(new House(-20,12,13,11,Face.EAST,Material.MANGROVE_PLANKS,Material.STRIPPED_MANGROVE_LOG,Material.WARPED_PLANKS,Material.WARPED_STAIRS,Material.PINK_STAINED_GLASS,Material.LIME_WOOL,"Han"));
            house(new House(20,12,13,11,Face.WEST,Material.SANDSTONE,Material.STRIPPED_ACACIA_LOG,Material.CUT_SANDSTONE,Material.SANDSTONE_STAIRS,Material.YELLOW_STAINED_GLASS,Material.YELLOW_WOOL,"Pazar"));
        }
        private void house(House h){
            int hx=h.width()/2,hz=h.depth()/2,wallTop=5;
            for(int x=h.x()-hx-1;x<=h.x()+hx+1;x++)for(int z=h.z()-hz-1;z<=h.z()+hz+1;z++){
                put(x,0,z,Material.DARK_OAK_PLANKS);
                if(Math.abs(x)==hx+1||Math.abs(z)==hz+1){put(x,1,z,Material.COBBLED_DEEPSLATE);put(x,2,z,Material.CUT_COPPER);}
            }
            for(int height=1;height<=wallTop;height++)for(int x=h.x()-hx;x<=h.x()+hx;x++)for(int z=h.z()-hz;z<=h.z()+hz;z++){
                boolean edge=Math.abs(x-h.x())==hx||Math.abs(z-h.z())==hz;
                if(edge)put(x,height,z,Material.SMOOTH_SANDSTONE);
            }
            for(int x:new int[]{h.x()-hx,h.x()+hx})for(int z:new int[]{h.z()-hz,h.z()+hz})for(int height=1;height<=wallTop;height++)put(x,height,z,h.wood());
            windows(h);
            int dx=switch(h.front()){case EAST->hx;case WEST->-hx;default->0;},dz=switch(h.front()){case SOUTH->hz;case NORTH->-hz;default->0;};
            int doorX=h.x()+dx,doorZ=h.z()+dz;
            Shape lower=switch(h.front()){case NORTH->Shape.NORTH_DOOR_BOTTOM;case SOUTH->Shape.SOUTH_DOOR_BOTTOM;case EAST->Shape.EAST_DOOR_BOTTOM;case WEST->Shape.WEST_DOOR_BOTTOM;};
            Shape upper=switch(h.front()){case NORTH->Shape.NORTH_DOOR_TOP;case SOUTH->Shape.SOUTH_DOOR_TOP;case EAST->Shape.EAST_DOOR_TOP;case WEST->Shape.WEST_DOOR_TOP;};
            put(doorX,1,doorZ,Material.SPRUCE_DOOR,lower);put(doorX,2,doorZ,Material.SPRUCE_DOOR,upper);
            Shape signShape=switch(h.front()){case NORTH->Shape.NORTH_SIGN;case SOUTH->Shape.SOUTH_SIGN;case EAST->Shape.EAST_SIGN;case WEST->Shape.WEST_SIGN;};put(doorX,4,doorZ,Material.OAK_WALL_SIGN,signShape,h.name());
            for(int ox=-1;ox<=1;ox++)put(doorX-((h.front()==Face.EAST||h.front()==Face.WEST)?0:ox),1,doorZ-((h.front()==Face.NORTH||h.front()==Face.SOUTH)?0:ox),h.wood());
            roof(h,wallTop);
            awning(h,doorX,doorZ,dx,dz);
            put(h.x()-hx+1,1,h.z()+hz-1,Material.BARREL);put(h.x()+hx-1,1,h.z()+hz-1,Material.FLOWER_POT);
            put(h.x()-hx+1,2,h.z()+hz-1,Material.LANTERN);put(h.x()+hx-1,2,h.z()+hz-1,Material.LANTERN);
        }
        private void windows(House h){
            int hx=h.width()/2,hz=h.depth()/2;
            for(int x=h.x()-hx+2;x<=h.x()+hx-2;x++)for(int side:new int[]{-1,1}){
                int z=h.z()+side*hz;if(Math.abs(x-h.x())<2&&h.front()==(side<0?Face.NORTH:Face.SOUTH))continue;
                put(x,3,z,Material.SMOOTH_QUARTZ);put(x,4,z,h.window());put(x,5,z,Material.SMOOTH_QUARTZ);
            }
            for(int z=h.z()-hz+2;z<=h.z()+hz-2;z++)for(int side:new int[]{-1,1}){
                int x=h.x()+side*hx;if(Math.abs(z-h.z())<2&&h.front()==(side<0?Face.WEST:Face.EAST))continue;
                put(x,3,z,Material.SMOOTH_QUARTZ);put(x,4,z,h.window());put(x,5,z,Material.SMOOTH_QUARTZ);
            }
        }
        private void roof(House h,int wallTop){
            int hx=h.width()/2+1,hz=h.depth()/2+1;
            for(int dz=-hz;dz<=hz;dz++){
                int height=wallTop+1+hz-Math.abs(dz);Shape shape=dz<0?Shape.SOUTH_STAIR:Shape.NORTH_STAIR;
                for(int x=h.x()-hx;x<=h.x()+hx;x++)put(x,height,h.z()+dz,h.stairs(),shape);
            }
            int ridge=wallTop+1+hz;
            for(int x=h.x()-hx;x<=h.x()+hx;x++)put(x,ridge,h.z(),h.roof());
            for(int end:new int[]{-1,1}){
                int z=h.z()+end*(hz+1);for(int height=1;height<=3;height++){
                    put(h.x()-1,wallTop+height,z,Material.STRIPPED_SPRUCE_LOG);put(h.x()+1,wallTop+height,z,Material.STRIPPED_SPRUCE_LOG);
                }
                put(h.x(),wallTop+2,z,h.window());put(h.x(),wallTop+3,z,Material.DARK_OAK_PLANKS);
            }
        }
        private void awning(House h,int doorX,int doorZ,int dx,int dz){
            for(int side=-2;side<=2;side++)for(int forward=1;forward<=3;forward++){
                int x=doorX+dx*forward+(dz!=0?side:0),z=doorZ+dz*forward+(dx!=0?side:0);
                Material cloth=(side%2==0)?h.awning():Material.WHITE_WOOL;
                put(x,4,z,cloth);
            }
            for(int side:new int[]{-2,2}){
                int x=doorX+dx*2+(dz!=0?side:0),z=doorZ+dz*2+(dx!=0?side:0);
                for(int height=1;height<=3;height++)put(x,height,z,Material.SPRUCE_FENCE);
            }
            int outsideX=doorX+dx,outsideZ=doorZ+dz;
            for(int step=0;step<=2;step++)put(doorX+dx*(step+1),0,doorZ+dz*(step+1),step==0?Material.STONE_BRICK_STAIRS:Material.POLISHED_ANDESITE);
        }
        private void fountain(){
            for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
                int edge=Math.max(Math.abs(x),Math.abs(z));
                if(edge==5)put(x,1,z,Material.CUT_COPPER);
                if(edge==4)put(x,1,z,Material.PRISMARINE_BRICKS);
                if(edge<4)put(x,1,z,Material.WATER);
                if((Math.abs(x)==4&&Math.abs(z)<=2)||(Math.abs(z)==4&&Math.abs(x)<=2))put(x,2,z,Material.SEA_LANTERN);
            }
            put(0,2,0,Material.PRISMARINE_BRICKS);put(0,3,0,Material.PRISMARINE_BRICKS);put(0,4,0,Material.SEA_LANTERN);put(0,5,0,Material.END_ROD);
            for(int[] p:new int[][]{{-3,-3},{3,-3},{-3,3},{3,3}}){put(p[0],2,p[1],Material.CUT_COPPER);put(p[0],3,p[1],Material.LANTERN);}
            for(int[] p:new int[][]{{-7,0},{7,0},{0,-7},{0,7}}){put(p[0],1,p[1],Material.CHISELED_SANDSTONE);put(p[0],2,p[1],Material.LANTERN);}
        }
        private void marketStalls(){
            stall(-12,-12,Material.RED_WOOL);stall(12,-12,Material.CYAN_WOOL);stall(-12,14,Material.LIME_WOOL);stall(12,14,Material.YELLOW_WOOL);
        }
        private void stall(int x,int z,Material color){
            put(x,1,z,Material.BARREL);put(x,1,z+1,Material.CHEST);put(x,1,z-1,Material.CRAFTING_TABLE);
            for(int dx:new int[]{-2,2})for(int dz:new int[]{-2,2})for(int h=2;h<=4;h++)put(x+dx,h,z+dz,Material.SPRUCE_FENCE);
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)put(x+dx,4,z+dz,((dx+dz)&1)==0?color:Material.WHITE_WOOL);
            for(int dx:new int[]{-1,1}){put(x+dx,2,z,Material.LANTERN);put(x+dx,3,z,Material.IRON_CHAIN);}
        }
        private void landscaping(){
            tree(-12,-22);tree(12,-22);tree(-12,22);tree(12,22);
            bench(-9,0,Face.EAST);bench(9,0,Face.WEST);bench(0,-9,Face.SOUTH);bench(-8,18,Face.NORTH);
            flowerbed(-10,-16);flowerbed(10,-16);flowerbed(-10,19);flowerbed(10,19);
            for(int[] p:new int[][]{{-15,-15},{15,-15},{-15,19},{15,19}}){put(p[0],1,p[1],Material.COBBLESTONE_WALL);put(p[0],2,p[1],Material.LANTERN);}
        }
        private void tree(int x,int z){
            for(int h=1;h<=5;h++)put(x,h,z,Material.STRIPPED_OAK_LOG);
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int h=3;h<=6;h++)if(Math.abs(dx)+Math.abs(dz)+(h==6?1:0)<=4&&!(dx==0&&dz==0&&h<6))put(x+dx,h,z+dz,Material.OAK_LEAVES,Shape.LEAF);
            put(x,7,z,Material.OAK_LEAVES,Shape.LEAF);
        }
        private void bench(int x,int z,Face facing){
            Shape shape=switch(facing){case NORTH->Shape.NORTH_STAIR;case SOUTH->Shape.SOUTH_STAIR;case EAST->Shape.EAST_STAIR;case WEST->Shape.WEST_STAIR;};
            put(x-1,1,z,Material.SPRUCE_STAIRS,shape);put(x,1,z,Material.SPRUCE_STAIRS,shape);put(x+1,1,z,Material.SPRUCE_STAIRS,shape);
            put(x-1,2,z,Material.SPRUCE_FENCE);put(x+1,2,z,Material.SPRUCE_FENCE);
        }
        private void flowerbed(int x,int z){
            for(int dx=0;dx<5;dx++)for(int dz=0;dz<3;dz++)if(dx==0||dx==4||dz==0||dz==2)put(x+dx,1,z+dz,Material.MOSSY_COBBLESTONE);
            Material[] flowers={Material.POPPY,Material.CORNFLOWER,Material.AZURE_BLUET,Material.OXEYE_DAISY,Material.ORANGE_TULIP,Material.PINK_TULIP};int i=0;
            for(int dx=1;dx<4;dx++)for(int dz=1;dz<2;dz++)put(x+dx,1,z+dz,flowers[Math.floorMod(x*17+z*7+i++,flowers.length)]);
        }
        private void wall(){
            for(int t=-31;t<=31;t++)for(int side:new int[]{-31,31}){
                boolean gate=Math.abs(t)<=3;
                if(!gate){put(t,1,side,Material.STONE_BRICK_WALL);if(Math.floorMod(t,4)==0){put(t,2,side,Material.MOSSY_STONE_BRICKS);put(t,3,side,Material.LANTERN);}}
                if(!gate){put(side,1,t,Material.STONE_BRICK_WALL);if(Math.floorMod(t,4)==0){put(side,2,t,Material.MOSSY_STONE_BRICKS);put(side,3,t,Material.LANTERN);}}
            }
            for(int[] gate:new int[][]{{0,-31},{0,31},{-31,0},{31,0}})arch(gate[0],gate[1]);
        }
        private void arch(int x,int z){
            boolean northSouth=Math.abs(z)==31;int axis=northSouth?x:z;
            for(int offset:new int[]{-4,4})for(int h=1;h<=5;h++)put(northSouth?x+offset:x, h,northSouth?z:z+offset, h==5?Material.CUT_COPPER:Material.CHISELED_STONE_BRICKS);
            for(int offset=-4;offset<=4;offset++)put(northSouth?x+offset:x,5,northSouth?z:z+offset,offset==0?Material.GOLD_BLOCK:Material.CUT_COPPER);
            put(x,4,z,Material.SEA_LANTERN);
        }
        private void towers(){
            for(int x:new int[]{-28,28})for(int z:new int[]{-28,28}){
                for(int h=1;h<=6;h++)for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(Math.abs(dx)==2||Math.abs(dz)==2)put(x+dx,h,z+dz,h==1?Material.COBBLED_DEEPSLATE:Material.STONE_BRICKS);
                for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)put(x+dx,7,z+dz,Math.abs(dx)==2||Math.abs(dz)==2?Material.DARK_OAK_SLAB:Material.DARK_OAK_PLANKS);
                put(x,3,z,Material.LANTERN);
            }
            // A small bell frame marks the town hall without blocking its main doorway.
            for(int dx:new int[]{-2,2})for(int dz:new int[]{-2,2})for(int h=12;h<=14;h++)put(dx,h,-20+dz,Material.STRIPPED_DARK_OAK_LOG);
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)put(dx,15,-20+dz,Material.DARK_PRISMARINE);
            put(0,14,-20,Material.BELL,Shape.CEILING_BELL);
        }
        private void lamps(){
            for(int[] p:new int[][]{{-4,-8},{4,-8},{-9,-3},{9,-3},{-8,4},{8,4},{-4,8},{4,8}}){
                for(int h=1;h<=3;h++)put(p[0],h,p[1],h==1?Material.COBBLED_DEEPSLATE_WALL:Material.SPRUCE_FENCE);
                put(p[0],4,p[1],Material.IRON_CHAIN);put(p[0],5,p[1],Material.LANTERN);
                for(int dx=-1;dx<=1;dx++)put(p[0]+dx,3,p[1],Material.SPRUCE_FENCE);
            }
        }
    }
}
