package tr.kasabatr.vampir;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;

final class MapLayout {
    static final List<Station> STATIONS = List.of(new Station("sapel", "Şapel kitabesi", "Şapel", -22, 69, -48), new Station("mezarlik", "Mezar taşı yazısı", "Mezarlık", -46, 69, -48), new Station("gozcu", "Gözcü işareti", "Gözcü Kulesi", 50, 69, -48), new Station("arsiv", "Arşiv sayfaları", "Arşiv", 2, 69, -48), new Station("sifaci", "Şifacı malzemeleri", "Şifacı Evi", 50, 69, -24), new Station("demirci", "Demirci ocağı", "Demirci", 50, 69, 24), new Station("han", "Han erzakları", "Harabe Han", 2, 69, 48), new Station("terzi", "Terzi tezgâhı", "Terzi Evi", -22, 69, -24), new Station("fener", "Fener deposu", "Fener Deposu", 26, 69, -24), new Station("tasci", "Taş işçiliği", "Taşçı Evi", 26, 69, 24), new Station("avci", "Avcı barınağı", "Avcı Barınağı", -46, 69, 0), new Station("karakol", "Eski karakol", "Karakol", 50, 69, 0), new Station("degirmen", "Değirmen çarkı", "Değirmen", -46, 69, 48), new Station("pazar", "Pazar envanteri", "Pazar", -46, 69, 24), new Station("ahir", "Ahır erzakları", "Ahır", 50, 69, 48), new Station("tarla", "Tarlaya malzeme götür", "Tarla", 26, 69, 48), new Station("kuyu", "Kuyu mekanizması", "Kuyu", -22, 69, 24));
    private static final double[][] SEATS = new double[][]{{8.5, 0.5}, {7.5, 2.5}, {6.5, 4.5}, {4.5, 6.5}, {2.5, 7.5}, {0.5, 8.5}, {-1.5, 7.5}, {-3.5, 6.5}, {-5.5, 4.5}, {-6.5, 2.5}, {-7.5, 0.5}, {-6.5, -1.5}, {-5.5, -3.5}, {-3.5, -5.5}, {-1.5, -6.5}, {0.5, -7.5}, {2.5, -6.5}, {4.5, -5.5}, {6.5, -3.5}, {7.5, -1.5}};

    MapLayout() {
    }

    static int originX(String name) {
        if(name.startsWith("vk_room_102_"))return 320;
        if(name.startsWith("vk_room_103_"))return 640;
        return 0;
    }
    static int originX(World world){return world==null?0:originX(world.getName());}

    static Location seat(World world, int n) {
        double[] dArray = SEATS[Math.floorMod(n, SEATS.length)];
        float f = (float)Math.toDegrees(Math.atan2(-dArray[0], dArray[1]));
        return new Location(world, originX(world)+dArray[0], 69.0, dArray[1], f, 0.0f);
    }

    static String zone(Location location) {
        Station station = null;
        double d = Double.MAX_VALUE;
        for (Station station2 : STATIONS) {
            double d2;
            double d3 = location.getX() - originX(location.getWorld()) - (double)station2.x - 0.5;
            double d4 = d3 * d3 + (d2 = location.getZ() - (double)station2.z - 0.5) * d2;
            if (!(d4 < d)) continue;
            d = d4;
            station = station2;
        }
        return d > 225.0 ? "Meydan/sokak" : station.zone;
    }

    static int deliveryStop(int from,int excluded) {
        return java.util.stream.IntStream.range(0,STATIONS.size()).filter(i->i!=from&&i!=excluded).boxed().min(java.util.Comparator.<Integer>comparingDouble(i->{var a=STATIONS.get(from);var b=STATIONS.get(i);return Math.pow(a.x-b.x,2)+Math.pow(a.z-b.z,2);}).thenComparingInt(i->i)).orElseThrow();
    }

    static boolean inside(Location location) {
        double x=location.getX()-originX(location.getWorld());
        return location.getWorld() != null && x >= -68 && x < 69 && location.getZ() >= -68 && location.getZ() < 69 && location.getY() >= 60.0 && location.getY() <= 105.0;
    }

    record Station(String id, String name, String zone, int x, int y, int z) {
        Location location(World world) {
            return new Location(world, originX(world)+(double)this.x + 0.5, this.y, (double)this.z + 0.5);
        }
    }
}
