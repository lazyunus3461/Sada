const assert=require('node:assert/strict');
const {connect,delay,text}=require('../../../scripts/integration/smoke.cjs');
const {simplify}=require('../../../scripts/integration/node_modules/prismarine-nbt');
async function login(name) {
    const b=await connect(name),mark=b.log.length;
    b.submit('Kasaba',b.form.inputs.length===2?'Kasaba':undefined);
    await b.wait('system_chat',p=>text(p).includes('keşfedebilirsin'),mark);return b;
}
async function command(b,command) {
    const mark=b.log.length;
    b.c.write('custom_payload',{channel:'kasaba_test:roles',data:Buffer.from('COMMAND|rol '+command)});
    await b.wait('system_chat',p=>text(p).includes('roles-probe:command-done'),mark);
}
function plain(c){return typeof c==='string'?c:(c?.text||c?.['']||'')+(c?.extra||[]).map(plain).join('');}
function component(packet){
    const raw=packet.unsignedChatContent||packet.message||packet.content;
    return raw?.type?simplify(raw):raw;
}
async function received(b,from,message) {
    const until=Date.now()+8000;
    while(Date.now()<until){
        const found=b.log.slice(from).find(e=>['profileless_chat','player_chat','system_chat'].includes(e.name)&&plain(component(e.p)).includes(message));
        if(found)return component(found.p);
        const kick=b.log.slice(from).find(e=>e.name==='kick_disconnect');if(kick)throw new Error(text(kick));
        await delay(25);
    }
    throw new Error('Chat not received; chat packets '+text(b.log.slice(from).filter(e=>e.name.includes('chat'))));
}
async function chat(sender,receiver,label,color,message) {
    const marks=[sender.log.length,receiver.log.length];sender.c.chat(message);
    for(const [i,b] of [sender,receiver].entries()) {
        const result=await received(b,marks[i],message);
        assert.equal(plain(result),'['+label+'] '+sender.c.username+' » '+message);
        assert(text(result).toLowerCase().includes(color.toLowerCase()));
    }
}
async function run(){
    const id=Date.now().toString(36),a=await login('Chat'+id),b=await login('View'+id);
    await command(a,'ver '+a.c.username+' kurucu');
    await chat(a,b,'Kurucu','#ff4545','Merhaba Kasaba!');
    console.log('PASS sender and observer receive the same colored Kurucu chat prefix and original message');
    await command(a,'ver '+a.c.username+' rehber');
    await chat(a,b,'Rehber','aqua','Rehber buradayim.');
    console.log('PASS role change immediately updates asynchronous chat formatting');
    await command(a,'renk rehber #12ABEF');await command(a,'isim rehber Yardım Ekibi');
    await chat(a,b,'Yardım Ekibi','#12abef','Yardimci olabilirim.');
    await command(a,'renk rehber turkuaz');await command(a,'isim rehber Rehber');
    console.log('PASS existing Turkish name/color commands also update chat prefixes');
    await command(a,'al '+a.c.username);
    await chat(a,b,'Oyuncu','gray','Normal oyuncuyum.');
    console.log('PASS removing a role replaces the chat prefix without leaving the old role');
    await a.end();await b.end();
}
module.exports={login,command,plain};
if(require.main===module)run().then(()=>process.exit(0)).catch(e=>{console.error('FAIL',e);process.exit(1);});
