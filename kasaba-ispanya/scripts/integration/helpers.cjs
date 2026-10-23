const assert=require('node:assert/strict');
const {fixture,clients,delay}=require('../../../kasaba-sosyal/scripts/integration/helpers.cjs');
async function player(name){
 const b=await fixture(name);b.c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('kasaba_test:ispanya')});
 b.sp=async op=>{const mark=b.log.length;b.c.write('custom_payload',{channel:'kasaba_test:ispanya',data:Buffer.from(op)});const event=await b.wait(e=>b.msg(e)==='ispanya-probe:done:'+op||b.msg(e).startsWith('ispanya-probe:error:'),mark,45000);assert(!b.msg(event).startsWith('ispanya-probe:error:'),b.msg(event));return mark;};
 b.spjson=async(op,key='state')=>{const mark=await b.sp(op);const text=b.log.slice(mark).map(e=>b.msg(e)).find(s=>s.startsWith('ispanya-probe:'+key+':'));assert(text,key);return JSON.parse(text.slice(('ispanya-probe:'+key+':').length));};b.st=async()=>({room:null,current:null,moving:null,positions:null,skips:null,finished:null,owner:null,...await b.spjson('STATE')});return b;
}
async function until(b,predicate,timeout=45000){const end=Date.now()+timeout;let s;while(Date.now()<end){s=await b.st();if(predicate(s))return s;await delay(100);}throw Error('Spain timeout '+JSON.stringify(s));}
module.exports={player,clients,delay,until};
