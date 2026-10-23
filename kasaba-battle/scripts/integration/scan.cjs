const assert=require('node:assert/strict');
const {connect,clients,delay}=require('../../../kasaba-arena/scripts/integration/regression.cjs');
const {probe,command,state,waitState,boxes}=require('./regression.cjs');
async function run(){
 const suffix=Date.now().toString(36).slice(-6),bots=[];for(const ch of 'AB'){const b=await connect('SCAN'+suffix+ch);b.c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('kasaba_test:battle')});bots.push(b);}
 const[a,b]=bots;await probe(a,'ADMIN');const from=await command(a,'br sandık tara 1 800');assert(a.log.slice(from).some(e=>a.msg(e).includes('800 sandık adayı kaydedildi')));
 const mark=await probe(a,'POOLS'),entry=a.log.slice(mark).find(e=>a.msg(e).startsWith('battle-probe:pools:'));const pools=JSON.parse(a.msg(entry).slice('battle-probe:pools:'.length));assert.equal(pools[0].count,800);assert.equal(pools[0].active,800);assert.equal(pools[0].unsafe,0);
 for(const bot of bots)await command(bot,'br solo');await waitState(a,s=>s.room?.players===2);await command(a,'br başlat 1');const s=await waitState(a,s=>s.room?.stage==='OYNANIYOR');assert.equal(s.room.chests,240);assert(s.items.includes('WOODEN_SWORD:1'));assert.equal(s.armor.length,4);
 const loot=await boxes(a);assert(loot.every(c=>c.pos[0]>=-125&&c.pos[0]<125&&c.pos[2]>=-125&&c.pos[2]<125));await command(a,'br bitir 1');for(const bot of bots){await command(bot,'br çık');await waitState(bot,s=>!s.session);}
 console.log('PASS manual /br sandık tara 1 800 saves 800 safe central points; two-player game has 240 chests inside 250x250 and the starting sword/armor; PASS ALL CENTRAL SCAN REGRESSIONS');
}
if(require.main===module)run().catch(error=>{console.error(error);process.exitCode=1}).finally(async()=>{for(const c of clients)c.end();await delay(500)});
