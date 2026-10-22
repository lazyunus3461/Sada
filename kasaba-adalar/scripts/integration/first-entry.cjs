const assert = require('node:assert/strict');
const fs = require('node:fs');
const {connect, clients, delay} = require('../../../kasaba-arena/scripts/integration/regression.cjs');
const {probe, state, waitState} = require('./islands.cjs');

const names = ['survival', 'creative', 'adventure', 'spectator'];
function command(bot, text) {
  // Use the same unsigned command packet as an offline Minecraft client.
  bot.c.write('chat_command', {command:text});
}
function clientMode(bot) {
  let mode;
  for (const packet of bot.log) {
    if (['login', 'respawn'].includes(packet.name)) mode = packet.p.worldState.gamemode;
    if (packet.name === 'game_state_change' && packet.p.reason === 'change_game_mode') mode = names[packet.p.gameMode];
  }
  return mode;
}
function abilities(bot) {
  return bot.log.findLast(packet => packet.name === 'abilities').p.flags;
}
async function outside(bot) {
  command(bot, 'lobi');
  const s = await waitState(bot, s => s.world === 'world' && s.temporary === 2);
  await delay(150);
  assert.equal(s.mode, 'SURVIVAL');
  assert.equal(clientMode(bot), 'survival');
  assert.equal(abilities(bot) & 12, 0);
  assert(s.items.includes('DIAMOND:7'));
  assert(!s.items.includes('DIAMOND_BLOCK:5'));
}
async function firstCommand(bot, text) {
  const from = bot.log.length;
  command(bot, text); // Exactly ONE command, never repeated to repair entry.
  const s = await waitState(bot, s => s.world === 'ada');
  await delay(150);
  const first = bot.log.slice(from).find(packet => packet.name === 'respawn');
  assert(first, 'Must actually change worlds');
  if (process.env.EXPECT_OLD_ENTRY === '1') {
    assert.equal(first.p.worldState.gamemode, 'survival');
    console.log('REPRODUCED 1.3.1: first /ada world-entry packet is SURVIVAL although server state later becomes CREATIVE');
    return;
  }
  assert.equal(first.p.worldState.gamemode, 'creative', 'First world-entry packet must already be creative: /' + text);
  assert.equal(s.mode, 'CREATIVE');
  assert.equal(clientMode(bot), 'creative');
  assert.equal(abilities(bot) & 12, 12, 'Client must have flight AND creative inventory');
  assert.equal((await state(bot)).allowFlight, true);
  console.log('PASS one /' + text + ': first entry packet, final client/server mode, creative inventory and flight');
}

async function run() {
  const old = process.env.EXPECT_OLD_ENTRY === '1';
  const name = old ? JSON.parse(fs.readFileSync(require('node:path').resolve(__dirname, '../../target/creative-mode-state.json'))).name
    : 'FI' + Date.now().toString(36).slice(-7);
  const bot = await connect(name);
  bot.c.write('custom_payload', {channel:'minecraft:register', data:Buffer.from('kasaba_test:islands')});
  await probe(bot, 'USER');
  await waitState(bot, s => s.world === 'world' && s.temporary === 2);
  await probe(bot, 'MODE|SURVIVAL');
  await probe(bot, 'ITEM|DIAMOND|7');
  if (old) {
    await firstCommand(bot, 'ada');
    await outside(bot);
    return;
  }
  await firstCommand(bot, 'ada al');
  const initial = await state(bot);
  await probe(bot, 'ITEM|DIAMOND_BLOCK|5');
  await outside(bot);
  for (const text of ['ada', 'ada git', 'oyunlar git adaniyap']) {
    await firstCommand(bot, text);
    assert((await state(bot)).items.includes('DIAMOND_BLOCK:5'));
    await outside(bot);
  }
  await probe(bot, 'BLOCK_ISLAND|ON');
  command(bot, 'ada');
  await delay(750);
  let s = await state(bot);
  assert.equal(s.world, 'world');
  assert.equal(s.mode, 'SURVIVAL');
  assert.equal(clientMode(bot), 'survival');
  assert(s.items.includes('DIAMOND:7'));
  assert(!s.items.includes('DIAMOND_BLOCK:5'));
  console.log('PASS cancelled first teleport restores the outside mode/inventory and leaves no creative session in the lobby');
  await probe(bot, 'BLOCK_ISLAND|OFF');
  await probe(bot, 'FLIGHT_RESET|ON');
  await firstCommand(bot, 'ada');
  await outside(bot);
  await probe(bot, 'FLIGHT_RESET|OFF');
  command(bot, 'ada');
  await waitState(bot, s => s.world === 'ada');
  command(bot, 'lobi');
  await waitState(bot, s => s.world === 'world');
  await delay(650);
  s = await state(bot);
  assert.equal(s.mode, 'SURVIVAL');
  assert.equal(clientMode(bot), 'survival');
  assert.equal(s.allowFlight, false);
  assert.equal(s.island, initial.island);
  assert.equal(s.total, initial.total);
  console.log('PASS teleport flight reset is repaired; quick exit cannot restore creative outside; ownership is unchanged');
}
run().catch(error => {console.error(error); process.exitCode = 1;})
  .finally(async () => {for (const client of clients) client.end(); await delay(500);});
