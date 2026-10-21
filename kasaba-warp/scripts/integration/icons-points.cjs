const assert=require('node:assert/strict');
const fs=require('node:fs');
const {delay,text}=require('../../../scripts/integration/smoke.cjs');
const {login,probe,command:rawCommand,menu,close,click,success,itemText}=require('./warp.cjs');
// Keep automated administrative bursts below vanilla's normal command spam limit.
async function command(...args){await delay(1100);return rawCommand(...args);}
const mcData=require('../../../scripts/integration/node_modules/minecraft-data')('1.21.10');
const stateFile='/workspace/Sada/kasaba-warp/target/icons-points-state.json';
const id=Date.now().toString(36);
const pointKey=b=>[b.position.x,b.position.y,b.position.z,b.position.yaw,b.position.pitch].join(',');
const saved=new Set(['21.5,-60,18.5,123,-12','2.5,-60,2.5,10,0','-8.5,-60,6.5,-45,7']);
async function run(){
    if(process.env.KASABA_WARP_PERSIST==='1'){
        const state=JSON.parse(fs.readFileSync(stateFile));const a=await login(state.admin);
        await command(a,'warp bilgi '+state.warp,'Kayıtlı konum: 3');let m=await menu(a);
        assert.equal(m.items.items[4].itemId,mcData.itemsByName.oak_sapling.id);assert(itemText(m.items.items[4]).includes('731'));
        let mark=a.log.length;click(a,m,53);const next=await a.wait('open_window',p=>text(p.windowTitle).includes('2/2'),mark);a.c.write('close_window',{windowId:next.windowId});
        mark=a.log.length;a.c.write('tab_complete',{transactionId:71,text:'/warp noktasil '+state.warp+' '});
        const completion=await a.wait('tab_complete',p=>p.transactionId===71,mark);assert.deepEqual(completion.matches.map(e=>e.match),['1','2','3']);
        a.c.write('tab_complete',{transactionId:72,text:'/warp noktasil a.b '});await command(a,'warp bilgi pazar','Pazar Yeri');
        const b=await login('IPRest'+id);await command(b,'warp '+state.privateWarp,'yetkin yok');
        mark=await command(b,'warp '+state.warp,'başlatıldı');await success(b,mark);assert(saved.has(pointKey(b)));
        await a.end();await b.end();
        const legacy=await login('IPLegacy'+id);mark=await command(legacy,'warp pazar','başlatıldı');await success(legacy,mark);assert.equal(pointKey(legacy),'21.5,-60,18.5,123,-12');await legacy.end();
        console.log('PASS full restart preserves custom icon/model, fixed cell, three saved points and private access; native pagination/completion and legacy single-point warp work');return;
    }
    const a=await login('IPAdmin'+id);const warp='iporman'+id;const privateWarp='ipozel'+id;
    await probe(a,'ADMIN');await probe(a,'A');await probe(a,'SAPLING');
    await command(a,'warp oluştur '+warp+' 1-3','oluşturuldu');
    let mark=a.log.length;await probe(a,'SAPLING_CHECK');assert(a.log.slice(mark).some(e=>text(e.p).includes('sapling=true')));
    let m=await menu(a);assert.equal(m.items.items[2].itemId,mcData.itemsByName.oak_sapling.id);assert.equal(m.items.items[2].itemCount,1);
    assert(itemText(m.items.items[2]).includes('731'));assert(itemText(m.items.items[2]).includes(warp));close(a,m);
    await command(a,'warp oluştur collision'+id+' 1-3','zaten');await command(a,'warp oluştur footer'+id+' 6-3','1–5');
    await command(a,'warp simge '+warp+' 1-5','güncellendi');
    m=await menu(a);assert.equal(m.items.items[4].itemId,mcData.itemsByName.oak_sapling.id);assert(!itemText(m.items.items[2]).includes(warp));close(a,m);
    console.log('PASS held custom model is copied as one menu item without consuming seven held items; fixed cell moves and collision/footer edits are rejected');
    await probe(a,'B');await command(a,'warp noktaekle '+warp,'Toplam 2');await command(a,'warp noktaekle '+warp,'zaten');
    await probe(a,'C');await command(a,'warp noktaekle '+warp,'Toplam 3');await command(a,'warp noktalar '+warp,'3 konum');
    await command(a,'warp noktasil '+warp+' 4','1–3');
    await command(a,'warp oluştur '+privateWarp+' 1-7','oluşturuldu');await command(a,'warp yetki '+privateWarp+' özel','özel oldu');
    let b=await login('IPUser'+id);await command(b,'warp noktaekle '+warp,'yetkin yok');
    m=await menu(b);assert.equal(m.items.items[6].itemCount,0);assert(!text(m.items).includes(privateWarp));
    mark=b.log.length;click(b,m,4,1);await delay(200);assert(!b.log.slice(mark).some(e=>e.name==='system_chat'&&text(e.p).includes('başlatıldı')));close(b,m);
    console.log('PASS same warp accepts three saved points, rejects duplicates, enforces admin editing and leaves hidden private fixed cells empty');
    const seen=new Set();m=await menu(b);mark=b.log.length;click(b,m,4);await b.wait('system_chat',p=>text(p).includes('başlatıldı'),mark);await success(b,mark);
    assert(saved.has(pointKey(b)));seen.add(pointKey(b));await command(b,'warp '+warp,'saniye bekle');
    await probe(a,'CONSOLE|warp yenile');await command(b,'warp '+warp,'saniye bekle');await b.end();
    // Independent authenticated players use the unchanged real three-second warmup.
    for(let i=0;i<5;i++){
        b=await login('IP'+i+id);mark=await command(b,'warp '+warp,'başlatıldı');await success(b,mark);assert(saved.has(pointKey(b)));seen.add(pointKey(b));await b.end();
    }
    console.log('PASS menu and command teleports land exclusively at saved coordinates/facing; real warmup and twenty-second cooldown remain active ('+seen.size+' destinations observed)');
    await command(a,'warp noktasil '+warp+' 1','Kalan 2');await command(a,'warp noktalar '+warp,'2 konum');
    await command(a,'warp noktasil '+warp+' 2','Kalan 1');await command(a,'warp noktasil '+warp+' 1','Son nokta');
    await probe(a,'A');await command(a,'warp noktaekle '+warp,'Toplam 2');await probe(a,'C');await command(a,'warp noktaekle '+warp,'Toplam 3');
    m=await menu(a);assert.equal(m.items.items[4].itemId,mcData.itemsByName.oak_sapling.id);assert(itemText(m.items.items[4]).includes('3 kayıtlı konum'));close(a,m);
    console.log('PASS numbered removal promotes the next point and protects the last; icon and menu cell survive point edits and reload');
    fs.writeFileSync(stateFile,JSON.stringify({warp,privateWarp,admin:a.c.username}));await a.end();
}
run().then(()=>process.exit(0)).catch(e=>{console.error('FAIL',e);process.exit(1);});
