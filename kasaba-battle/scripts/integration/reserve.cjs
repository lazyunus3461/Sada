const assert=require('node:assert/strict');
const {connect,clients,delay}=require('../../../kasaba-arena/scripts/integration/regression.cjs');
const {probe,command,state,waitState}=require('./regression.cjs');
let sequence=100;
async function eat(b){b.c.write('held_item_slot',{slotId:0});b.c.write('use_item',{hand:0,sequence:sequence++,rotation:{x:0,y:0}});await delay(1900);return state(b);}
async function run(){
 const suffix=Date.now().toString(36).slice(-6),bots=[];for(const ch of 'AB'){const b=await connect('HP'+suffix+ch);b.c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('kasaba_test:battle')});bots.push(b);}
 const[a,b]=bots;await probe(a,'ADMIN');await probe(a,'EFFECT|3600|0');const outside=await state(a);assert.equal(outside.absorption,4);
 for(const bot of bots)await command(bot,'br solo');await waitState(a,s=>s.room?.players===2);await command(a,'br başlat 1');await waitState(a,s=>s.room?.stage==='OYNANIYOR');assert.equal((await state(a)).absorption,0);
 for(const bot of bots)await probe(bot,'LOOKBOX|any');
 await probe(a,'HEALTH|10');await probe(a,'RESERVE|2');let s=await eat(a);assert.equal(s.absorption,4);assert.equal(s.hp,10);assert.equal(s.reserveDuration,-1);assert(s.items.includes('DRIED_KELP:2'));
 s=await eat(a);assert.equal(s.absorption,4);assert(s.items.includes('DRIED_KELP:2'));
 await probe(a,'RESERVE|3');s=await eat(a);assert.equal(s.absorption,6);assert.equal(s.hp,10);assert(s.items.includes('SWEET_BERRIES:2'));
 await probe(a,'RESERVE|2');s=await eat(a);assert.equal(s.absorption,6);assert(s.items.includes('DRIED_KELP:3'));
 console.log('PASS real eating at full hunger gives exactly 2/3 yellow hearts, normal health stays unchanged, no stacking/downgrade and rejected food is not consumed');
 await probe(b,'HIT|'+a.name+'|2');s=await state(a);assert.equal(s.absorption,4);assert.equal(s.hp,10);
 await probe(a,'RESERVE|3');s=await eat(a);assert.equal(s.absorption,6);await probe(b,'HIT|'+a.name+'|8');s=await state(a);assert.equal(s.absorption,0);assert.equal(s.hp,8);
 await probe(a,'RESERVE|2');s=await eat(a);assert.equal(s.absorption,4);assert.equal(s.hp,8);await probe(a,'COOKIE');s=await eat(a);assert.equal(s.hp,12);assert.equal(s.absorption,4);
 console.log('PASS incoming damage consumes yellow reserve first; depleted reserve refills only to the food limit; existing healing cookie separately restores two red hearts');
 await probe(a,'RESERVE|3');s=await eat(a);assert.equal(s.absorption,6);const end=Date.now()+65000;while(Date.now()<end){await delay(Math.min(10000,end-Date.now()));s=await state(a);assert.equal(s.absorption,6);assert.equal(s.reserveDuration,-1);}
 console.log('PASS yellow reserve remains for 65 real seconds with infinite duration and never replenishes itself after damage');
 await command(a,'br lobi');s=await state(a);assert.equal(s.absorption,0);assert.equal(s.reserveDuration,0);await command(a,'br çık');s=await waitState(a,s=>!s.session&&s.world==='world');assert.equal(s.absorption,outside.absorption);assert(s.reserveDuration>0);
 await command(b,'br çık');s=await waitState(b,s=>!s.session&&s.world==='world');assert.equal(s.absorption,0);assert.equal(s.reserveDuration,0);
 console.log('PASS BR lobby clears bonus hearts; main-lobby exit restores the pre-BR absorption effect and leaves other players with zero bonus');
 // Elimination also clears the reserve and item inventory immediately.
 for(const bot of bots)await command(bot,'br solo');await waitState(a,s=>s.room?.players===2);await command(a,'br başlat 1');await probe(a,'LOOKBOX|any');await probe(a,'RESERVE|3');await eat(a);await probe(b,'HIT|'+a.name+'|1000');s=await waitState(a,s=>!s.room);assert.equal(s.absorption,0);assert.equal(s.reserveDuration,0);assert(!s.items.some(i=>i.startsWith('SWEET_BERRIES')));
 for(const bot of bots){await command(bot,'br çık');await waitState(bot,s=>!s.session);}
 console.log('PASS death clears reserve hearts and bonus foods; PASS ALL RESERVE-HEALTH REGRESSIONS');
}
if(require.main===module)run().catch(error=>{console.error(error);process.exitCode=1}).finally(async()=>{for(const c of clients)c.end();await delay(500)});
