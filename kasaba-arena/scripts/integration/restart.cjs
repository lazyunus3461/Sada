const assert=require('node:assert/strict');
const path=require('node:path');
const fs=require('node:fs');
const {connect,clients,delay}=require('./regression.cjs');
async function run(){
 const saved=JSON.parse(fs.readFileSync(path.resolve(__dirname,'../../target/restart-state.json')));
 const a=await connect(saved.name),b=await connect(saved.recipient);
 assert.equal((await a.state()).balance,saved.balance);assert.equal((await b.state()).balance,saved.recipientBalance);
 const mark=a.log.length;a.c.write('chat_command',{command:'akçe'});await a.wait(e=>a.msg(e).includes('Bakiyen: 724.50 Akçe'),mark);
 assert((await a.state()).regions.includes('ortak-5'));assert((await a.state()).regions.includes('1v1-1__ana'));
 await a.command('oyunlar git arena');await a.command('arena xarena');const state=await a.state();assert(state.items.includes('APPLE:9:elma'));assert.equal(state.skills,4);assert(state.slots.slice(0,4).every(x=>x.endsWith(':skill')));await a.command('lobi');
 console.log('PASS restart retains real Akce balances, Turkish native command, five regions and purchased consumable stock');
}
run().catch(error=>{console.error(error);process.exitCode=1;}).finally(async()=>{for(const client of clients)client.end();await delay(500);});
