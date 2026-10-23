const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {fixture,clients,delay,waitState}=require('./helpers.cjs');
const req=require('node:module').createRequire(path.resolve(__dirname,'../../../scripts/integration/package.json'));
const data=req('minecraft-data')('1.21.10'),Item=req('prismarine-item')('1.21.10');
async function run(){
 const root=path.resolve(__dirname,'../../target'),saved=JSON.parse(fs.readFileSync(root+'/restart-state.json'));
 const b=await fixture(saved.names[2]);await b.command('ada');await waitState(b,s=>s.world==='ada'&&s.mode==='CREATIVE');await delay(1100);
 const eggs=Object.keys(data.itemsByName).filter(n=>n.endsWith('_spawn_egg'));
 const redstone=['redstone','redstone_block','redstone_torch','redstone_lamp','repeater','comparator','target','lever','oak_button','stone_button','oak_pressure_plate','heavy_weighted_pressure_plate','sculk_sensor','calibrated_sculk_sensor','sculk_shrieker','tripwire_hook','string','daylight_detector','lightning_rod','waxed_copper_bulb','piston','sticky_piston','slime_block','honey_block','dispenser','dropper','crafter','hopper','trapped_chest','note_block','jukebox','rail','powered_rail','detector_rail','activator_rail','minecart','hopper_minecart','oak_chest_boat','bamboo_chest_raft','iron_door','iron_trapdoor','tnt'];
 for(const name of [...redstone,...eggs]){
  assert(data.itemsByName[name],name);
  b.c.write('set_creative_slot',{slot:36,item:Item.toNotch(new Item(data.itemsByName[name].id,1))});
  await delay(75);assert(!(await b.state()).items[0].startsWith(name.toUpperCase()+':'),'Creative inventory accepted '+name);
 }
 for(const name of ['stone','white_wool','chest','oak_door']){
  b.c.write('set_creative_slot',{slot:36,item:Item.toNotch(new Item(data.itemsByName[name].id,1))});
  await delay(100);assert.equal((await b.state()).items[0],name.toUpperCase()+':1');
 }
 for(const material of ['REPEATER','ZOMBIE_SPAWN_EGG','CRAFTING_TABLE']){
  const mark=await b.probe('SAFETY|'+material),denied=b.log.slice(mark).some(e=>b.msg(e)==='network-probe:use:DENY');
  if(material!=='CRAFTING_TABLE')assert(denied,material);
 }
 await b.probe('ITEM|0|ZOMBIE_SPAWN_EGG|1');await b.probe('ITEM|1|REPEATER|1');await delay(1200);
 let state=await b.state();assert.equal(state.items[0],'AIR');assert.equal(state.items[1],'AIR');
 await b.command('lobi');await b.probe('ITEM|0|REPEATER|1');await delay(1200);assert.equal((await b.state()).items[0],'REPEATER:1');
 await b.probe('ITEM|0|AIR|1');
 const result=JSON.parse(fs.readFileSync(root+'/live-results.json'));result.islandCreativeRestrictions={actualCreativePackets:true,spawnEggs:eggs.length,redstoneItems:redstone.length,ordinaryBuildingAllowed:true,outsideIslandUnaffected:true};fs.writeFileSync(root+'/live-results.json',JSON.stringify(result,null,2)+'\n');
 console.log('PASS real creative inventory packets reject '+eggs.length+' spawn eggs and '+redstone.length+' redstone/mechanism items; ordinary building works, existing forbidden items are removed only inside islands');
}
run().catch(e=>{console.error(e);process.exitCode=1;}).finally(async()=>{for(const c of clients)c.end();await delay(500);});
