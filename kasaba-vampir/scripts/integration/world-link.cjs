const assert=require('node:assert/strict');
const fs=require('node:fs');
const {fixture}=require('./regression.cjs');
const {clients,delay}=require('../../../kasaba-arena/scripts/integration/regression.cjs');
const checks=[];const pass=text=>{checks.push(text);console.log('PASS '+text);};

async function run(){
 const suffix=Date.now().toString(36).slice(-5),a=await fixture('WL'+suffix+'A'),b=await fixture('WL'+suffix+'B');
 await a.probe('ADMIN');
 const initial=await a.json('LINK','link');assert(initial.available&&initial.registered,JSON.stringify(initial));
 const expected=process.env.EXPECTED_WORLD||'vampirkoylu_uc_oda';assert.equal(initial.source,expected);
 await a.probe('COMPASS');await delay(200);await a.button('Oyunlar');await a.button('Vampir Köylü');
 let compass=await a.json('LINK','link'),state=await a.state();
 assert.equal(state.world,expected);assert.equal(compass.currentUid,initial.sourceUid);assert.notEqual(compass.floor,'AIR');
 assert(Math.abs(state.x-144.5)<.01&&state.y===69&&Math.abs(state.z-.5)<.01,JSON.stringify(state));
 pass('Real Hub compass buttons enter the populated public lobby at 144.5 / 69 / 0.5');
 let mark=await a.command('mv info');await delay(500);
 const info=a.log.slice(mark).map(e=>a.msg(e)).filter(Boolean).join('\n');
 assert(info.toLowerCase().includes(expected.toLowerCase()),info);assert(!info.toLowerCase().includes('does not exist'),info);
 pass('Real Multiverse /mv info recognizes the world reached from the compass');
 await a.command('lobi');await a.command('mvtp '+expected);await delay(1200);
 let direct=await a.json('LINK','link');state=await a.state();
 assert.equal(direct.currentUid,compass.currentUid);assert.equal(state.world,expected);assert.notEqual(direct.floor,'AIR');
 assert(Math.abs(state.x-144.5)<.01&&state.y===69&&Math.abs(state.z-.5)<.01,JSON.stringify(state));
 pass('Real /mvtp and compass enter the same world UUID and the same solid lobby position');
 await a.probe('BAD_LOBBY');assert.equal((await a.json('LINK','link')).available,false);
 await a.command('vk dünya bağla');assert((await a.json('LINK','link')).available);
 pass('An invalid lobby is unavailable instead of teleporting into empty space; /vk dünya bağla restores the populated link');
 await a.probe('USER');await a.command('vk dünya bağla');assert((await a.json('LINK','link')).available);await a.probe('ADMIN');
 await a.command('vk oda kur');const id=(await a.state()).room;await b.command('vk oda katıl '+id);await a.command('vk deneme');
 assert.equal((await a.state()).phase,'DAY');assert.deepEqual(await a.json('VILLAGE','village'),{safeStations:17,taskRoomDoors:34,centerWall:'STONE_BRICKS'});
 assert.equal((await a.json('ROUTES','routes')).routes,340);
 pass('A match copies the linked populated world and exposes 17 real stations with 340 walkable routes');
 await a.command('vk durdur');for(const player of [a,b])await player.command('lobi');
 const result={version:'1.2.1',multiverse:'5.3.2',world:expected,checks};
 fs.writeFileSync(require('node:path').resolve(__dirname,'../../validation/world-link.json'),JSON.stringify(result,null,2)+'\n');
 console.log(JSON.stringify(result,null,2));
}
run().catch(error=>{console.error(error);process.exitCode=1;}).finally(async()=>{for(const client of clients)client.end();await delay(500);});
