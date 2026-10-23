package tr.kasabatr.vampir;

import java.io.*;
import java.nio.ByteBuffer;

/** Retain only original map chunks; preserve edited chunks from an existing world. */
final class AnvilRegion {
    private AnvilRegion() {}
    static byte[] retain(byte[] template,byte[] existing)throws IOException{
        return merge(template,existing,index->true,false);
    }
    static byte[] village(byte[] template,byte[] existing,java.util.function.IntPredicate preserveLobby)throws IOException{
        return merge(template,existing,preserveLobby,true);
    }
    private static byte[] merge(byte[] template,byte[] existing,java.util.function.IntPredicate preserve,boolean includeExtra)throws IOException{
        if(template.length<8192||existing!=null&&existing.length<8192)throw new IOException("Geçersiz bölge başlığı");
        byte[] header=new byte[8192];ByteArrayOutputStream body=new ByteArrayOutputStream();int sector=2;
        for(int index=0;index<1024;index++){
            boolean hasTemplate=location(template,index)!=0,hasOld=existing!=null&&location(existing,index)!=0;
            if(!hasTemplate&&!(includeExtra&&hasOld&&preserve.test(index)))continue;
            byte[] chosen=hasOld&&preserve.test(index)?existing:template;
            byte[] chunk=chunk(chosen,index);int sectors=(chunk.length+4095)/4096;
            if(sectors>255||sector>0xffffff)throw new IOException("Bölge dosyası sınırı aşıldı");
            ByteBuffer.wrap(header).putInt(index*4,(sector<<8)|sectors);System.arraycopy(chosen,4096+index*4,header,4096+index*4,4);
            body.write(chunk);body.write(new byte[sectors*4096-chunk.length]);sector+=sectors;
        }
        ByteArrayOutputStream result=new ByteArrayOutputStream(sector*4096);result.write(header);body.writeTo(result);return result.toByteArray();
    }
    private static int location(byte[] region,int index){return ByteBuffer.wrap(region).getInt(index*4);}
    private static byte[] chunk(byte[] region,int index)throws IOException{
        int pointer=location(region,index),offset=(pointer>>>8)*4096,sectors=pointer&255;
        if(offset<8192||sectors==0||offset>region.length-5)throw new IOException("Bozuk chunk konumu");
        int length=ByteBuffer.wrap(region).getInt(offset);
        if(length<1||length>sectors*4096-4||length>region.length-offset-4)throw new IOException("Bozuk chunk boyutu");
        if((region[offset+4]&128)!=0)throw new IOException("Harici chunk dosyası otomatik taşınamıyor; eski dünya korundu");
        return java.util.Arrays.copyOfRange(region,offset,offset+4+length);
    }
}
