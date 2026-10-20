const assert=require('node:assert/strict');
const {connect,delay,text}=require('./smoke.cjs');
async function probe(b,operation) {
    const mark=b.log.length;
    b.c.write('custom_payload',{channel:'kasaba_test:flight',data:Buffer.from(operation,'ascii')});
    await b.wait('system_chat',p=>text(p).includes('flight-probe-survived:'+operation),mark);
}
(async()=>{
    let b=await connect('KR'+Date.now().toString(36));
    await probe(b,'CLEAR_FLOAT');
    console.log('PASS loopback-only native flight probe is available');
    await probe(b,'STALE_FLOAT');
    await delay(450);
    assert(!b.log.some(e=>e.name==='kick_disconnect'));
    console.log('PASS exhausted native floating counter cannot kick during authentication');
    await probe(b,'FLYING_VEHICLE');
    console.log('PASS vehicle flying cause is suppressed only during authentication');
    let mark=b.log.length;b.submit('Kasaba','Kasaba');
    await b.wait('clear_dialog',()=>true,mark);
    await b.wait('system_chat',p=>text(p).includes('keşfedebilirsin'),mark);
    console.log('PASS authentication completes with a stale floating counter');
    await probe(b,'STALE_FLOAT');
    console.log('PASS short post-login grace protects dialog-to-game transition');
    await probe(b,'CLEAR_FLOAT');
    await delay(5600);
    mark=b.log.length;
    b.c.write('custom_payload',{channel:'kasaba_test:flight',data:Buffer.from('FLYING_PLAYER','ascii')});
    await b.wait('kick_disconnect',p=>text(p).includes('flight-probe:FLYING_PLAYER'),mark);
    await b.end();
    console.log('PASS genuine flying kick remains active after the bounded grace period');
    b=await connect('KP'+Date.now().toString(36));
    mark=b.log.length;
    b.c.write('custom_payload',{channel:'kasaba_test:flight',data:Buffer.from('PLUGIN','ascii')});
    await b.wait('kick_disconnect',p=>text(p).includes('flight-probe:PLUGIN'),mark);
    await b.end();
    console.log('PASS non-flying plugin kicks are not exempted during authentication');
})().then(()=>process.exit(0)).catch(e=>{console.error('FAIL',e);process.exit(1);});
