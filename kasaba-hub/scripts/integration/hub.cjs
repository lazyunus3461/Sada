const assert=require('node:assert/strict');
const fs=require('node:fs');
const {connect,delay,text}=require('../../../scripts/integration/smoke.cjs');
const id=Date.now().toString(36),file='/workspace/Sada/kasaba-hub/target/integration-state.json';
async function auth(b){await delay(110);const mark=b.log.length;b.submit('Kasaba',b.form.inputs.length===2?'Kasaba':undefined);await b.wait('system_chat',p=>text(p).includes('keşfedebilirsin'),mark);}
async function login(name){const b=await connect(name);await auth(b);return b;}
async function probe(b,op){const mark=b.log.length;b.c.write('custom_payload',{channel:'kasaba_test:hub',data:Buffer.from(op)});await b.wait('system_chat',p=>text(p).includes('hub-probe:done:'+op)||text(p).includes('hub-probe:error:'),mark);assert(!b.log.slice(mark).some(e=>e.name==='system_chat'&&text(e.p).includes('hub-probe:error:')));return mark;}
async function command(b,cmd,expected){const mark=await probe(b,'PLAYER|'+cmd);await b.wait('system_chat',p=>text(p).includes(expected),mark);return mark;}
async function page(b,cmd,needle){await delay(1050);const mark=b.log.length;b.c.write('chat_command',{command:cmd});await b.wait('show_dialog',p=>text(p).includes(needle),mark);await delay(110);return mark;}
function action(b,label){const i=b.form.actions.findIndex(a=>text(a.label).includes(label));assert(i>=0,'Missing button '+label);return b.action(i);}
// Paper's DialogClickManager publishes callbacks from a queue on the next tick.
async function click(b,label,next){await delay(110);const mark=b.log.length,selected=action(b,label);b.send(selected);if(next)await b.wait('show_dialog',p=>text(p).includes(next),mark);await delay(110);return mark;}
function lobby(b){assert.equal(b.position.x,35.5);assert.equal(b.position.y,-60);assert.equal(b.position.z,35.5);assert.equal(b.position.yaw,123);}
(async()=>{
    if(process.env.KASABA_HUB_PERSIST==='1'){
        const state=JSON.parse(fs.readFileSync(file)),a=await login(state.admin);await a.wait('system_chat',p=>text(p).includes('Kasaba lobisine hoş geldin'));lobby(a);
        await page(a,'grup','Arkadaş Grubu');assert(text(a.form).includes('Grup oluştur'));
        await page(a,'oyunlar','Oyunlar yakında');await command(a,'merkez lobi bilgi','35.50');await a.end();
        console.log('PASS full restart retains exact lobby/facing, existing auth accounts, clears transient groups and shows no fake production games');return;
    }
    const a=await login('HA'+id);await probe(a,'ADMIN');await probe(a,'LOBBY');await command(a,'merkez lobi ayarla','Ana lobi ayarlandı');
    const b=await connect('HB'+id);let mark=b.log.length;b.c.write('chat_command',{command:'oyunlar'});await b.wait('system_chat',p=>text(p).includes('Önce giriş yap'),mark);await auth(b);
    await b.wait('system_chat',p=>text(p).includes('Kasaba lobisine hoş geldin'));lobby(b);await b.wait('show_dialog',p=>text(p).includes('Oyun Merkezi'));
    assert(!b.log.some(e=>e.name==='open_window'));assert.notEqual(b.form.can_close_with_escape,0); // Native codec omits the default true field.
    console.log('PASS actual registration/login remains required; verified players arrive at lobby and get a native page without an inventory');
    await page(a,'grup','Arkadaş Grubu');const stale=action(a,'Grup oluştur');await page(a,'merkez','Oyun Merkezi');a.send(stale);await delay(100);await page(a,'grup','Arkadaş Grubu');assert(text(a.form).includes('Grup oluştur'));
    await click(a,'Grup oluştur','Arkadaş Grubu');await click(a,'Arkadaş davet et','Arkadaşını davet et');
    mark=a.log.length;a.send(action(a,'Davet gönder'),undefined); // Supply the native text input using its actual key.
    await delay(100);await page(a,'grup','Arkadaş Grubu');await click(a,'Arkadaş davet et','Arkadaşını davet et');
    const invite=action(a,'Davet gönder');const nbt=structuredClone(invite.additions);nbt.value.oyuncu={type:'string',value:b.c.username};
    const codec=require('../../../scripts/integration/node_modules/prismarine-nbt');a.c.write('custom_click_action',{id:invite.id.value,payload:codec.proto.createPacketBuffer('anonymousNbt',nbt)});
    await b.wait('system_chat',p=>text(p).includes('seni grubuna davet etti'));await command(b,'grup katıl '+a.c.username,'gruba katıldı');
    await page(b,'grup','Arkadaş Grubu');assert(text(b.form).includes(a.c.username));assert(text(b.form).includes('2/8'));await command(b,'grup davet '+a.c.username,'grup liderine');
    console.log('PASS stale pages cannot create a group; native invitation form joins the whole group and member authority is enforced');
    await probe(b,'ITEM');await probe(b,'AWAY');await command(b,'lobi','Kasaba lobisine hoş geldin');lobby(b);mark=await probe(b,'ITEM_CHECK');await b.wait('system_chat',p=>text(p).includes('item=true'),mark);
    mark=await probe(b,'ACCESS');await b.wait('system_chat',p=>text(p).includes('inside=true,outside=false,damage=true,hunger=true'),mark);
    await command(a,'merkez düzenleme aç','açıldı');mark=await probe(a,'ACCESS');await a.wait('system_chat',p=>text(p).includes('inside=false,outside=false'),mark);await command(a,'merkez düzenleme kapat','kapatıldı');
    console.log('PASS /lobi preserves inventory; protection is local, damage/hunger are blocked and explicit editing permits building');
    await page(a,'oyunlar','Oyunlar yakında');assert(!text(a.form).includes('Katıl'));
    await probe(a,'REGISTER');await page(b,'oyunlar','Yerel Test');mark=await click(b,'Yerel Test');await b.wait('system_chat',p=>text(p).includes('grup liderine'),mark);
    await page(a,'oyunlar','Yerel Test');mark=await click(a,'Yerel Test');await a.wait('system_chat',p=>text(p).includes('grubu kabul edemedi'),mark);
    mark=await probe(b,'STATUS');await b.wait('system_chat',p=>text(p).includes('playing=false,calls=1,size=2,inside=0'),mark);
    await probe(a,'ACCEPT');await page(a,'oyunlar','Yerel Test');mark=await click(a,'Yerel Test');await a.wait('system_chat',p=>text(p).includes('grubunla katıldın'),mark);
    mark=await probe(b,'STATUS');await b.wait('system_chat',p=>text(p).includes('playing=true,calls=2,size=2,inside=2'),mark);await command(b,'grup ayrıl','Önce /lobi');
    await probe(a,'UNREGISTER');await b.wait('system_chat',p=>text(p).includes('Kasaba lobisine hoş geldin'),mark);mark=await probe(b,'STATUS');await b.wait('system_chat',p=>text(p).includes('playing=false'),mark);
    await page(a,'oyunlar','Oyunlar yakında');
    console.log('PASS only the group leader starts a game; rejected entry releases everyone, accepted entry receives two players once, unregister returns everyone and removes the test adapter');
    await a.end();await page(b,'grup','Arkadaş Grubu');assert(text(b.form).includes('Lider: '+b.c.username));await b.end();b.c.end();
    fs.writeFileSync(file,JSON.stringify({admin:a.c.username,member:b.c.username}));
    console.log('PASS leader disconnect promotes the next online member; all transient membership is removed on final quit');
})().then(()=>process.exit(0)).catch(e=>{console.error('FAIL',e);process.exit(1)});
