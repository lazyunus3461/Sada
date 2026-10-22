const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {connect, clients, delay} = require('../../../kasaba-arena/scripts/integration/regression.cjs');
const {probe, command, state, waitState} = require('./islands.cjs');

async function compassIsland(bot) {
  let from = bot.log.length;
  bot.c.write('held_item_slot', {slotId:8});
  bot.c.write('use_item', {hand:0, sequence:1, rotation:{x:0,y:0}});
  await bot.wait(e => e.name === 'show_dialog', from);
  let index = bot.form.actions.findIndex(a => JSON.stringify(a.label).includes('Oyunlar'));
  assert(index >= 0, 'Compass must open the main menu');
  await delay(150);
  from = bot.log.length;
  bot.click(index);
  await bot.wait(e => e.name === 'show_dialog', from);
  index = bot.form.actions.findIndex(a => JSON.stringify(a.label).includes('Adanı Yap'));
  assert(index >= 0, 'Adani Yap must be in the games page');
  await delay(150);
  bot.click(index);
}

async function run() {
  const saved = path.resolve(__dirname, '../../target/creative-mode-state.json');
  const name = fs.existsSync(saved) ? JSON.parse(fs.readFileSync(saved)).name
    : 'GM' + Date.now().toString(36).slice(-7);
  fs.writeFileSync(saved, JSON.stringify({name}));
  const bot = await connect(name);
  bot.c.write('custom_payload', {channel:'minecraft:register', data:Buffer.from('kasaba_test:islands')});
  await probe(bot, 'USER');
  let s = await waitState(bot, s => s.world === 'world' && s.temporary === 2);
  const outsideCycles = [s.daylightCycle, s.weatherCycle];
  if (!s.island) {
    await command(bot, 'ada al');
    await waitState(bot, s => s.world === 'ada');
    await command(bot, 'lobi');
    await waitState(bot, s => s.world === 'world' && s.temporary === 2);
  }
  await probe(bot, 'MODE|ADVENTURE');
  await probe(bot, 'ITEM|DIAMOND|7');
  await probe(bot, 'CONFLICT|ON');
  await compassIsland(bot);
  await delay(1000);
  s = await state(bot);
  if (process.env.EXPECT_BROKEN === '1') {
    assert(s.worldOverrides > 0 && s.creativeVetoes > 0, 'Conflict fixture did not run');
    assert(s.world !== 'ada' || s.mode !== 'CREATIVE', 'Old release unexpectedly works');
    console.log('REPRODUCED 1.3.0: creative cancellation / forced world mode prevents a creative island session');
    return;
  }
  assert.equal(s.world, 'ada');
  assert.equal(s.mode, 'CREATIVE');
  assert(s.worldOverrides >= 3, 'Immediate and delayed world overrides must execute');
  assert(s.creativeVetoes > 0, 'An external creative-mode veto must execute');
  assert(!s.items.includes('DIAMOND:7'));
  const island = s.island, total = s.total;
  console.log('PASS actual compass > Oyunlar > Adani Yap stays creative despite mode veto and immediate/next-tick/delayed overrides');
  assert.equal(s.daylightCycle, false);
  assert.equal(s.weatherCycle, false);
  assert.equal(s.time, 6000);
  await probe(bot, 'NIGHT_RAIN');
  await delay(600);
  s = await state(bot);
  assert.equal(s.time, 6000);
  assert.equal(s.storm, false);
  assert.equal(s.thunder, false);
  console.log('PASS island stays sunny at noon, daylight/weather cycles disabled and attempts to set night/storm are rejected');

  for (const mode of ['SURVIVAL', 'ADVENTURE', 'SPECTATOR']) {
    await probe(bot, 'MODE|' + mode);
    await delay(150);
    assert.equal((await state(bot)).mode, 'CREATIVE');
  }
  await command(bot, 'ada');
  assert.equal((await state(bot)).mode, 'CREATIVE');
  await probe(bot, 'ITEM|DIAMOND_BLOCK|5');
  await command(bot, 'lobi');
  s = await waitState(bot, s => s.world === 'world' && s.temporary === 2);
  assert.equal(s.mode, 'ADVENTURE');
  assert.deepEqual([s.daylightCycle, s.weatherCycle], outsideCycles);
  assert(s.items.includes('DIAMOND:7'));
  assert(!s.items.includes('DIAMOND_BLOCK:5'));
  await delay(800);
  assert.equal((await state(bot)).mode, 'ADVENTURE');
  console.log('PASS /lobi restores the actual previous mode and inventory; delayed tasks cannot turn the lobby creative');

  await probe(bot, 'MODE|SURVIVAL');
  assert.equal((await state(bot)).mode, 'SURVIVAL');
  await command(bot, 'oyunlar git adaniyap');
  s = await waitState(bot, s => s.world === 'ada' && s.mode === 'CREATIVE');
  assert(s.items.includes('DIAMOND_BLOCK:5'));
  await command(bot, 'lobi');
  s = await waitState(bot, s => s.world === 'world' && s.temporary === 2);
  assert.equal(s.mode, 'SURVIVAL');
  // Exit while entry callbacks are still pending.
  await command(bot, 'ada');
  await command(bot, 'lobi');
  await waitState(bot, s => s.world === 'world');
  await delay(1000);
  assert.equal((await state(bot)).mode, 'SURVIVAL');
  await command(bot, 'oyunlar git arena');
  await delay(800);
  s = await state(bot);
  assert.equal(s.mode, 'SURVIVAL');
  assert(!s.items.includes('DIAMOND_BLOCK:5'));
  await command(bot, 'lobi');
  await waitState(bot, s => s.world === 'world' && s.temporary === 2);
  s = await state(bot);
  assert.equal(s.island, island);
  assert.equal(s.total, total);
  await probe(bot, 'CONFLICT|OFF');
  console.log('PASS direct /ada and Oyunlar entry, fast exit, Arena isolation and unchanged single-island ownership');
}

run().catch(error => {console.error(error); process.exitCode = 1;})
  .finally(async () => {for (const client of clients) client.end(); await delay(500);});
