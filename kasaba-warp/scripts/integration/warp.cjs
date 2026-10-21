const assert=require('node:assert/strict');
const {connect,delay,text}=require('../../../scripts/integration/smoke.cjs');
const {simplify}=require('../../../scripts/integration/node_modules/prismarine-nbt');
const id=Date.now().toString(36);
async function authenticate(b){const mark=b.log.length;b.submit('Kasaba',b.form.inputs.length===2?'Kasaba':undefined);await b.wait('system_chat',p=>text(p).includes('keşfedebilirsin'),mark);}
async function login(name){const b=await connect(name);await authenticate(b);return b;}
async function probe(b,operation){const mark=b.log.length;b.c.write('custom_payload',{channel:'kasaba_test:warp',data:Buffer.from(operation)});await b.wait('system_chat',p=>text(p).includes('warp-probe:done:'+operation),mark);}
async function command(b,command,expected){const mark=b.log.length;b.c.write('chat_command',{command});if(expected)await b.wait('system_chat',p=>text(p).includes(expected),mark);return mark;}
async function menu(b){const mark=await command(b,'warp');const opened=await b.wait('open_window',()=>true,mark);const items=await b.wait('window_items',p=>p.windowId===opened.windowId,mark);return {opened,items};}
function close(b,m){b.c.write('close_window',{windowId:m.opened.windowId});}
function click(b,m,slot,mode=0,mouseButton=0){b.c.write('window_click',{windowId:m.opened.windowId,stateId:m.items.stateId,slot,mouseButton,mode,changedSlots:[],cursorItem:undefined});}
function itemText(item){return text(item.components||[]);}
function atA(b){assert.equal(b.position.x,21.5);assert.equal(b.position.y,-60);assert.equal(b.position.z,18.5);assert.equal(b.position.yaw,123);}
async function success(b,from,contains='noktasına ışınlandın'){await b.wait('system_chat',p=>text(p).includes(contains),from);}
(async()=>{
    if(process.env.KASABA_WARP_PERSIST==='1') {
        const a=await login(process.env.KASABA_WARP_ADMIN);await command(a,'warp bilgi pazar','Pazar Yeri');
        const b=await login('Rest'+id);await command(b,'warp vipalan','yetkin yok');
        const mark=await command(b,'warp pazar','başlatıldı');await success(b,mark);atA(b);
        console.log('PASS warp position, facing, custom label and private access persist across full restart');
        await a.end();await b.end();return;
    }
    const a=await login('WAdmin'+id);await probe(a,'ADMIN');await probe(a,'A');
    await command(a,'warp oluştur pazar','oluşturuldu');await command(a,'warp isim pazar Pazar Yeri','güncellendi');await command(a,'warp açıklama pazar Alışveriş alanı','güncellendi');
    await command(a,'warp oluştur vipalan','oluşturuldu');await command(a,'warp yetki vipalan özel','özel oldu');
    await probe(a,'B');await command(a,'warp oluştur orman','oluşturuldu');
    console.log('PASS Kurucu can create, name, describe and restrict warps with Turkish commands');
    let b=await connect('WUser'+id);await command(b,'warp','Önce giriş yap');await authenticate(b);
    await command(b,'warp oluştur yasak','yetkin yok');await command(b,'warp vipalan','yetkin yok');
    let m=await menu(b);assert.equal(m.opened.inventoryType,5);assert(text(m.items).includes('Pazar Yeri'));assert(!text(m.items).includes('vipalan'));close(b,m);
    let mark=b.log.length;b.c.write('tab_complete',{transactionId:33,text:'/warp '});let completions=await b.wait('tab_complete',p=>p.transactionId===33,mark);assert(text(completions).includes('pazar'));assert(!text(completions).includes('vipalan'));
    console.log('PASS auth blocks warp commands; normal players cannot edit or discover restricted menu/completion entries');
    await probe(b,'B');mark=await command(b,'warp pazar','başlatıldı');await delay(600);
    b.c.write('position',{x:3.5,y:-60,z:2.5,flags:{onGround:true,hasHorizontalCollision:false}});b.c.write('tick_end',{});
    await b.wait('system_chat',p=>text(p).includes('Hareket ettiğin'),mark);
    await command(b,'warp pazar','başlatıldı');await probe(b,'DAMAGE');
    await command(b,'warp pazar','başlatıldı');await command(b,'warp iptal','iptal edildi');
    console.log('PASS movement, damage and explicit cancel allow immediate retry without starting cooldown');
    await probe(b,'B');await probe(b,'BLOCK');mark=await command(b,'warp pazar','başlatıldı');
    await success(b,mark,'başka bir eklenti tarafından engellendi');assert.equal(b.position.x,2.5);
    console.log('PASS an externally cancelled teleport neither moves the player nor starts cooldown');
    await probe(a,'CONSOLE|rol yetki vip kasabawarp.git.vipalan ver');await probe(a,'CONSOLE|rol ver '+b.c.username+' vip');
    m=await menu(b);assert(text(m.items).includes('vipalan'));close(b,m);
    mark=await command(b,'warp vipalan','başlatıldı');await probe(a,'CONSOLE|rol al '+b.c.username);
    await success(b,mark,'erişimi değişti');assert.equal(b.position.x,2.5);
    console.log('PASS exact KasabaRoles permission reveals private warp and revocation is checked again before teleport');
    mark=await command(b,'warp pazar','başlatıldı');const started=Date.now();await delay(900);assert.equal(b.position.x,2.5);
    await success(b,mark);assert(Date.now()-started>=2700);atA(b);const cooldownStarted=Date.now();
    await command(b,'warp orman','saniye bekle');await probe(a,'CONSOLE|warp yenile');await command(b,'warp orman','saniye bekle');
    console.log('PASS three-second preparation completes at saved facing, then twenty-second cooldown survives reload');
    await probe(b,'ITEM');m=await menu(b);mark=b.log.length;click(b,m,0,1);await delay(200);await probe(b,'ITEM_CHECK');
    assert(b.log.slice(mark).some(e=>e.name==='system_chat'&&text(e.p).includes('inventory=true,pearls=false')));
    close(b,m);await b.end();b=await login('WUser'+id);await command(b,'warp pazar','saniye bekle');
    console.log('PASS menu shift-click cannot steal display items and reconnect does not reset cooldown');
    const remaining=Math.max(0,20_150-(Date.now()-cooldownStarted));await delay(remaining);
    m=await menu(b);const slot=m.items.items.findIndex(item=>itemText(item).includes('Pazar Yeri'));assert(slot>=0&&slot<45);
    mark=b.log.length;click(b,m,slot);await b.wait('system_chat',p=>text(p).includes('başlatıldı'),mark);await success(b,mark);atA(b);
    console.log('PASS real inventory click teleports after twenty seconds without resetting configuration to shorter delays');
    await probe(a,'MANY');m=await menu(b);assert(simplify(m.opened.windowTitle).text.includes('1/2'));
    mark=b.log.length;click(b,m,53);let next=await b.wait('open_window',()=>true,mark);assert(simplify(next.windowTitle).text.includes('2/2'));b.c.write('close_window',{windowId:next.windowId});
    console.log('PASS inventory pagination handles more than forty-five visible warps');
    await a.end();await b.end();console.log('PERSIST_ADMIN=WAdmin'+id);
})().then(()=>process.exit(0)).catch(e=>{console.error('FAIL',e);process.exit(1);});
