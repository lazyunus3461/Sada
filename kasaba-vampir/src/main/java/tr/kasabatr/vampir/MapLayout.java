package tr.kasabatr.vampir;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;

final class MapLayout {
    static final List<Station> STATIONS = List.of(new Station("sapel", "Şapel kitabesi", "Şapel", -10, 69, -41), new Station("mezarlik", "Mezar taşı yazısı", "Mezarlık", -49, 69, -39), new Station("gozcu", "Gözcü işareti", "Gözcü Kulesi", 43, 69, -42), new Station("arsiv", "Arşiv sayfaları", "Arşiv", 16, 69, -44), new Station("sifaci", "Şifacı malzemeleri", "Şifacı Evi", 44, 69, -13), new Station("demirci", "Demirci ocağı", "Demirci", 44, 69, 19), new Station("han", "Han erzakları", "Harabe Han", -8, 69, 43), new Station("terzi", "Terzi tezgâhı", "Terzi Evi", -26, 69, -18), new Station("fener", "Fener deposu", "Fener Deposu", 25, 69, -27), new Station("tasci", "Taş işçiliği", "Taşçı Evi", 25, 69, 31), new Station("avci", "Avcı barınağı", "Avcı Barınağı", -56, 69, 20), new Station("karakol", "Eski karakol", "Karakol", 56, 69, 3), new Station("degirmen", "Değirmen çarkı", "Değirmen", -45, 69, 40), new Station("pazar", "Pazar envanteri", "Pazar", -43, 69, -3), new Station("ahir", "Ahır erzakları", "Ahır", 33, 69, 45), new Station("tarla", "Tarlaya malzeme götür", "Tarla", 19, 69, 45), new Station("kuyu", "Kuyu mekanizması", "Kuyu", -31, 69, 23));
    private static final double[][] SEATS = new double[][]{{8.5, 0.5}, {7.5, 2.5}, {6.5, 4.5}, {4.5, 6.5}, {2.5, 7.5}, {0.5, 8.5}, {-1.5, 7.5}, {-3.5, 6.5}, {-5.5, 4.5}, {-6.5, 2.5}, {-7.5, 0.5}, {-6.5, -1.5}, {-5.5, -3.5}, {-3.5, -5.5}, {-1.5, -6.5}, {0.5, -7.5}, {2.5, -6.5}, {4.5, -5.5}, {6.5, -3.5}, {7.5, -1.5}};

    MapLayout() {
    }

    static Location seat(World world, int n) {
        double[] dArray = SEATS[Math.floorMod(n, SEATS.length)];
        float f = (float)Math.toDegrees(Math.atan2(-dArray[0], dArray[1]));
        return new Location(world, dArray[0], 69.0, dArray[1], f, 0.0f);
    }

    static String zone(Location location) {
        Station station = null;
        double d = Double.MAX_VALUE;
        for (Station station2 : STATIONS) {
            double d2;
            double d3 = location.getX() - (double)station2.x - 0.5;
            double d4 = d3 * d3 + (d2 = location.getZ() - (double)station2.z - 0.5) * d2;
            if (!(d4 < d)) continue;
            d = d4;
            station = station2;
        }
        return d > 225.0 ? "Meydan/sokak" : station.zone;
    }

    static boolean inside(Location location) {
        return location.getWorld() != null && location.getX() >= -68 && location.getX() < 69 && location.getZ() >= -68 && location.getZ() < 69 && location.getY() >= 60.0 && location.getY() <= 105.0;
    }

    record Station(String id, String name, String zone, int x, int y, int z) {
        Location location(World world) {
            return new Location(world, (double)this.x + 0.5, this.y, (double)this.z + 0.5);
        }
    }
}
