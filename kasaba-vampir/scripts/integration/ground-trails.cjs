const assert=require('node:assert/strict');
const fs=require('node:fs');
const {fixture,finishWork}=require('./regression.cjs');
const {clients,delay}=require('../../../kasaba-arena/scripts/integration/regression.cjs');
const stations=require('../../world-template/vampirkoylu_sokaklar-konumlar.json').gorev_istasyonlari;
const checks=[];const pass=text=>{checks.push(text);console.log('PASS '+text);};
const floorDust=event=>event.name==='world_particles'&&Math.abs(event.p.y-69.08)<.00001;

async function run(){
 const suffix=Date.now().toString(36).slice(-5),bots=[];
 for(const ch of 'ABCDEF')bots.push(await fixture('GT'+suffix+ch));
 const [owner,observer]=bots;await owner.probe('ADMIN');await owner.command('vk oda kur');
 const room=(await owner.state()).room;
 for(const bot of bots.slice(1))await bot.command('vk oda katıl '+room);
 await owner.command('vk başlat');
 for(const bot of bots){const s=await bot.state();assert.equal(s.held,6);assert(s.items.includes('COMPASS:1'));assert(!s.items.includes('FILLED_MAP:1'));}
 pass('All six roles start with the task compass instead of a held map');

 const routes=await owner.json('ROUTES','routes');assert.equal(routes.routes,340);assert(routes.doorCrossings>=340,JSON.stringify(routes));
 pass('All 20 start seats reach all 17 stations on real Paper terrain: 340 cardinal routes, solid floors, usable doors, no wall or diagonal shortcuts');

 for(const bot of bots.slice(1))await bot.probe('TASK_FLAGS|complete');
 await owner.probe('POSE|3.5|69|.5');await observer.probe('POSE|3.5|69|.5');
 await owner.command('vk görev 1');
 const mark=owner.log.length,otherMark=observer.log.length;await delay(1200);
 const dust=owner.log.slice(mark).filter(floorDust);
 assert(dust.length>0,'No floor particles received: '+JSON.stringify(owner.log.slice(mark).filter(e=>e.name.includes('particle')).slice(0,2)));
 assert.equal(observer.log.slice(otherMark).filter(floorDust).length,0);
 const trail=await owner.json('TRAIL','trail');assert.equal(trail.target,(await owner.state()).tasks[0].station);assert(trail.arrows>0&&trail.arrows<=10);
 for(const event of dust){assert.equal(event.p.amount,1);assert(Math.abs(event.p.y-69.08)<.00001);}
 pass('Only the task owner receives ground-arrow packets; another living player on the exact same block receives none');

 const start=trail.steps[0],next=trail.steps[1];assert(next);
 await owner.probe(`POSE|${next.x+.5}|69|${next.z+.5}`);
 const moved=await owner.json('TRAIL','trail');assert.equal(moved.target,trail.target);assert.equal(moved.distance,trail.distance-1);assert.deepEqual(moved.steps[0],next);
 await owner.probe('POSE|-5.5|69|6.5');const changed=await owner.json('TRAIL','trail');
 assert.equal(changed.target,trail.target);assert.deepEqual(changed.steps[0],{x:-6,z:6});
 assert(!changed.instruction.includes('↗')&&!changed.instruction.includes('↘'));
 pass('Walking advances the route by one tile; taking another street recalculates from the new block without switching the chosen task');

 const station=(await owner.state()).tasks[0].station,[sx,,sz]=stations[station].konum;
 await owner.probe(`POSE|${sx-3}|69|${sz-7}`);await owner.probe(`DOOR|close|${station}`);
 let closed=await owner.json('TRAIL','trail');assert(closed.arrows>0);
 const beforeDoor=closed.steps[closed.arrows-1];await owner.probe(`POSE|${beforeDoor.x+.5}|69|${beforeDoor.z+.5}`);
 closed=await owner.json('TRAIL','trail');assert(closed.instruction.includes('kapıyı'),JSON.stringify(closed));assert.equal(closed.arrows,1);
 await owner.probe(`DOOR|open|${station}`);const open=await owner.json('TRAIL','trail');assert(open.arrows>closed.arrows);
 pass('The trail stops at a closed wooden door, asks the player to open it and continues inside when it opens');

 for(const bot of bots.slice(1))await bot.probe('TASK_FLAGS|restore');
 const task=(await owner.state()).tasks.find(t=>t.kind===2);await owner.command('vk görev 3');
 await owner.probe('STATION|'+task.station);await delay(175);
 await finishWork(owner,false,false,{station:task.station,packingOnly:true});
 let carrying=(await owner.state()).tasks.find(t=>t.kind===2);
 assert.equal((await owner.json('TRAIL','trail')).target,carrying.destination);
 await owner.probe('STATION|'+carrying.destination);carrying=(await owner.state()).tasks.find(t=>t.kind===2);
 assert.equal(carrying.leg,1);assert.equal((await owner.json('TRAIL','trail')).target,carrying.destination);
 await owner.probe('STATION|'+carrying.destination);
 assert((await owner.state()).tasks.find(t=>t.kind===2).completed);
 const remaining=(await owner.state()).tasks.filter(t=>!t.completed).map(t=>t.carrying?t.destination:t.station);assert(remaining.includes((await owner.json('TRAIL','trail')).target));
 pass('Packing and each delivery automatically retarget the path; completed work switches to the next unfinished task');

 await owner.probe('BELL');assert.equal((await owner.state()).phase,'SPEECH');
 const meetingMark=owner.log.length;await delay(1200);assert.equal(owner.log.slice(meetingMark).filter(floorDust).length,0);
 pass('Ground arrows stop during meetings');
 await owner.command('vk durdur');for(const bot of bots)await bot.command('lobi');
 const exitMark=owner.log.length;await delay(700);assert.equal(owner.log.slice(exitMark).filter(floorDust).length,0);
 assert.equal((await owner.state()).roomCount,0);
 const report={version:'1.1.1',realClients:6,...routes,checks};
 fs.writeFileSync(require('node:path').resolve(__dirname,'../../validation/ground-trails.json'),JSON.stringify(report,null,2)+'\n');
 console.log(JSON.stringify(report,null,2));
}
run().catch(error=>{console.error(error);process.exitCode=1;}).finally(async()=>{for(const client of clients)client.end();await delay(500);});
