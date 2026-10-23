const assert = require('node:assert/strict');
const fs = require('node:fs');
const crypto = require('node:crypto');
const {fixture, speech, pas, finishPersonal, finishWork} = require('./regression.cjs');
const {clients, delay} = require('../../../kasaba-arena/scripts/integration/regression.cjs');
const checks = [];
const pass = text => { checks.push(text); console.log('PASS ' + text); };

const completeTask = finishPersonal;

async function run() {
  const suffix = Date.now().toString(36).slice(-5), bots = [];
  for (const letter of 'ABCDEF') bots.push(await fixture('DH' + suffix + letter));
  const owner = bots[0];
  await owner.probe('ADMIN');
  await owner.command('vk oda kur');
  const room = (await owner.state()).room;
  for (const bot of bots.slice(1)) await bot.command('vk oda katıl ' + room);
  await owner.command('vk başlat');
  const states = await Promise.all(bots.map(b => b.state()));
  const role = name => bots[states.findIndex(s => s.role === name)];
  const vamp = role('VAMPIRE'), doctor = role('DOCTOR'), seer = role('SEER');
  const villagers = bots.filter((b, i) => states[i].role === 'VILLAGER');
  const world = states[0].world;
  assert.equal(states[0].taskGoal, 15);
  assert((await vamp.state()).roleTitle.includes('Av hazır'));

  const mark = doctor.log.length;
  await completeTask(vamp);
  const fake = await doctor.state();
  assert.equal(fake.taskDone, 0);
  assert.equal(fake.fakeTaskDone, 1);
  assert(Math.abs(fake.barProgress - 1 / 15) < .00001);
  assert(doctor.log.slice(mark).some(e => e.name === 'boss_bar' && e.p.action === 2 && Math.abs(e.p.health - 1 / 15) < .00001));
  await completeTask(doctor);
  const real = await doctor.state();
  assert.equal(real.taskDone, 1);
  assert.equal(real.fakeTaskDone, 1);
  assert(Math.abs(real.barProgress - 2 / 15) < .00001);
  for (const bot of bots) { const state=await bot.state(); assert(Math.abs(state.barProgress-real.barProgress)<.00001); assert.equal(state.barTitle.match(/Görev %\d+/)[0],real.barTitle.match(/Görev %\d+/)[0]); }
  assert(!doctor.log.some(e => doctor.msg(e).includes('Takım ilerlemesi:')));
  pass('A vampire fake task and a real task give identical public boss-bar increments; the hidden real count is not disclosed');

  await doctor.command('vk görev 2');
  await doctor.probe('ATLAS');
  const hashes = new Set();
  for (let frame = 0; frame < 6 && hashes.size < 2; frame++) {
    await doctor.probe('PAINT|' + frame);
    const png = fs.readFileSync('/workspace/scratch/kasaba-vampir-110-validation/atlas-' + doctor.name + '-' + frame + '.png');
    hashes.add(crypto.createHash('sha256').update(png).digest('hex'));
    await delay(350);
  }
  assert.equal(hashes.size, 2);
  pass('Stationary held-map frames pulse around pending tasks and the selected destination without showing other players');

  await vamp.probe('POSE|3.5|69|.5');
  await villagers[0].probe('POSE|5.5|69|.5');
  await villagers[1].probe('POSE|5.5|69|1.5');
  await vamp.probe('HIT|' + villagers[0].name);
  assert.equal((await villagers[0].state()).alive, false);
  let hunt = await vamp.state();
  assert(hunt.huntRemaining > 39000 && hunt.huntRemaining <= 40000);
  await vamp.probe('HIT|' + villagers[1].name);
  assert((await villagers[1].state()).alive);
  await delay(1100);
  hunt = await vamp.state();
  assert(hunt.roleTitle.includes('Av: '));
  assert.deepEqual(hunt.roleViewers, [vamp.name]);
  pass('A successful hunt starts a private forty-second countdown; immediate extra attacks are denied');

  // Use a real wall-clock wait: the production cooldown is not shortened by the fixture.
  await delay(Math.max(0, hunt.huntRemaining - 550));
  await vamp.probe('HIT|' + villagers[1].name);
  assert((await villagers[1].state()).alive, 'A hunt before forty seconds must be blocked');
  hunt = await vamp.state();
  assert(hunt.huntRemaining > 0 && hunt.huntRemaining < 1000);
  await delay(hunt.huntRemaining + 150);
  await vamp.probe('HIT|' + villagers[1].name);
  assert.equal((await villagers[1].state()).alive, false);
  assert.equal((await vamp.state()).aliveCount, 4);
  assert.equal((await vamp.state()).phase, 'DAY');
  assert((await vamp.state()).huntRemaining > 39000);
  pass('An attack just before forty seconds is blocked; after forty seconds a second kill succeeds during the same day');

  await doctor.probe('EXPIRE');
  await speech(bots);
  assert.equal((await doctor.state()).phase, 'VOTE');
  assert(Math.abs((await doctor.state()).barProgress - 2 / 15) < .00001);
  await pas(bots);
  assert.equal((await doctor.state()).phase, 'ROLE');
  assert(Math.abs((await doctor.state()).barProgress - 2 / 15) < .00001);
  await doctor.probe('EXPIRE'); // No vampire target was selected: no automatic kill.
  assert((await doctor.state()).firstNightDone);
  await vamp.probe('POSE|3.5|69|.5');
  await seer.probe('POSE|5.5|69|.5');
  assert((await vamp.state()).huntRemaining > 0);
  await vamp.probe('HIT|' + seer.name);
  assert((await seer.state()).alive);
  pass('The visible fake contribution survives voting/night; meetings and a new day do not reset the hunt cooldown');

  const tasks = (await vamp.state()).tasks;
  const station = Array.from({length: 17}, (_, i) => i).find(i => !tasks.some(t => !t.completed && t.station === i));
  await vamp.probe('STATION|' + station);
  await delay(175);
  await finishWork(vamp, true);
  hunt = await vamp.state();
  assert.equal(hunt.taskDone, 1);
  assert.equal(hunt.fakeTaskDone, 2);
  assert(Math.abs(hunt.barProgress - 3 / 15) < .00001);
  const repeatMark = vamp.log.length;
  await vamp.probe('STATION|' + station);
  await delay(175);
  assert(!vamp.log.slice(repeatMark).some(e => e.name === 'show_dialog'));
  assert.equal((await vamp.state()).fakeTaskDone, 2);
  pass('Vampires can also do fake common maintenance after the first night, with the same thirty-second repeat limit');

  // Seed a late-game boundary, then use real native task callbacks for every subsequent increment.
  await doctor.probe('COUNTERS|13|2');
  await completeTask(vamp);
  hunt = await vamp.state();
  assert.equal(hunt.taskDone, 13);
  assert.equal(hunt.fakeTaskDone, 3);
  assert(Math.abs(hunt.barProgress - .99) < .00001);
  assert(hunt.barTitle.includes('%99'));
  assert.equal(hunt.phase, 'DAY');
  assert(fs.existsSync('/workspace/kasaba-test-server/' + world));
  await completeTask(doctor);
  assert.equal((await doctor.state()).taskDone, 14);
  assert(Math.abs((await doctor.state()).barProgress - .99) < .00001);
  await completeTask(doctor, true);
  assert(!(await doctor.state()).room);
  assert(!fs.existsSync('/workspace/kasaba-test-server/' + world));
  assert(doctor.log.some(e => doctor.msg(e).includes('Köylüler bütün görevleri tamamladı')));
  pass('Fake work caps the visible bar at 99% without ending the game; only the final genuine task awards the villager victory');
  for (const bot of bots) await bot.command('lobi');
  console.log(JSON.stringify({version: '1.1.0', realClients: 6, checks}, null, 2));
}

run().catch(e => { console.error(e); process.exitCode = 1; }).finally(async () => {
  for (const client of clients) client.end();
  await delay(500);
});
