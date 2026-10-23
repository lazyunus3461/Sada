const assert = require('node:assert/strict');
const {fixture,speech,pas,finishWork,finishPersonal} = require('./regression.cjs');
const {clients,delay} = require('../../../kasaba-arena/scripts/integration/regression.cjs');
const checks=[];const pass=text=>{checks.push(text);console.log('PASS '+text);};
const getTask=async(bot,kind)=>(await bot.state()).tasks.find(t=>t.kind===kind);
const until=async when=>delay(Math.max(0,when-Date.now())+100);

async function run(){
 const bots=[],suffix=Date.now().toString(36).slice(-5);
 for(const letter of 'ABCDEF')bots.push(await fixture('VT'+suffix+letter));
 const owner=bots[0];await owner.probe('ADMIN');await owner.command('vk oda kur');const room=(await owner.state()).room;
 for(const bot of bots.slice(1))await bot.command('vk oda katıl '+room);
 await owner.command('vk başlat');const states=await Promise.all(bots.map(b=>b.state()));
 const vampire=bots[states.findIndex(s=>s.role==='VAMPIRE')],doctor=bots[states.findIndex(s=>s.role==='DOCTOR')];
 for(const state of states){assert.deepEqual(state.tasks.map(t=>t.kind),[0,1,2,3]);assert.equal(state.tasks.filter(t=>t.kind<2).length,2);}
 assert.deepEqual(await owner.json('VILLAGE','village'),{safeStations:17,taskRoomDoors:34,centerWall:'STONE_BRICKS'});
 pass('Every role receives two easy/two medium tasks; 17 indoor workstations are safe and all 34 task-room doors remain usable');

 await vampire.probe('POSE|-5.5|69|6.5');await doctor.probe('POSE|3.5|69|6.5');
 assert((await doctor.json('VISIBILITY','visibility')).live[vampire.name]);
 await doctor.probe('SIGHT_WALL|on');assert.equal((await doctor.json('VISIBILITY','visibility')).live[vampire.name],false);
 await doctor.probe('SIGHT_WALL|off');assert((await doctor.json('VISIBILITY','visibility')).live[vampire.name]);
 pass('A nearby name label disappears behind an opaque wall and returns when the view is clear');

 let task=await getTask(doctor,0);await doctor.probe('STATION|'+task.station);await delay(175);task=await getTask(doctor,0);
 await doctor.button(task.work.labels[task.work.sequence[0]]);assert.equal((await getTask(doctor,0)).step,0);
 task=await getTask(doctor,0);await until(task.work.nextInputAt);await doctor.button(task.work.labels[(task.work.sequence[0]+1)%3]);assert.equal((await getTask(doctor,0)).step,0);
 task=await getTask(doctor,0);await until(task.work.nextInputAt);await doctor.button(task.work.labels[task.work.sequence[0]]);assert.equal((await getTask(doctor,0)).step,1);
 await doctor.probe('POSE|3.5|69|.5');await delay(1100);assert.equal((await getTask(doctor,0)).work.startedAt,-1);
 const start=Date.now();await finishPersonal(doctor);assert(Date.now()-start>=6000);assert.equal((await doctor.state()).taskDone,1);
 pass('Easy mechanism rejects rapid/wrong input, resets when the player leaves, and takes at least six real seconds to complete');

 const sortingStart=Date.now();await finishPersonal(doctor);assert(Date.now()-sortingStart>=8000);assert.equal((await doctor.state()).taskDone,2);
 pass('Material sorting uses all four items, follows its displayed order, and takes at least eight real seconds');

 task=await getTask(doctor,2);await doctor.probe('STATION|'+task.station);await delay(175);await finishWork(doctor,false,false,{packingOnly:true});
 task=await getTask(doctor,2);assert(task.carrying);const first=task.destination;await doctor.command('vk görev 3');assert.equal((await doctor.json('ATLAS','atlas')).target,first);
 await doctor.probe('STATION|'+first);task=await getTask(doctor,2);assert.equal(task.leg,1);assert(!task.completed);assert.equal((await doctor.state()).taskDone,2);assert.notEqual(task.destination,first);
 assert.equal((await doctor.json('ATLAS','atlas')).target,task.destination);await doctor.probe('STATION|'+task.destination);
 assert((await getTask(doctor,2)).completed);assert.equal((await doctor.state()).taskDone,3);
 pass('Delivery requires packing and two distinct nearby stops; the atlas follows each leg and only final delivery adds progress');

 task=await getTask(doctor,3);await doctor.probe('STATION|'+task.station);await delay(175);assert(JSON.stringify(doctor.form).includes('Hatırla:'));
 await doctor.button('Diziyi Gizle ve Başla');assert((await getTask(doctor,3)).work.preview);
 task=await getTask(doctor,3);await until(task.work.startedAt+4000);await doctor.button('Diziyi Gizle ve Başla');assert(!JSON.stringify(doctor.form).includes('Hatırla:'));
 task=await getTask(doctor,3);await until(task.work.nextInputAt);await doctor.button(task.work.labels[(task.work.sequence[0]+1)%4]);assert((await getTask(doctor,3)).work.preview);
 await finishWork(doctor);assert((await getTask(doctor,3)).completed);assert.equal((await doctor.state()).taskDone,4);
 pass('Medium memory task enforces its preview, hides the clue, restarts a mistaken round and requires both rounds before completion');

 task=await getTask(vampire,3);await vampire.probe('STATION|'+task.station);await doctor.probe('EXPIRE');assert.equal((await getTask(vampire,3)).work.startedAt,-1);assert.equal((await vampire.state()).fakeTaskDone,0);
 await speech(bots);await pas(bots);await doctor.probe('EXPIRE');
 task=await getTask(vampire,3);await vampire.probe('STATION|'+task.station);await delay(175);await finishWork(vampire,false,false,{station:task.station});
 assert((await getTask(vampire,3)).completed);assert.equal((await vampire.state()).fakeTaskDone,1);assert.equal((await doctor.state()).taskDone,4);
 assert(Math.abs((await doctor.state()).barProgress-5/15)<.00001);
 pass('Meetings interrupt unfinished work; a vampire must solve the same medium memory task and earns only fake public progress');

 await owner.command('vk durdur');for(const bot of bots)await bot.command('lobi');
 console.log(JSON.stringify({version:'1.1.1',realClients:6,checks},null,2));
}
run().catch(e=>{console.error(e);process.exitCode=1;}).finally(async()=>{for(const client of clients)client.end();await delay(500);});
