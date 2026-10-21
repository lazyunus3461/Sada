const assert=require('node:assert/strict');
const fs=require('node:fs');
const {login}=require('../../../kasaba-roles/scripts/integration/chat.cjs');
const {delay,text}=require('../../../scripts/integration/smoke.cjs');
const state=JSON.parse(fs.readFileSync('/workspace/Sada/kasaba-clan/target/integration-state.json'));
async function probe(b,operation){const mark=b.log.length;b.c.write('custom_payload',{channel:'kasaba_test:clan',data:Buffer.from(operation)});await b.wait('system_chat',p=>text(p).includes('clan-probe:done:'+operation),mark);return mark;}
async function cmd(b,cmd,expected){const mark=await probe(b,'PLAYER|'+cmd);await b.wait('system_chat',p=>text(p).includes(expected),mark);}
async function access(b,denied){const mark=await probe(b,'ACCESS');await b.wait('system_chat',p=>text(p).includes('access:break='+denied+',place='+denied+',bucket='+denied+',high='+denied+',chest='+denied+',entity='+denied+',pvp=false'),mark);}
(async()=>{
    const a=await login(state.admin),b=await login(state.member),o=await login(state.outsider);
    await probe(a,'CONSOLE|rol ver '+o.c.username+' kurucu');await access(o,true);
    await cmd(o,'klan yönetim aç','açıldı');await access(o,false);
    await probe(a,'CONSOLE|rol al '+o.c.username);await access(o,true);
    await probe(a,'CONSOLE|rol ver '+o.c.username+' kurucu');await cmd(o,'klan yönetim kapat','kapatıldı');await access(o,true);await probe(a,'CONSOLE|rol al '+o.c.username);
    await cmd(a,'klan yetki '+b.c.username+' üye','güncellendi');await cmd(b,'klan alan seç','lideri veya yardımcısına');await cmd(a,'klan yetki '+b.c.username+' yardımcı','güncellendi');
    await probe(a,'CONSOLE|rol ver '+a.c.username+' kurucu');await cmd(a,'klan alan liste','#1');
    await a.end();await b.end();await o.end();console.log('PASS admin has no implicit claim bypass; explicit bypass can be closed and permission revocation takes effect immediately; ordinary members cannot manage claims');
})().then(()=>process.exit(0)).catch(e=>{console.error('FAIL',e);process.exit(1)});
