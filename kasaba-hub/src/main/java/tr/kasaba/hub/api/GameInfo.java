package tr.kasaba.hub.api;

public record GameInfo(String id,String name,String description,int minimum,int maximum){
    public GameInfo{
        if(id==null||!id.matches("[a-z0-9_-]{1,32}")||name==null||name.isBlank()||name.length()>48||description==null||description.length()>240||minimum<1||maximum<minimum||maximum>20)throw new IllegalArgumentException("Geçersiz oyun bilgisi.");
    }
}
