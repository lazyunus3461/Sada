const assert=require('node:assert/strict');
const {connect,delay,text}=require('./smoke.cjs');
const username=process.env.KASABA_SPAWN_USER||('Spawn'+Date.now().toString(36));
async function probe(b,operation) {
    const mark=b.log.length;
    b.c.write('custom_payload',{channel:'kasaba_test:spawn',data:Buffer.from(operation)});
    await b.wait('system_chat',p=>text(p).includes('spawn-probe:'+operation),mark);
}
async function command(b,command,message) {
    const mark=b.log.length;b.c.write('chat_command',{command});
    await b.wait('system_chat',p=>text(p).includes(message),mark);
}
async function authenticate(b) {
    const mark=b.log.length;b.submit('Kasaba',b.form.inputs.length===2?'Kasaba':undefined);
    await b.wait('system_chat',p=>text(p).includes('keşfedebilirsin'),mark);
    await b.wait('clear_dialog',()=>true,mark);
}
function atSpawn(b) {
    assert.equal(b.position.x,21.5);assert.equal(b.position.y,-60);assert.equal(b.position.z,18.5);
    assert.equal(b.position.yaw,123);assert.equal(b.position.pitch,-12);
}
(async()=>{
    let b=await connect(username);
    if(process.env.KASABA_SPAWN_PERSIST==='1') {
        atSpawn(b);assert.equal(b.form.inputs.length,1);await authenticate(b);atSpawn(b);
        console.log('PASS configured spawn survives full server restart, including facing and post-login position');
        await probe(b,'CLEAR');await probe(b,'AWAY');await b.end();b=await connect(username);
        // Vanilla entity collisions can nudge a player just before logout.
        // Verify the previous area, far from the configured spawn, is restored.
        assert(Math.hypot(b.position.x-2.5,b.position.z-2.5)<1);await authenticate(b);
        console.log('PASS removing spawn restores ordinary last-location joins without losing accounts');
        await b.end();return;
    }
    await command(b,'kayityeri ayarla','Önce giriş yap');await authenticate(b);
    await command(b,'kayityeri ayarla','yetkin yok');
    console.log('PASS spawn administration requires authentication and administrative permission');
    await probe(b,'GRANT_ROLE');await probe(b,'MOVE');
    await command(b,'kayityeri ayarla','bulunduğun noktaya ayarlandı');
    await command(b,'kayityeri bilgi','X: 21.50');
    console.log('PASS Turkish set/info commands save the actual position and facing');
    await probe(b,'AWAY');await b.end();b=await connect(username);atSpawn(b);
    assert.equal(b.form.inputs.length,1);
    console.log('PASS reconnect begins at configured login point instead of last logout location');
    const mark=b.log.length;await probe(b,'LOCKED_TELEPORT');await delay(150);
    assert(!b.log.slice(mark).some(e=>e.name==='position'&&(e.p.x!==21.5||e.p.z!==18.5)));
    console.log('PASS allowing the internal login teleport does not allow other plugin teleports while locked');
    await authenticate(b);atSpawn(b);
    console.log('PASS successful login leaves the player at the configured point');
    await b.end();const fresh=await connect('New'+Date.now().toString(36));atSpawn(fresh);
    assert.equal(fresh.form.inputs.length,2);await authenticate(fresh);atSpawn(fresh);await fresh.end();
    console.log('PASS new account registration also begins and finishes at the same point');
    console.log('PERSIST_USER='+username);
})().then(()=>process.exit(0)).catch(e=>{console.error('FAIL',e);process.exit(1);});
