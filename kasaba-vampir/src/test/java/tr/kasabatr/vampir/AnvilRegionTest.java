package tr.kasabatr.vampir;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AnvilRegionTest {
    private byte[] region(int... slots){byte[] bytes=new byte[(2+slots.length)*4096];for(int i=0;i<slots.length;i++){ByteBuffer.wrap(bytes).putInt(slots[i]*4,((i+2)<<8)|1).putInt((i+2)*4096,2);bytes[(i+2)*4096+4]=2;bytes[(i+2)*4096+5]=(byte)(30+slots[i]);}return bytes;}
    @Test void keepsOnlyMapChunksAndPreservesEdits()throws Exception{byte[] seed=region(0,5),existing=region(0,5,20);existing[2*4096+5]=99;byte[] result=AnvilRegion.retain(seed,existing);assertEquals(4*4096,result.length);assertEquals(0,ByteBuffer.wrap(result).getInt(20*4));assertEquals(99,result[2*4096+5]);}
    @Test void missingMapChunksAreRestoredFromTemplate()throws Exception{byte[] result=AnvilRegion.retain(region(0,5),region(0));assertNotEquals(0,ByteBuffer.wrap(result).getInt(5*4));assertEquals(35,result[3*4096+5]);}
    @Test void corruptExistingChunkIsRejected(){byte[] existing=region(0);ByteBuffer.wrap(existing).putInt(0,0x7fffff01);assertThrows(java.io.IOException.class,()->AnvilRegion.retain(region(0),existing));}
}
