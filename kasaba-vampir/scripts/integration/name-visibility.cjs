const assert = require('node:assert/strict');
const {fixture} = require('./regression.cjs');
const {clients, delay} = require('../../../kasaba-arena/scripts/integration/regression.cjs');

async function run() {
  const suffix = Date.now().toString(36).slice(-5);
  const actor = await fixture('NV' + suffix + 'A'), viewer = await fixture('NV' + suffix + 'B');
  await actor.probe('ADMIN');
  await actor.command('vk oda kur');
  await viewer.command('vk oda katıl ' + (await actor.state()).room);
  await actor.command('vk deneme');
  for (let i = 0; i < 40; i++) {
    const x = -5.5, z = i % 2 ? -6.5 : 6.5;
    for (const distance of [9, 11]) {
      await actor.probe(`POSE|${x}|69|${z}`);
      await viewer.probe(`POSE|${x + distance}|69|${z}`);
      const visible = await viewer.json('VISIBILITY', 'visibility');
      assert.equal(visible.live[actor.name], distance === 9,
        JSON.stringify({iteration: i, distance, visible, actor: await actor.state(), viewer: await viewer.state()}));
      if (distance === 9 && i % 4 === 0) {
        await viewer.probe('RESET_NAMES');
        assert((await viewer.json('VISIBILITY', 'visibility')).live[actor.name], 'A visibility reset must recover on the next proximity update');
      }
    }
  }
  await actor.command('vk durdur');
  await actor.command('lobi');
  await viewer.command('lobi');
  console.log('PASS Player names survive 80 near/far checks, cross-chunk teleports and ten simulated visibility resets');
}

run().catch(e => { console.error(e); process.exitCode = 1; }).finally(async () => {
  for (const client of clients) client.end();
  await delay(500);
});
