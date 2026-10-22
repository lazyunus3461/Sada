package tr.kasaba.akce;

import tr.kasaba.akce.api.AkceApi;
import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Copy-on-write ledger. Balances and payment receipts commit in the same atomic file replacement. */
public final class AkceLedger implements AkceApi {
    private static final long MAX=100_000_000_000_000L;
    private record Receipt(UUID player,long cents,boolean paid) {}
    private final Path path;
    private Map<UUID,Long> balances=new HashMap<>();
    private Map<UUID,String> names=new HashMap<>();
    private Map<String,Receipt> receipts=new HashMap<>();

    public AkceLedger(Path path) throws IOException {
        this.path=path;
        if(!Files.exists(path))return;
        Properties data=new Properties();try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){data.load(reader);}
        if(!"1".equals(data.getProperty("schema")))throw new IOException("Akçe dosyasının sürümü okunamadı; dosya değiştirilmedi.");
        try{
            for(String key:data.stringPropertyNames()){
                String value=data.getProperty(key);
                if(key.startsWith("balance.")){UUID id=UUID.fromString(key.substring(8));long cents=Long.parseLong(value);checkRange(cents);balances.put(id,cents);}
                else if(key.startsWith("name.")){UUID id=UUID.fromString(key.substring(5));if(!value.matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("Oyuncu adı bozuk");names.put(id,value);}
                else if(key.startsWith("tx.")){String tx=new String(Base64.getUrlDecoder().decode(key.substring(3)),StandardCharsets.UTF_8);String[] fields=value.split(":");if(fields.length!=3||!Set.of("true","false").contains(fields[2]))throw new IllegalArgumentException("İşlem kaydı bozuk");long cents=Long.parseLong(fields[1]);checkRange(cents);receipts.put(tx,new Receipt(UUID.fromString(fields[0]),cents,Boolean.parseBoolean(fields[2])));}
            }
            if(!balances.keySet().containsAll(names.keySet()))throw new IllegalArgumentException("İsimsiz hesap kaydı");
        }catch(RuntimeException ex){throw new IOException("Akçe dosyası bozuk; mevcut dosya korundu.",ex);}
    }
    public static BigDecimal parse(String text){
        if(text==null||!text.matches("[0-9]{1,13}([.,][0-9]{1,2})?"))throw new IllegalArgumentException("Miktarı 100 veya 100,50 gibi yaz; en fazla iki ondalık basamak kullan.");
        BigDecimal amount=new BigDecimal(text.replace(',','.'));cents(amount);return amount.setScale(2);
    }
    private static void checkRange(long amount){if(amount<0||amount>MAX)throw new IllegalArgumentException("Miktar 0 ile 1.000.000.000.000 Akçe arasında olmalı.");}
    private static long cents(BigDecimal amount){
        if(amount==null||amount.signum()<0||amount.compareTo(BigDecimal.valueOf(MAX,2))>0)throw new IllegalArgumentException("Geçersiz Akçe miktarı.");
        try{long value=amount.movePointRight(2).longValueExact();checkRange(value);return value;}catch(ArithmeticException ex){throw new IllegalArgumentException("En fazla iki ondalık basamak kullan.");}
    }
    public synchronized void remember(UUID id,String name,BigDecimal initial){
        Objects.requireNonNull(id);if(name==null||!name.matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("Geçersiz oyuncu adı.");
        if(name.equals(names.get(id))&&balances.containsKey(id))return;
        Map<UUID,Long> money=new HashMap<>(balances);money.putIfAbsent(id,cents(initial));Map<UUID,String> labels=new HashMap<>(names);labels.put(id,name);commit(money,labels,receipts);
    }
    public synchronized Optional<UUID> find(String name){List<UUID> found=names.entrySet().stream().filter(e->e.getValue().equalsIgnoreCase(name)).map(Map.Entry::getKey).toList();return found.size()==1?Optional.of(found.getFirst()):Optional.empty();}
    public synchronized String name(UUID id){return names.getOrDefault(id,id.toString());}
    public synchronized List<UUID> richest(){return names.keySet().stream().sorted(Comparator.<UUID>comparingLong(id->balances.getOrDefault(id,0L)).reversed().thenComparing(id->names.get(id))).limit(10).toList();}
    @Override public synchronized BigDecimal balance(UUID player){return BigDecimal.valueOf(balances.getOrDefault(Objects.requireNonNull(player),0L),2);}
    @Override public synchronized void deposit(UUID player,BigDecimal amount){long value=cents(amount);long result=Math.addExact(balances.getOrDefault(Objects.requireNonNull(player),0L),value);checkRange(result);Map<UUID,Long> money=new HashMap<>(balances);money.put(player,result);commit(money,names,receipts);}
    public synchronized void set(UUID player,BigDecimal amount){long value=cents(amount);Map<UUID,Long> money=new HashMap<>(balances);money.put(Objects.requireNonNull(player),value);commit(money,names,receipts);}
    @Override public synchronized boolean transfer(UUID from,UUID to,BigDecimal amount){
        Objects.requireNonNull(from);Objects.requireNonNull(to);if(from.equals(to))throw new IllegalArgumentException("Kendine Akçe gönderemezsin.");long value=cents(amount);if(value==0)throw new IllegalArgumentException("Gönderilecek miktar sıfırdan büyük olmalı.");long source=balances.getOrDefault(from,0L),target=balances.getOrDefault(to,0L);if(source<value)return false;checkRange(Math.addExact(target,value));Map<UUID,Long> money=new HashMap<>(balances);money.put(from,source-value);money.put(to,target+value);commit(money,names,receipts);return true;
    }
    @Override public synchronized boolean withdraw(UUID player,BigDecimal amount,String transactionId){
        Objects.requireNonNull(player);if(transactionId==null||transactionId.isBlank()||transactionId.length()>512)throw new IllegalArgumentException("Geçersiz işlem kimliği.");long value=cents(amount);Receipt old=receipts.get(transactionId);if(old!=null){if(!old.player().equals(player)||old.cents()!=value)throw new IllegalArgumentException("İşlem kimliği başka bir ödeme için kullanılmış.");return old.paid();}
        long before=balances.getOrDefault(player,0L);boolean paid=before>=value;Map<UUID,Long> money=new HashMap<>(balances);if(paid)money.put(player,before-value);Map<String,Receipt> saved=new HashMap<>(receipts);saved.put(transactionId,new Receipt(player,value,paid));commit(money,names,saved);return paid;
    }
    private void commit(Map<UUID,Long> money,Map<UUID,String> labels,Map<String,Receipt> payments){
        Properties data=new Properties();data.setProperty("schema","1");money.forEach((id,value)->data.setProperty("balance."+id,value.toString()));labels.forEach((id,value)->data.setProperty("name."+id,value));payments.forEach((id,value)->data.setProperty("tx."+Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes(StandardCharsets.UTF_8)),value.player()+":"+value.cents()+":"+value.paid()));
        Path temp=path.resolveSibling(path.getFileName()+".tmp");try{
            Files.createDirectories(path.toAbsolutePath().getParent());StringWriter writer=new StringWriter();data.store(writer,"Kasaba Akce - bu dosyayi sunucu kapaliyken yedekleyin");byte[] bytes=writer.toString().getBytes(StandardCharsets.UTF_8);
            try(FileChannel channel=FileChannel.open(temp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}
            // Fail closed if the filesystem cannot replace the ledger atomically.
            Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException ex){throw new IllegalStateException("Akçe kaydedilemedi; bakiyeler değiştirilmedi.",ex);}
        balances=new HashMap<>(money);names=new HashMap<>(labels);receipts=new HashMap<>(payments);
    }
}
