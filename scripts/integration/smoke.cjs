const assert = require('node:assert/strict');
const mc = require('minecraft-protocol');
const delay = ms => new Promise(r => setTimeout(r,ms));
const text = p => JSON.stringify(p);
async function connect(username) {
 const c=mc.createClient({host:'127.0.0.1',port:Number(process.env.KASABA_TEST_PORT || 25575),username,version:'1.21.10',auth:'offline'});
 const log=[]; let window=0,state=0,position,clientError;
 c.on('packet',(p,m)=>{
  log.push({name:m.name,p});
  if(m.name==='open_window') window=p.windowId;
  if((m.name==='window_items'||m.name==='set_slot')&&p.windowId===window) state=p.stateId;
  if(m.name==='position'){position=p;c.write('teleport_confirm',{teleportId:p.teleportId});c.write('player_loaded',{});}
 });
 c.on('error',e=>{clientError=e;});
 const wait=async (name,predicate=()=>true,from=0)=>{
  const end=Date.now()+12000;
  while(Date.now()<end){if(clientError)throw clientError;const found=log.slice(from).find(e=>e.name===name&&predicate(e.p));if(found)return found.p;await delay(25);}
  throw new Error('Timed out waiting for '+name+'; packets: '+log.slice(-8).map(e=>e.name).join(','));
 };
 await wait('open_window',p=>text(p).includes('Kasaba’ya Hoş Geldin'));
 await delay(150);
 return {c,log,wait,get position(){return position},close(){c.write('close_window',{windowId:window});},click(slot){c.write('window_click',{windowId:window,stateId:state,slot,mouseButton:0,mode:0,changedSlots:[],cursorItem:null});},
  async menuClick(slot,title){let start=log.length;this.click(slot);await wait('open_window',p=>text(p).includes(title),start);await delay(150);},
  async password(value){c.write('name_item',{name:value});await delay(200);this.click(2);},
  async end(){c.end();await delay(500);}
 };
}
(async()=>{
 const status=await mc.ping({host:'127.0.0.1',port:Number(process.env.KASABA_TEST_PORT||25575),version:'1.21.10',closeTimeout:3000});
 assert(status.version.name.includes('1.21.10'));
 console.log('PASS real Paper 1.21.10 status response');
 const username=process.env.KASABA_TEST_USER || ('KT'+Date.now().toString(36));
 let b=await connect(username);
 if(process.env.KASABA_LOGIN_ONLY==='1'){
  await b.menuClick(11,'Giriş Yap');
  const mark=b.log.length;await b.password('Kasaba-Test-123');
  await b.wait('system_chat',p=>text(p).includes('keşfedebilirsin'),mark);
  console.log('PASS account authenticates after full server restart');await b.end();return;
 }
 assert(b.log.some(e=>e.name==='boss_bar'&&text(e.p).includes('Kasaba’ya Hoş Geldin')));
 console.log('PASS welcome menu and upper welcome bar');
 let reopen=b.log.length; b.close();
 await b.wait('open_window',p=>text(p).includes('Kasaba’ya Hoş Geldin'),reopen); await delay(150);
 console.log('PASS closed menu reopens');
 let start=b.log.length;
 b.c.write('chat_command',{command:'help'});
 await b.wait('system_chat',p=>text(p).includes('Önce giriş yap'),start);
 console.log('PASS commands blocked before authentication');
 const origin=b.position;
 start=b.log.length;
 b.c.write('position',{x:origin.x+2,y:origin.y,z:origin.z,flags:{onGround:true,hasHorizontalCollision:false}});
 b.c.write('tick_end',{});
 const correction=await b.wait('position',()=>true,start);
 assert.equal(correction.x,origin.x);
 console.log('PASS movement blocked before authentication');
 await b.menuClick(15,'Kayıt Ol');
 start=b.log.length; await b.password('Kasaba-Test-123');
 await b.wait('open_window',p=>text(p).includes('Şifreni Tekrarla'),start);
 console.log('PASS registration opens confirmation screen');
 start=b.log.length; await b.password('Different-Test-123');
 await b.wait('system_chat',p=>text(p).includes('eşleşmedi'),start);
 await b.wait('open_window',p=>text(p).includes('Kayıt Ol'),start);
 console.log('PASS mismatched confirmation rejected');
 start=b.log.length; await b.password('Kasaba-Test-123');
 await b.wait('open_window',p=>text(p).includes('Şifreni Tekrarla'),start);await delay(150);
 start=b.log.length; await b.password('Kasaba-Test-123');
 await b.wait('system_chat',p=>text(p).includes('keşfedebilirsin'),start);
 console.log('PASS new account registration');
 await b.end();
 b=await connect(username);
 await b.menuClick(11,'Giriş Yap');
 start=b.log.length; await b.password('Wrong-Test-123');
 await b.wait('system_chat',p=>text(p).includes('Şifre yanlış'),start);
 await b.wait('open_window',p=>text(p).includes('Giriş Yap'),start);await delay(150);
 console.log('PASS wrong password rejected after reconnect');
 start=b.log.length;await b.password('Kasaba-Test-123');
 await b.wait('system_chat',p=>text(p).includes('keşfedebilirsin'),start);
 start=b.log.length;b.c.write('chat_command',{command:'kasaba'});
 await b.wait('system_chat',p=>text(p).includes('Zaten giriş yaptın'),start);
 console.log('PASS saved password authenticates and unlocks player');
 await b.end();
 b=await connect(username);
 await b.menuClick(11,'Giriş Yap');
 for(let attempt=1;attempt<=5;attempt++){
  start=b.log.length;await b.password('Wrong-Test-123');
  if(attempt<5){await b.wait('system_chat',p=>text(p).includes('Şifre yanlış'),start);await b.wait('open_window',p=>text(p).includes('Giriş Yap'),start);await delay(150);}
  else await b.wait('kick_disconnect',p=>text(p).includes('Çok fazla'),start);
 }
 console.log('PASS repeated wrong passwords trigger lockout');
 await b.end();
 const denied=mc.createClient({host:'127.0.0.1',port:Number(process.env.KASABA_TEST_PORT||25575),username,version:'1.21.10',auth:'offline'});
 await new Promise((resolve,reject)=>{const timer=setTimeout(()=>{denied.end();reject(new Error('Reconnect lockout missing'));},12000);denied.on('kick_disconnect',p=>{if(text(p).includes('Çok fazla')){clearTimeout(timer);resolve();}});denied.on('error',reject);});
 denied.end();console.log('PASS lockout survives reconnect');
})().then(()=>process.exit(0)).catch(e=>{console.error('FAIL',e);process.exit(1)});
