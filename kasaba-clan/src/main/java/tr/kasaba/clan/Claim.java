package tr.kasaba.clan;

import java.util.UUID;

record Claim(int number,UUID world,Rect bounds) {
    Claim {
        if(number<1||world==null||bounds==null||bounds.area()>1_000_000||bounds.width()>4096||bounds.depth()>4096)
            throw new IllegalArgumentException("Arazi numarası/konumu veya büyüklüğü geçersiz.");
    }
}
