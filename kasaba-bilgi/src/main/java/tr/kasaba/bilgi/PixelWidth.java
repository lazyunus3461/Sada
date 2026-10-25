package tr.kasaba.bilgi;
/** Default Minecraft font advances; server-side text fitting without changing the client GUI scale. */
final class PixelWidth {
    static int width(String text){return text.codePoints().map(c->switch(c){case ' '->4;case 'i','!',':',';',39, '.',','->2;case 'l','|'->3;case 'I','[',']','t'->4;case 'f','k','(',')','{','}'->5;case '▸','♛','◈','⚑','⌛','…'->8;default->6;}).sum();}
    static String fit(String text,int pixels){if(width(text)<=pixels)return text;StringBuilder out=new StringBuilder();int used=0;for(int c:text.codePoints().toArray()){int n=width(new String(Character.toChars(c)));if(used+n+8>pixels)break;out.appendCodePoint(c);used+=n;}return out+"…";}
}
