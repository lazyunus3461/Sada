const assert=require('node:assert/strict');
const fs=require('node:fs');
const {connect,delay,text}=require('../../../scripts/integration/smoke.cjs');
const {login,plain}=require('../../../kasaba-roles/scripts/integration/chat.cjs');
const {simplify}=require('../../../scripts/integration/node_modules/prismarine-nbt');
const id=Date.now().toString(36),tag=('K'+Date.now().toString(16).slice(-4)).toUpperCase(),otherTag='O'+tag.slice(1);
const stateFile='/workspace/Sada/kasaba-clan/target/integration-state.json';
async function probe(b,operation){const mark=b.log.length;b.c.write('custom_payload',{channel:'kasaba_test:clan',data:Buffer.from(operation)});await b.wait('system_chat',p=>text(p).includes('clan-probe:done:'+operation)||text(p).includes('clan-probe:error:'),mark);assert(!b.log.slice(mark).some(e=>e.name==='system_chat'&&text(e.p).includes('clan-probe:error:')));return mark;}
async function command(b,cmd,expected){await delay(1050);const mark=b.log.length;b.c.write('chat_command',{command:cmd});await b.wait('system_chat',p=>text(p).includes(expected),mark);return mark;}
async function dispatch(b,cmd,expected){const mark=await probe(b,'PLAYER|'+cmd);await b.wait('system_chat',p=>text(p).includes(expected),mark);return mark;}
async function select(b,x1,z1,x2,z2){await probe(b,'POS|'+(x1+.5)+'|'+(z1+.5));await dispatch(b,'klan alan birinci','Birinci köşe');await probe(b,'POS|'+(x2+.5)+'|'+(z2+.5));await dispatch(b,'klan alan ikinci','İkinci köşe');}
async function access(b,denied){const mark=await probe(b,'ACCESS');await b.wait('system_chat',p=>text(p).includes('access:break='+denied+',place='+denied+',bucket='+denied+',high='+denied+',chest='+denied+',entity='+denied+',pvp=false'),mark);}
async function tab(b,role,clan,from=0){const expected='['+role+'] '+(clan?'['+clan+'] ':'')+b.c.username;await b.wait('player_info',p=>p.data.some(e=>e.uuid===b.c.uuid&&e.displayName&&plain(simplify(e.displayName))===expected),from);}
async function team(b,name,role,clan,from=0){
    const expected='['+role+'] '+(clan?'['+clan+'] ':'');
    const entry=b.log.filter(e=>e.name==='teams'&&e.p.players?.includes(name)).at(-1);assert(entry);
    await b.wait('teams',p=>p.team===entry.p.team&&p.prefix&&plain(simplify(p.prefix))===expected,from);
}
async function chat(a,b,role,clan,message){
    const mark=b.log.length;a.c.chat(message);const expected='['+role+'] ['+clan+'] '+a.c.username+' » '+message;
    for(let i=0;i<320;i++){
        const packet=b.log.slice(mark).find(e=>{if(!['player_chat','profileless_chat','system_chat'].includes(e.name))return false;const raw=e.p.unsignedChatContent||e.p.message||e.p.content;return raw&&plain(raw.type?simplify(raw):raw)===expected;});
        if(packet)return;await delay(25);
    }
    throw new Error('Combined clan chat was not received: '+text(b.log.slice(mark).filter(e=>e.name.includes('chat'))));
}
async function run(){
    if(process.env.KASABA_CLAN_PERSIST==='1'){
        const state=JSON.parse(fs.readFileSync(stateFile));const a=await login(state.admin),b=await login(state.member),o=await login(state.outsider);
        await tab(a,'Kurucu',state.tag);await tab(b,'Oyuncu',state.tag);await team(o,a.c.username,'Kurucu',state.tag);
        await command(a,'klan bilgi','100/5000');await command(a,'klan orman bilgi','world');await access(o,true);await access(b,false);
        await select(a,1040,1040,1060,1060);const privateMark=await command(a,'klan alan al','100×100');
        assert(!a.log.slice(privateMark).some(e=>e.name==='system_chat'&&text(e.p).includes(state.warp)));await command(a,'klan alan iptal','kapatıldı');
        await probe(a,'POS|205.5|205.5');await dispatch(a,'warp oluştur restart'+id,'çakışıyor');
        await a.end();await b.end();await o.end();console.log('PASS full restart retains forest, clan/members, exact claims, all three label locations, outsider protection and warp placement guard; claim rejection does not expose private warp IDs');return;
    }
    const a=await login('KA'+id),b=await login('KM'+id);let o=await connect('KO'+id);
    let mark=o.log.length;o.c.write('chat_command',{command:'klan oluştur NO Girişsiz'});await o.wait('system_chat',p=>text(p).includes('Önce giriş yap'),mark);
    mark=o.log.length;o.submit('Kasaba',o.form.inputs.length===2?'Kasaba':undefined);await o.wait('system_chat',p=>text(p).includes('keşfedebilirsin'),mark);
    await probe(a,'ADMIN');await command(a,'klan oluştur '+tag+' Test Kasaba','Klan oluşturuldu');
    await command(a,'klan orman ayarla','orman dünyası ayarlandı');
    await command(b,'klan katıl '+tag,'geçerli davetin yok');await command(a,'klan davet '+b.c.username,'gönderildi');
    mark=b.log.length;await command(b,'klan katıl '+tag,'klana katıldı');await tab(b,'Oyuncu',tag,mark);await tab(a,'Kurucu',tag);await team(o,a.c.username,'Kurucu',tag);
    console.log('PASS auth blocks commands; invite is required; joining updates native TAB and overhead while retaining the role');
    await chat(a,o,'Kurucu',tag,'Klan etiketi denemesi');
    mark=o.log.length;await command(a,'klan sohbet Sadece klan üyelerine','Sadece klan üyelerine');await b.wait('system_chat',p=>text(p).includes('Sadece klan üyelerine'));assert(!o.log.slice(mark).some(e=>e.name==='system_chat'&&text(e.p).includes('Sadece klan üyelerine')));
    console.log('PASS public chat contains role and clan once; private clan chat reaches members only');
    await probe(a,'POS|600.5|600.5');const warp='klanorman'+id;await dispatch(a,'warp oluştur '+warp+' 2-3','oluşturuldu');
    await probe(a,'POS|1000.5|1000.5');await dispatch(a,'warp noktaekle '+warp,'Toplam 2');await dispatch(a,'warp yetki '+warp+' özel','özel oldu');
    await select(a,1040,1040,1060,1060);await command(a,'klan alan al','100×100');
    await probe(a,'POS|200.5|200.5');await command(a,'klan alan iptal','kapatıldı');await command(a,'klan alan seç','Seçim açıldı');
    mark=a.log.length;a.c.write('block_dig',{status:0,location:{x:200,y:-61,z:200},face:1,sequence:1});await a.wait('system_chat',p=>text(p).includes('Birinci köşe: 200, 200'),mark);
    await probe(a,'POS|209.5|209.5');mark=a.log.length;a.c.write('block_place',{hand:0,location:{x:209,y:-61,z:209},direction:1,cursorX:.5,cursorY:1,cursorZ:.5,insideBlock:false,worldBorderHit:false,sequence:2});await a.wait('system_chat',p=>text(p).includes('İkinci köşe: 209, 209'),mark);
    await command(a,'klan alan al','10×10 = 100');await command(a,'klan bilgi','100/5000');
    await select(a,1200,1200,1300,1249);await command(a,'klan alan al','Toplam arazi sınırı');await command(a,'klan alan iptal','kapatıldı');
    console.log('PASS private extra warp target blocks a partially overlapping claim; real left/right click selects a 100-block parcel; total 5000 budget is enforced');
    await command(o,'klan oluştur '+otherTag+' Başka Klan','oluşturuldu');await select(o,205,205,215,215);await command(o,'klan alan al','çakışıyor');await command(o,'klan alan iptal','kapatıldı');
    await access(o,true);await access(b,false);
    mark=await probe(a,'AUTOMATION');await a.wait('system_chat',p=>text(p).includes('automation:flow=true,ownflow=false,push=true,pull=true,hopper=true,ownhopper=false,explosion=true,burn=true'),mark);
    console.log('PASS foreign break/place/bucket/chest/entity and high-Y access are denied; member access and existing PvP are preserved; flow/piston/hopper/explosion/fire protections work');
    await probe(o,'MODE|CREATIVE');await probe(o,'POS|201.5|202.5');await probe(o,'STONE');
    o.c.write('block_dig',{status:0,location:{x:202,y:-60,z:202},face:1,sequence:3});await delay(250);mark=await probe(o,'BLOCK');await o.wait('system_chat',p=>text(p).includes('block=STONE'),mark);
    await probe(b,'MODE|CREATIVE');await probe(b,'POS|201.5|202.5');
    b.c.write('block_dig',{status:0,location:{x:202,y:-60,z:202},face:1,sequence:4});await delay(250);mark=await probe(b,'BLOCK');await b.wait('system_chat',p=>text(p).includes('block=AIR'),mark);
    await probe(b,'MODE|SURVIVAL');await probe(o,'MODE|SURVIVAL');
    console.log('PASS real creative dig packets leave foreign claimed stone intact and allow a member to break it');
    await probe(a,'POS|205.5|205.5');await dispatch(a,'warp oluştur conflict'+id,'çakışıyor');await dispatch(a,'warp noktaekle '+warp,'çakışıyor');await dispatch(a,'warp taşı '+warp,'çakışıyor');await dispatch(a,'warp bilgi '+warp,'Kayıtlı konum: 2');
    await probe(a,'CORRUPT');await access(o,true);await tab(a,'Kurucu',tag);await probe(a,'RESTORE');
    console.log('PASS new/moved/extra warp cannot cover a claim; malformed reload retains active ownership and labels until the original file is restored');
    mark=b.log.length;await command(a,'klan çıkar '+b.c.username,'klandan çıkarıldı');await tab(b,'Oyuncu',null,mark);await access(b,true);
    await command(a,'klan davet '+b.c.username,'gönderildi');await command(b,'klan katıl '+tag,'klana katıldı');await command(a,'klan yetki '+b.c.username+' yardımcı','yetkisi güncellendi');
    await command(a,'klan ayrıl','Lider önce');await command(b,'klan dağıt onayla','sadece klan liderine');
    console.log('PASS removal immediately revokes both claims and labels; rejoin/assistant rank work and leadership safeguards prevent accidental loss');
    fs.writeFileSync(stateFile,JSON.stringify({admin:a.c.username,member:b.c.username,outsider:o.c.username,tag,otherTag,warp}));await a.end();await b.end();await o.end();
}
run().then(()=>process.exit(0)).catch(e=>{console.error('FAIL',e);process.exit(1);});
