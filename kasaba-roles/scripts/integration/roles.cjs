const assert=require('node:assert/strict');
const {connect,delay,text}=require('../../../scripts/integration/smoke.cjs');
const {simplify}=require('../../../scripts/integration/node_modules/prismarine-nbt');
const username=process.env.KASABA_ROLES_USER||('Role'+Date.now().toString(36));
async function login(name) {
    const b=await connect(name);const mark=b.log.length;
    b.submit('Kasaba',b.form.inputs.length===2?'Kasaba':undefined);
    await b.wait('system_chat',p=>text(p).includes('keşfedebilirsin'),mark);
    return b;
}
async function request(b,message,expected) {
    const mark=b.log.length;
    b.c.write('custom_payload',{channel:'kasaba_test:roles',data:Buffer.from(message)});
    await b.wait('system_chat',p=>text(p).includes('roles-probe:'+expected),mark);
}
async function command(b,command){await request(b,'COMMAND|rol '+command,'command-done');}
async function permission(b,node,value){await request(b,'CHECK|'+node,node+'='+value);}
function plain(c){return typeof c==='string'?c:(c.text||'')+(c.extra||[]).map(plain).join('');}
function ownEntries(b,from=0){return b.log.slice(from).filter(e=>e.name==='player_info').flatMap(e=>e.p.data).filter(e=>e.uuid===b.c.uuid);}
async function tab(b,label,color,order,from=0) {
    await b.wait('player_info',p=>p.data.some(e=>e.uuid===b.c.uuid&&e.displayName&&plain(simplify(e.displayName))==='['+label+'] '+b.c.username),from);
    const display=ownEntries(b,from).filter(e=>e.displayName).at(-1);
    const component=simplify(display.displayName);
    assert.equal(plain(component),'['+label+'] '+b.c.username);
    assert(text(component).toLowerCase().includes(color.toLowerCase()),'Missing TAB role color '+color+': '+text(component));
    assert(ownEntries(b,from).some(e=>e.listPriority===order),'Missing TAB order '+order);
}
(async()=>{
    let b=await login(username);
    if(process.env.KASABA_ROLES_PERSIST==='1') {
        await tab(b,'Rehber','aqua',400);await permission(b,'kasabaroles.admin',false);
        console.log('PASS role assignment survives a full Paper restart');await b.end();return;
    }
    await tab(b,'Oyuncu','gray',1);await permission(b,'kasabaroles.admin',false);
    let mark=b.log.length;b.c.write('chat_command',{command:'rol ver '+username+' kurucu'});
    await b.wait('system_chat',p=>text(p).includes('yetkin yok'),mark);
    await permission(b,'kasabaroles.admin',false);
    console.log('PASS default TAB tag and permission check prevent self-promotion');
    mark=b.log.length;await command(b,'ver '+username+' kurucu');
    await tab(b,'Kurucu','#FF4545',1000,mark);await permission(b,'kasabaroles.admin',true);
    await permission(b,'minecraft.command.kick',true);
    console.log('PASS console grants Kurucu with colored native TAB and registered wildcard permissions');
    mark=b.log.length;await command(b,'al '+username);
    await tab(b,'Oyuncu','gray',1,mark);await permission(b,'kasabaroles.admin',false);
    await permission(b,'minecraft.command.kick',false);
    console.log('PASS removing role immediately removes previous administrative permissions');
    await command(b,'olustur deneme turkuaz');
    await command(b,'isim deneme Deneme Ekibi');
    await command(b,'renk deneme #123ABC');await command(b,'sira deneme 500');
    await command(b,'yetki deneme kasaba.test.* ver');
    await command(b,'yetki deneme kasaba.test.denied yasakla');
    mark=b.log.length;await command(b,'ver '+username+' deneme');
    await tab(b,'Deneme Ekibi','#123ABC',500,mark);
    await permission(b,'kasaba.test.use',true);await permission(b,'kasaba.test.denied',false);
    console.log('PASS Turkish role editing, custom hex color, rank order and exact wildcard denial');
    await command(b,'yetki deneme custom.unregistered ver');await permission(b,'custom.unregistered',true);
    await command(b,'yetki deneme custom.unregistered sil');await permission(b,'custom.unregistered',false);
    console.log('PASS explicit unregistered permissions can be granted and revoked');
    await command(b,'ver '+username+' olmayan');await permission(b,'kasaba.test.use',true);
    await command(b,'sil deneme');await permission(b,'kasaba.test.use',true);
    await command(b,'sil oyuncu');
    console.log('PASS invalid role grants, assigned role deletion and default deletion preserve active roles');
    await command(b,'yenile');await permission(b,'kasaba.test.use',true);
    await b.end();b=await login(username);
    await tab(b,'Deneme Ekibi','#123ABC',500);await permission(b,'kasaba.test.use',true);
    console.log('PASS role and permissions persist across reload and reconnect');
    await command(b,'ver '+username+' rehber');
    await tab(b,'Rehber','aqua',400);await permission(b,'kasaba.test.use',false);
    await command(b,'sil deneme');
    const offline='Off'+Date.now().toString(36);
    await command(b,'ver '+offline+' vip');await b.end();
    b=await login(offline);await tab(b,'VIP','gold',300);await b.end();
    console.log('PASS offline role grant applies on first join');
    console.log('PERSIST_USER='+username);
})().then(()=>process.exit(0)).catch(e=>{console.error('FAIL',e);process.exit(1);});
