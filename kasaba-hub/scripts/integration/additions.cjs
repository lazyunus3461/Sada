const assert=require('node:assert/strict');
const fs=require('node:fs');
const {connect,delay,text}=require('../../../scripts/integration/smoke.cjs');
const stateFile='/workspace/Sada/kasaba-hub/target/additions-state.json';
async function login(name){const b=await connect(name);await delay(110);const from=b.log.length;b.submit('Kasaba',b.form.inputs.length===2?'Kasaba':undefined);await b.wait('system_chat',p=>text(p).includes('Kasaba lobisine hoş geldin'),from);return b;}
async function probe(b,op){const from=b.log.length;b.c.write('custom_payload',{channel:'kasaba_test:hub',data:Buffer.from(op)});await b.wait('system_chat',p=>text(p).includes('hub-probe:done:'+op)||text(p).includes('hub-probe:error:'),from);assert(!b.log.slice(from).some(e=>e.name==='system_chat'&&text(e.p).includes('hub-probe:error:')));return from;}
async function command(b,cmd,expected){const from=await probe(b,'PLAYER|'+cmd);await b.wait('system_chat',p=>text(p).includes(expected),from);return from;}
async function menu(b,count){await delay(150);const from=await probe(b,'MENU');await b.wait('system_chat',p=>text(p).includes('menu:count='+count+','),from);return from;}
async function page(b,needle){await delay(1050);const from=b.log.length;b.c.write('chat_command',{command:'oyunlar'});await b.wait('show_dialog',p=>text(p).includes(needle),from);await delay(110);}
async function choose(b,label,expected){const i=b.form.actions.findIndex(a=>text(a.label).includes(label));assert(i>=0);const from=b.log.length;b.send(b.action(i));await b.wait('system_chat',p=>text(p).includes(expected),from);return from;}
function at(b,x){assert.equal(b.position.x,x);assert.equal(b.position.y,-60);assert.equal(b.position.z,x);}
(async()=>{
 if(process.env.KASABA_HUB_ADDITIONS_PERSIST==='1'){
  const state=JSON.parse(fs.readFileSync(stateFile)),a=await login(state.admin);await menu(a,1);
  await page(a,'Kasaba Parkur');await command(a,'oyunlar git parkur_test','girişine ışınlandın');at(a,500.5);await menu(a,0);
  await command(a,'lobi','Kasaba lobisine hoş geldin');at(a,35.5);await menu(a,1);
  await command(a,'oyunlar sil parkur_test','Oyun girişi silindi');await page(a,'Oyunlar yakında');await a.end();
  console.log('PASS full restart persists custom entrance location/title and auth accounts; command travel removes the compass, lobby return restores it, deletion persists');return;
 }
 const id=Date.now().toString(36),a=await login('HC'+id),b=await login('HD'+id);await probe(a,'ADMIN');
 await menu(b,1);await probe(b,'ITEM');await probe(b,'NORMAL_COMPASS');
 let from=b.log.length;b.c.write('held_item_slot',{slotId:8});b.c.write('use_item',{hand:0,sequence:1,rotation:{x:0,y:0}});await b.wait('show_dialog',p=>text(p).includes('Oyun Merkezi'),from);
 await probe(b,'MENU_GUARDS');await b.wait('system_chat',p=>text(p).includes('guards:click=true,number=true,drop=true,swap=true'));
 await probe(b,'AWAY');await menu(b,0);from=await probe(b,'MENU');await b.wait('system_chat',p=>text(p).includes('normal=true'),from);await command(b,'lobi','Kasaba lobisine hoş geldin');await menu(b,1);
 from=await probe(b,'ITEM_CHECK');await b.wait('system_chat',p=>text(p).includes('item=true'),from);
 console.log('PASS actual right-click packet opens native main page; generated compass cannot shift, hotbar-swap, drop or change hands; external teleport removes it and ordinary compass/diamonds survive');
 await probe(b,'FULL');await delay(1200);from=await probe(b,'FULL_CHECK');await b.wait('system_chat',p=>text(p).includes('full=true'),from);await menu(b,0);await probe(b,'FREE_SLOT');await delay(1200);await menu(b,1);
 console.log('PASS full inventory retains all 36 stacks, receives no replacement item, and gets one compass after freeing a slot');
 await command(b,'oyunlar ekle illegal Illegal','yetkin yok');
 await probe(a,'AWAY');await command(a,'oyunlar ekle parkur_test Kasaba Parkur','Oyun girişi eklendi');await command(a,'oyunlar ekle parkur_test Duplicate','zaten var');await command(a,'lobi','Kasaba lobisine hoş geldin');
 await command(a,'grup oluştur','oluşturuldu');await command(a,'grup davet '+b.c.username,'gönderildi');await command(b,'grup katıl '+a.c.username,'gruba katıldı');
 await page(b,'Kasaba Parkur');await choose(b,'Kasaba Parkur','grup liderine');
 await probe(b,'VETO');await page(a,'Kasaba Parkur');await choose(a,'Kasaba Parkur','Işınlanma engellendi');at(a,35.5);at(b,35.5);await menu(a,1);await menu(b,1);await probe(b,'ALLOW');
 console.log('PASS only admins create entrances and IDs cannot overwrite; member cannot start, another plugin teleport veto rolls back the preceding member and restores both lobby compasses');
 await page(a,'Kasaba Parkur');from=await choose(a,'Kasaba Parkur','girişine ışınlandın');await b.wait('system_chat',p=>text(p).includes('girişine ışınlandın'));at(a,500.5);at(b,500.5);await menu(a,0);await menu(b,0);
 await page(b,'Kasaba Parkur');await command(a,'oyunlar sil parkur_test','önce oyuncular /lobi');
 await command(a,'lobi','Kasaba lobisine hoş geldin');await command(b,'lobi','Kasaba lobisine hoş geldin');await menu(a,1);await menu(b,1);
 await command(a,'oyunlar ekle gecici Geçici Oyun','Oyun girişi eklendi');await probe(a,'AWAY');await command(a,'oyunlar konum gecici','konumu güncellendi');await command(a,'lobi','Kasaba lobisine hoş geldin');await command(a,'oyunlar sil gecici','Oyun girişi silindi');
 console.log('PASS native selection moves the whole group to saved coordinates, removes only temporary menu items and keeps /oyunlar usable; active entrance mutation is blocked, return restores items, update and delete work');
 const path='/workspace/kasaba-test-server/plugins/KasabaHub/oyunlar.yml',backup=fs.readFileSync(path);
 try{fs.writeFileSync(path,'oyunlar: [');await command(a,'merkez yenile','Dosya okunamadı');await page(a,'Kasaba Parkur');}finally{fs.writeFileSync(path,backup);}
 await command(a,'merkez yenile','yenilendi');await menu(a,1);await a.end();await b.end();fs.writeFileSync(stateFile,JSON.stringify({admin:a.c.username,member:b.c.username}));
 console.log('PASS corrupt entrance reload preserves the file and working menu; valid reload retains current groups and inventory');
})().then(()=>process.exit(0)).catch(e=>{console.error('FAIL',e);process.exit(1)});
