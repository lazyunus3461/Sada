const assert=require('node:assert/strict');
const {connect,clients,delay}=require('../../../kasaba-arena/scripts/integration/regression.cjs');
const requireFixture=require('node:module').createRequire(require('node:path').resolve(__dirname,'../../../scripts/integration/package.json'));
const nbt=requireFixture('prismarine-nbt');
const channel={network:['kasaba_test:network','network-probe:'],arena:['kasaba_test:arena','arena-probe:'],battle:['kasaba_test:battle','battle-probe:'],vampir:['kasaba_test:vampir','vampir-probe:']};
async function fixture(name){const b=await connect(name);b.c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from(Object.values(channel).map(c=>c[0]).join('\0'))});
 b.probe=async(op,type='network')=>{const [ch,prefix]=channel[type],mark=b.log.length;b.c.write('custom_payload',{channel:ch,data:Buffer.from(op)});const event=await b.wait(e=>b.msg(e)===prefix+'done:'+op||b.msg(e).startsWith(prefix+'error:'),mark,45000);assert(!b.msg(event).startsWith(prefix+'error:'),b.msg(event));return mark;};
 b.json=async(op,prefix='state',type='network')=>{const mark=await b.probe(op,type),messages=b.log.slice(mark).map(e=>b.msg(e)),start=channel[type][1];let text=messages.find(s=>s.startsWith(start+prefix+':'));if(!text){const chunks=messages.filter(s=>s.startsWith(start+'chunk:')).map(s=>{const match=s.match(/chunk:(\d+):(\d+):([\s\S]*)/);return{index:Number(match[1]),text:match[3]};}).sort((a,b)=>a.index-b.index);text=start+chunks.map(s=>s.text).join('');}assert(text.startsWith(start+prefix+':'),text);return JSON.parse(text.slice((start+prefix+':').length));};
 b.state=()=>b.json('STATE');b.command=cmd=>b.probe('CMD|'+cmd);
 b.page=async cmd=>{const mark=await b.command(cmd);await b.wait(e=>e.name==='show_dialog',mark,45000);await delay(100);return b.form;};
 b.button=async label=>{const buttons=b.form.actions;const index=buttons.findIndex(x=>x.label===label||JSON.stringify(x.label).includes(label));assert(index>=0,'Missing '+label+' in '+JSON.stringify(buttons));b.click(index);await delay(350);};
 b.capture=label=>{const raw=b.log.findLast(e=>e.name==='show_dialog').p.dialog.data;const simple=nbt.simplify(raw);const index=simple.actions.findIndex(a=>JSON.stringify(a.label).includes(label));assert(index>=0,label);const action=raw.value.actions.value.value[index].action.value;const additions=structuredClone(action.additions),id=action.id.value;return()=>b.c.write('custom_click_action',{id,payload:nbt.proto.createPacketBuffer('anonymousNbt',additions)});};
 return b;
}
async function waitState(b,predicate,timeout=45000){const end=Date.now()+timeout;let s;while(Date.now()<end){s=await b.state();if(predicate(s))return s;await delay(150);}throw Error('State timeout '+JSON.stringify(s));}
module.exports={fixture,clients,delay,waitState};
