const assert=require('node:assert/strict');
const {connect,clients,delay}=require('./regression.cjs');
async function run(){
 const suffix=Date.now().toString(36).slice(-6),a=await connect('SA'+suffix),b=await connect('SB'+suffix);
 for(const bot of[a,b])await bot.probe('ADMIN');
 // Save a kit while Hub owns the menu compass: that compass must not become a duel item.
 await a.probe('ITEM|STONE_SWORD');await a.command('arena kit normal kaydet');
 await a.pose(78,78,0,-62);await a.command('arena bölge pos1');await a.pose(86,86,0,-55);await a.command('arena bölge pos2');await a.command('arena bölge kaydet lobi ek');
 for(const room of[1,2]){await a.pose(900,900,0,-62);await a.command('arena bölge pos1');await a.pose(910,910,0,-55);await a.command('arena bölge pos2');await a.command('arena bölge kaydet 1v1 '+room);}
 for(const bot of[a,b])await bot.command('oyunlar git arena');
 await a.command('arena oda oluştur');assert.equal((await a.state()).room,'1v1-1');await b.command('arena 1v1');assert.equal((await b.state()).room,'1v1-3');await b.command('arena lobi');await b.command('arena katıl '+(await a.state()).code);assert(!(await b.state()).items.some(item=>item.startsWith('COMPASS:')));await a.command('arena lobi');
 console.log('PASS intersecting regions lock different room numbers and normal kit excludes the Hub menu compass');
 for(const bot of[a,b])await bot.command('arena xarena');await a.pose(81,82,-90);await b.pose(83,82,90);await a.probe('READY');await a.probe('HIT|'+b.name);assert.equal((await b.state()).hp,20);await b.pose(122,122);await a.probe('READY');await a.probe('HIT|'+b.name);assert.equal((await b.state()).hp,20);for(const bot of[a,b])await bot.command('lobi');
 console.log('PASS named safe lobby prevents damage even when X participants physically enter it or attack out of it');
}
run().catch(error=>{console.error(error);process.exitCode=1;}).finally(async()=>{for(const c of clients)c.end();await delay(500);});
