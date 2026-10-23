const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {player,clients,delay,until}=require('./helpers.cjs');
const checks=[];function pass(text){checks.push(text);console.log('PASS '+text);}
async function cast(b,sum){if(sum)await b.sp('SEED|'+sum);await b.command('ispanya zar');await b.sp('DRAIN');return b.st();}
async function giveTurn(a,b){for(let i=0;i<8;i++){const s=await a.st();if(s.current===s.uuid)return;if(s.current===(await b.st()).uuid){await b.sp('POS|0');await cast(b,2);}else throw Error('Unknown turn');}throw Error('Turn stuck');}
async function races(groups){let total=0;for(let round=0;round<1000;round++){let remaining=false;for(const group of groups){let s=await group[0].st();if(!s.active)continue;remaining=true;const current=group.find(p=>p.uuid===s.current);assert(current,'Missing current player '+JSON.stringify(s));await cast(current);total++;}if(!remaining)return total;}throw Error('Race did not finish');}
async function run(){
 const suffix=Date.now().toString(36).slice(-5),bots=[];for(const ch of 'ABCDEFG')bots.push(await player('IS'+suffix+ch));const [a,b,c,d,e,f,g]=bots;
 for(const p of bots){p.uuid=(await p.st()).uuid;await p.probe('ITEM|0|DIAMOND|7');}await a.probe('ADMIN');
 await a.command('grup oluştur');await a.command('grup davet '+b.name);await b.command('grup katıl '+a.name);
 await a.sp('FAIL|'+b.name+'|true');await a.command('oyunlar git ispanya');await delay(500);
 for(const p of [a,b]){const s=await p.st();assert(!s.session&&!s.playing);assert(s.items.includes('DIAMOND:7'));assert(!s.backup);}
 pass('Cancelled second-player lobby teleport rolls back the entire Hub party, inventories and game membership');
 await a.page('oyunlar');assert(JSON.stringify(a.form).includes('İspanya Adası'));await a.button('İspanya Adası');await until(a,s=>s.world==='ispanya'&&s.social&&s.session);await delay(1100);
 for(const p of [a,b]){const s=await p.st();assert(s.items.includes('COMPASS:1'));assert(s.gravity);assert.equal(s.mode,'ADVENTURE');}
 let mark=await a.sp('COMPASS');await a.wait(x=>x.name==='show_dialog',mark);await delay(100);assert(JSON.stringify(a.form).includes('Oda oluştur'));const stale=a.capture('Oda oluştur');await a.button('Oda oluştur');let s=await a.st();assert(s.room!==null,'Create-room messages: '+a.log.slice(mark).map(e=>a.msg(e)).filter(Boolean).join('\n'));assert.equal(s.room,(await b.st()).room);const colorRoom=s.room;await a.command('ispanya başlat');await until(a,s=>s.active);stale();await delay(200);assert.equal((await a.st()).rooms,1);assert((await a.st()).gameplay);assert(!(await a.st()).social);
 const guards=await a.spjson('GUARDS','guards');assert(Object.values(guards).every(Boolean),JSON.stringify(guards));mark=await b.command('ispanya zar');assert(b.log.slice(mark).some(x=>b.msg(x).includes('sırası sende değil')));assert.equal((await a.st()).moving,null);
 await a.sp('SEED|9');await a.command('ispanya zar');await delay(1100);s=await a.st();assert(s.moving&&s.positions[a.uuid]>0&&s.positions[a.uuid]<9);await a.sp('DRAIN');assert.equal((await a.st()).positions[a.uuid],32);
 await b.sp('TIMEOUT');assert((await b.st()).moving);await b.sp('DRAIN');
 await giveTurn(a,b);await a.sp('POS|17');await cast(a,2);assert.equal((await a.st()).positions[a.uuid],22);
 for(const [from,to] of [[52,79],[106,117]]){await giveTurn(a,b);await a.sp('POS|'+from);await cast(a,2);assert.equal((await a.st()).positions[a.uuid],to);}
 await giveTurn(a,b);await a.sp('POS|8');await a.sp('SEED|2|true');await a.command('ispanya zar');await a.sp('DRAIN');assert.equal((await a.st()).skips[a.uuid],1);await b.sp('POS|0');await cast(b,2);s=await a.st();assert.equal(s.current,b.uuid);assert(!s.skips[a.uuid]);
 await giveTurn(a,b);await a.sp('POS|8');await a.sp('SEED|2|false');await a.command('ispanya zar');await a.sp('DRAIN');assert.equal((await a.st()).positions[a.uuid],7);
 await giveTurn(a,b);await a.sp('POS|15');await cast(a,2);s=await a.st();assert(Math.abs(s.positions[a.uuid]-17)>=1&&Math.abs(s.positions[a.uuid]-17)<=6);
 await giveTurn(a,b);await a.sp('POS|11');await cast(a,2);assert.equal((await a.st()).positions[a.uuid],13);
 pass('Native game compass, party room, stale-button rejection, turn guards, natural movement, automatic dice, all three bridges and red/green/purple/black effects work');
 await a.command('ispanya bitir '+colorRoom);await delay(200);assert.equal((await a.st()).worlds.length,0);
 for(const p of [a,b])await p.command('lobi');await delay(300);await a.command('grup dağıt onayla');
 for(const p of bots){await p.command('oyunlar git ispanya');await until(p,s=>s.session);}
 const groups=[[a,b],[c,d],[e,f]];
 for(const group of groups){await group[0].command('ispanya oluştur');const room=(await group[0].st()).room;await group[1].command('ispanya katıl '+room);}
 await g.command('ispanya oluştur');assert.equal((await g.st()).rooms,3);assert.equal((await g.st()).room,null);
 const failed=(await a.st()).room;await a.sp('FAIL|'+b.name+'|false');await a.command('ispanya başlat');await delay(250);assert.equal((await a.st()).room,null);assert((await a.st()).social);assert.equal((await b.st()).world,'ispanya');
 await a.command('ispanya oluştur');await b.command('ispanya katıl '+(await a.st()).room);
 for(const group of groups)await group[0].command('ispanya başlat');for(const group of groups)await until(group[0],s=>s.active);
 s=await a.st();assert.equal(s.worlds.length,3);assert.equal(new Set(await Promise.all(groups.map(async group=>(await group[0].st()).world))).size,3);
 mark=await a.sp('VOID');assert(a.log.slice(mark).some(x=>a.msg(x)==='ispanya-probe:void:true'));
 pass('Three independent two-player matches coexist; fourth room is denied, failed match teleport returns both players safely, and new chunks remain void');
 const twoThrows=await races(groups);for(const p of bots.slice(0,6)){s=await p.st();assert(!s.active&&s.social&&s.world==='ispanya');assert(p.log.some(x=>p.msg(x).includes('Tüm oyuncular bitirdi. Sıralama:')));}await delay(300);assert.equal((await a.st()).worlds.length,0);
 pass('Three complete two-player races finish every racer, announce ordered results, return to the shared lobby and unload/delete all match worlds ('+twoThrows+' throws)');
 await a.command('ispanya oluştur');let four=(await a.st()).room;for(const p of [b,c,d])await p.command('ispanya katıl '+four);await g.command('ispanya katıl '+four);assert.equal((await g.st()).room,null);await a.command('ispanya başlat');
 await e.command('lobi');await f.command('lobi');for(const p of [e,f]){await p.command('oyunlar git arena');await p.command('arena 1v1');}await delay(1200);assert.equal((await e.state()).place,'MATCH');assert((await a.st()).active);await e.command('lobi');await f.command('lobi');
 const fourThrows=await races([[a,b,c,d]]);await delay(250);assert.equal((await a.st()).worlds.length,0);
 pass('A full four-player race preserves turn/finish order, rejects a fifth entrant and coexists with the existing 1v1 Arena ('+fourThrows+' throws)');
 for(const p of [a,b,c,d,g])await p.command('lobi');await delay(300);for(const p of [a,b,c,d,e,f,g]){s=await p.st();assert(!s.session&&!s.backup&&!s.playing);assert(s.items.includes('DIAMOND:7'),p.name+' inventory');assert(s.gravity);assert.equal(s.mode,'SURVIVAL');}
 for(const id of ['battle','vampir']){await a.command('oyunlar git '+id);assert((await a.state()).social);assert(!(await a.st()).session);await a.command('lobi');}await a.command('vip');assert(a.form||a.log.some(x=>x.name==='show_dialog'));
 pass('Original inventory/mode/gravity return on /lobi; BR and Vampire lobbies plus /vip remain usable with the Spain plugin loaded');
 await a.command('oyunlar git ispanya');await g.command('oyunlar git ispanya');await a.command('ispanya oluştur');await g.command('ispanya katıl '+(await a.st()).room);await a.command('ispanya başlat');await a.command('ispanya zar');a.c.end();await delay(600);s=await g.st();assert(s.active&&s.current===g.uuid&&!s.moving);await g.command('lobi');await delay(250);assert.equal((await g.st()).worlds.length,0);
 pass('Disconnect during movement hands the turn to the remaining racer; leaving the final seat removes the world');
 await c.probe('ITEM|0|GOLD_INGOT|11');await c.probe('CONSOLE|effect give '+c.name+' minecraft:absorption 600 0 true');await delay(150);await c.probe('ABS|4');await c.probe('CONSOLE|experience set '+c.name+' 7 levels');await c.command('oyunlar git ispanya');s=await c.st();assert(s.backup);const root=path.resolve(__dirname,'../../target');fs.mkdirSync(root,{recursive:true});const source=process.env.KASABA_TEST_SERVER+'/plugins/KasabaIspanya/recoveries/'+c.uuid+'.yml';fs.copyFileSync(source,root+'/pending-recovery.yml');await c.command('lobi');await delay(250);
 const result={version:'0.4.0',names:bots.map(p=>p.name),uuid:c.uuid,recoveryName:c.name,checks,maximumClients:7,twoPlayerGames:3,twoPlayerThrows:twoThrows,fourPlayerGames:1,fourPlayerThrows:fourThrows,sourceMapPreserved:true,restartPending:true};fs.writeFileSync(root+'/live-results.json',JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify(result,null,2));
}
run().catch(e=>{console.error(e);process.exitCode=1;}).finally(async()=>{for(const c of clients)c.end();await delay(500);});
