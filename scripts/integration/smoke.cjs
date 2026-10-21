const assert = require('node:assert/strict');
const mc = require('minecraft-protocol');
const nbtCodec = require('prismarine-nbt');
const simplify = nbtCodec.simplify;
const delay = ms => new Promise(r => setTimeout(r, ms));
const text = p => JSON.stringify(p);
const componentText=c=>typeof c==='string'?c:Array.isArray(c)?c.map(componentText).join(''):(c?.text||'')+(c?.extra||[]).map(componentText).join('');
// Paper 1.21.10's ServerboundCustomClickActionPacket uses a VarInt-length-
// prefixed optional NBT tag (ByteBufCodecs.lengthPrefixed), not a Boolean.
// Override the outdated minecraft-data definition only in this test client.
const customPackets = {'1.21':{types:{packet_common_custom_click_action:
    ['container',[{name:'id',type:'string'},{name:'payload',type:['buffer',{countType:'varint'}]}]]}}};
// Native 1.21.10 Action enum places UPDATE_LIST_ORDER before UPDATE_HAT.
// minecraft-data 1.21.9 reverses those flags, misreading a rank as a Boolean.
const playerInfo=structuredClone(require('minecraft-data')('1.21.10').protocol.play.toClient.types.packet_player_info);
playerInfo[1][0].type[1].flags=['add_player','initialize_chat','update_game_mode','update_listed',
    'update_latency','update_display_name','update_list_order','update_hat'];
customPackets['1.21'].play={toClient:{types:{packet_player_info:playerInfo}}};
const options = {customPackets,host:'127.0.0.1',port:Number(process.env.KASABA_TEST_PORT || 25575),version:'1.21.10',auth:'offline'};
const PASSWORD = 'Kasaba'; // Synthetic test accounts only.
async function connect(username) {
    const c = mc.createClient({...options, username});
    const log = [];
    let dialog, position, clientError;
    c.on('packet', (p,m) => {
        log.push({name:m.name,p,at:Date.now()});
        if (m.name === 'show_dialog') dialog = p.dialog.data;
        if (m.name === 'position') {
            position=p;
            c.write('teleport_confirm',{teleportId:p.teleportId});
            c.write('player_loaded',{});
        }
    });
    c.on('error',e=>{clientError=e;});
    const wait = async (name,predicate=()=>true,from=0) => {
        const deadline=Date.now()+12000;
        while(Date.now()<deadline) {
            if(clientError) throw clientError;
            const kicked=log.slice(from).find(e=>e.name==='kick_disconnect');
            if(kicked&&name!=='kick_disconnect')throw new Error('Disconnected: '+text(kicked.p));
            const found=log.slice(from).find(e=>e.name===name && predicate(e.p));
            if(found) return found.p;
            await delay(25);
        }
        throw new Error('Timed out waiting for '+name+'; recent packets: '+log.slice(-8).map(e=>e.name).join(','));
    };
    await wait('show_dialog');
    return {
        c,log,wait,
        get position(){return position;},
        get form(){return simplify(dialog);},
        action(index=0) {
            return structuredClone(dialog.value.actions.value.value[index].action.value);
        },
        send(action,password,confirmation) {
            const nbt=structuredClone(action.additions);
            if(password !== undefined) nbt.value.password={type:'string',value:password};
            if(confirmation !== undefined) nbt.value.confirm={type:'string',value:confirmation};
            c.write('custom_click_action',{id:action.id.value,payload:nbtCodec.proto.createPacketBuffer('anonymousNbt',nbt)});
        },
        submit(password,confirmation) {this.send(this.action(),password,confirmation);},
        async end() {c.end();await delay(450);}
    };
}
async function expectForm(b,from,contains) {
    return b.wait('show_dialog',p=>text(p).includes(contains),from);
}
async function run(){
    const status=await mc.ping({...options,closeTimeout:3000});
    assert(status.version.name.includes('1.21.10'));
    console.log('PASS real Paper 1.21.10 status response');
    const username=process.env.KASABA_TEST_USER || ('KF'+Date.now().toString(36));
    let b=await connect(username);
    if(process.env.KASABA_LOGIN_ONLY==='1') {
        assert.deepEqual(b.form.inputs.map(i=>i.key),['password']);
        const mark=b.log.length;b.submit('Kasaba-Test-123');
        await b.wait('system_chat',p=>text(p).includes('keşfedebilirsin'),mark);
        await b.wait('clear_dialog',()=>true,mark);
        console.log('PASS existing 1.0.0 account logs in through new dialog after restart');
        await b.end();return;
    }
    assert.equal(componentText(b.form.title),'Kasaba’ya Hoş Geldin');
    assert.equal(b.form.title.bold,1); // NBT Boolean is encoded as a byte.
    assert(new Set(text(b.form.title).toLowerCase().match(/#[0-9a-f]{6}/g)).size>=3);
    const large=b.log.find(e=>e.name==='set_title_text');
    assert(large);assert.equal(componentText(simplify(large.p.text)),'Kasaba’ya Hoş Geldin');
    const page=b.log.find(e=>e.name==='show_dialog');
    assert(page.at-large.at>=1500,'Large welcome title must be visible before the form opens');
    console.log('PASS bold multicolor welcome and large title before page');
    assert.deepEqual(b.form.inputs.map(i=>i.key),['password','confirm']);
    assert.equal(b.form.actions[0].label.text,'Kayıt Ol');
    assert.equal(b.form.can_close_with_escape,0);
    assert.equal(b.form.after_action,'none','No native waiting/back screen may be opened');
    assert(!text(b.form.actions).includes('Geri dön'));
    assert(!text(b.form.actions).includes('Back'));
    assert(!b.log.some(e=>e.name==='open_window'));
    console.log('PASS direct registration page with two text fields, welcome header and no inventory');
    assert(b.log.some(e=>e.name==='boss_bar'&&e.p.title&&componentText(simplify(e.p.title)).includes('Kasaba’ya Hoş Geldin')));
    console.log('PASS upper welcome bar');
    assert(b.log.some(e=>e.name==='abilities'&&(e.p.flags&4)!==0));
    let flightAttempt=b.log.length;b.c.write('abilities',{flags:2});
    const noFly=await b.wait('abilities',p=>(p.flags&2)===0,flightAttempt);
    assert((noFly.flags&4)!==0);
    console.log('PASS actual flying cannot be toggled while unauthenticated');
    let start=b.log.length;
    b.c.write('chat_command',{command:'help'});
    await b.wait('system_chat',p=>text(p).includes('Önce giriş yap'),start);
    const origin=b.position;start=b.log.length;
    b.c.write('position',{x:origin.x+2,y:origin.y,z:origin.z,flags:{onGround:true,hasHorizontalCollision:false}});
    b.c.write('tick_end',{});
    const correction=await b.wait('position',()=>true,start);
    assert.equal(correction.x,origin.x);
    console.log('PASS movement and commands blocked before authentication');
    start=b.log.length;b.submit('short','short');
    await expectForm(b,start,'karakter olmalı');
    console.log('PASS short password rejected within page');
    const oldAction=b.action();
    start=b.log.length;b.submit(PASSWORD,'Different-Test-123');
    await expectForm(b,start,'eşleşmedi');
    assert.deepEqual(b.form.inputs.map(i=>i.key),['password','confirm']);
    console.log('PASS mismatched confirmation rejected within same form');
    start=b.log.length;b.send(oldAction,PASSWORD,PASSWORD);await delay(350);
    assert(!b.log.slice(start).some(e=>e.name==='system_chat'&&text(e.p).includes('keşfedebilirsin')));
    console.log('PASS replay of old form action cannot authenticate');
    start=b.log.length;b.submit(PASSWORD,PASSWORD);
    await b.wait('system_chat',p=>text(p).includes('keşfedebilirsin'),start);
    await b.wait('clear_dialog',()=>true,start);
    assert(!b.log.some(e=>e.name==='open_window'));
    const restored=await b.wait('abilities',p=>(p.flags&4)===0,start);
    assert.equal(restored.flags&2,0);
    console.log('PASS survival flight permission restored after authentication');
    console.log('PASS six-character password registers and closes page');
    // Persist a genuine mid-air survival position before reconnecting, so the
    // authentication freeze is exercised above the ground, not on a solid block.
    const ground=b.position;
    b.c.write('position',{x:ground.x,y:ground.y+6,z:ground.z,flags:{onGround:false,hasHorizontalCollision:false}});
    b.c.write('tick_end',{});await delay(200);
    await b.end();b=await connect(username);
    assert(b.position.y>ground.y+3,'Airborne regression did not start above the ground');
    const floatingStart=b.log.length;
    const hoverUntil=Date.now()+7000;
    while(Date.now()<hoverUntil) {
        const pos=b.position;
        b.c.write('position',{x:pos.x,y:pos.y,z:pos.z,flags:{onGround:false,hasHorizontalCollision:false}});
        b.c.write('tick_end',{});await delay(50);
        assert(!b.log.slice(floatingStart).some(e=>e.name==='kick_disconnect'),'Unauthenticated floating client was kicked');
    }
    console.log('PASS flight guard prevents kick during seven-second airborne auth wait');
    assert.deepEqual(b.form.inputs.map(i=>i.key),['password']);
    assert.equal(b.form.actions[0].label.text,'Giriş Yap');
    console.log('PASS reconnect opens login page directly');
    start=b.log.length;b.submit('Wrong-Test-123');
    await expectForm(b,start,'Şifre yanlış');
    console.log('PASS wrong password rejected with inline form error');
    start=b.log.length;b.submit(PASSWORD);
    await b.wait('system_chat',p=>text(p).includes('keşfedebilirsin'),start);
    await b.wait('clear_dialog',()=>true,start);
    start=b.log.length;b.c.write('chat_command',{command:'kasaba'});
    await b.wait('system_chat',p=>text(p).includes('Zaten giriş yaptın'),start);
    assert(!b.log.slice(start).some(e=>e.name==='show_dialog'));
    console.log('PASS correct password goes directly to gameplay with no extra form or back screen');
    await b.end();b=await connect(username);
    for(let attempt=1;attempt<=5;attempt++) {
        start=b.log.length;b.submit('Wrong-Test-123');
        if(attempt<5) await expectForm(b,start,'Şifre yanlış');
        else await b.wait('kick_disconnect',p=>text(p).includes('Çok fazla'),start);
    }
    console.log('PASS repeated wrong passwords trigger lockout');
    await b.end();
    const denied=mc.createClient({...options,username});
    await new Promise((resolve,reject)=>{
        const timer=setTimeout(()=>{denied.end();reject(new Error('Reconnect lockout missing'));},12000);
        denied.on('kick_disconnect',p=>{if(text(p).includes('Çok fazla')){clearTimeout(timer);resolve();}});
        denied.on('error',reject);
    });
    denied.end();console.log('PASS lockout survives reconnect');
    const exit=await connect('KX'+Date.now().toString(36));
    start=exit.log.length;exit.send(exit.action(1));
    await exit.wait('kick_disconnect',p=>text(p).includes('tekrar görüşmek'),start);
    await exit.end();
    console.log('PASS page exit button disconnects safely');
}
module.exports={connect,options,delay,text};
if(require.main===module)run().then(()=>process.exit(0)).catch(e=>{console.error('FAIL',e);process.exit(1);});
