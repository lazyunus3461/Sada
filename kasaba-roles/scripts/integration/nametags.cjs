const assert=require('node:assert/strict');
const {login,command,plain}=require('./chat.cjs');
const {text}=require('../../../scripts/integration/smoke.cjs');
const {simplify}=require('../../../scripts/integration/node_modules/prismarine-nbt');
async function prefix(viewer,target,label,color,from=0) {
    const entry=await viewer.wait('teams',p=>p.players?.includes(target)&&p.team.startsWith('kr'),from);
    const packet=await viewer.wait('teams',p=>p.team===entry.team&&p.prefix&&plain(simplify(p.prefix))==='['+label+'] ',from);
    assert(text(simplify(packet.prefix)).toLowerCase().includes(color.toLowerCase()));
    assert.equal(packet.nameTagVisibility,'always');return entry.team;
}
async function board(b,action,expected) {
    const mark=b.log.length;b.c.write('custom_payload',{channel:'kasaba_test:roles',data:Buffer.from('BOARD|'+action)});
    await b.wait('system_chat',p=>text(p).includes('roles-probe:'+expected),mark);
}
(async()=>{
    const id=Date.now().toString(36),a=await login('Head'+id),b=await login('Watch'+id);
    await prefix(b,a.c.username,'Oyuncu','gray');
    let mark=b.log.length;await command(a,'ver '+a.c.username+' kurucu');
    // Existing membership need not be re-sent when only the prefix changes.
    const team=b.log.filter(e=>e.name==='teams'&&e.p.players?.includes(a.c.username)).at(-1).p.team;
    await b.wait('teams',p=>p.team===team&&p.prefix&&plain(simplify(p.prefix))==='[Kurucu] ',mark);
    console.log('PASS observer receives the colored native overhead role team and immediate role change');
    mark=b.log.length;await board(b,'CUSTOM','custom-board');
    const customTeam=await prefix(b,a.c.username,'Kurucu','#ff4545',mark);
    await board(b,'CHECK','sidebar=true');
    console.log('PASS overhead tags follow a new viewer scoreboard without replacing its sidebar');
    mark=b.log.length;await command(a,'ver '+a.c.username+' rehber');
    await b.wait('teams',p=>p.team===customTeam&&p.prefix&&plain(simplify(p.prefix))==='[Rehber] ',mark);
    await board(b,'CHECK','sidebar=true');
    console.log('PASS role changes update both shared and private scoreboard tags');
    mark=b.log.length;await a.end();
    await b.wait('teams',p=>p.team===customTeam&&p.mode==='remove',mark);
    await board(b,'CHECK','sidebar=true');await b.end();
    console.log('PASS quit removes only owned role teams and preserves the existing sidebar');
})().then(()=>process.exit(0)).catch(e=>{console.error('FAIL',e);process.exit(1);});
