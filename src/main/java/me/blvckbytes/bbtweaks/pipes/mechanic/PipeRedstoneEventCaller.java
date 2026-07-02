// $Id$
/*
 * CraftBook Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package me.blvckbytes.bbtweaks.pipes.mechanic;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Powerable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;

import java.util.HashSet;
import java.util.Set;

// Cruft ported over from CraftBook3 - formerly known as MechanicListenerAdapter.

public class PipeRedstoneEventCaller implements Listener {

    private static final boolean ALLOW_INDIRECT_REDSTONE = false;
    private static final Set<Material> isRedstoneBlock = new HashSet<>();

    static {
        isRedstoneBlock.add(Material.POWERED_RAIL);
        isRedstoneBlock.add(Material.DETECTOR_RAIL);
        isRedstoneBlock.add(Material.STICKY_PISTON);
        isRedstoneBlock.add(Material.PISTON);
        isRedstoneBlock.add(Material.LEVER);
        isRedstoneBlock.add(Material.STONE_PRESSURE_PLATE);
        isRedstoneBlock.addAll(Tag.WOODEN_PRESSURE_PLATES.getValues());
        isRedstoneBlock.add(Material.REDSTONE_TORCH);
        isRedstoneBlock.add(Material.REDSTONE_WALL_TORCH);
        isRedstoneBlock.add(Material.REDSTONE_WIRE);
        isRedstoneBlock.addAll(Tag.DOORS.getValues());
        isRedstoneBlock.add(Material.TNT);
        isRedstoneBlock.add(Material.DISPENSER);
        isRedstoneBlock.add(Material.NOTE_BLOCK);
        isRedstoneBlock.add(Material.REPEATER);
        isRedstoneBlock.add(Material.TRIPWIRE_HOOK);
        isRedstoneBlock.add(Material.COMMAND_BLOCK);
        isRedstoneBlock.addAll(Tag.BUTTONS.getValues());
        isRedstoneBlock.add(Material.TRAPPED_CHEST);
        isRedstoneBlock.add(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
        isRedstoneBlock.add(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        isRedstoneBlock.add(Material.COMPARATOR);
        isRedstoneBlock.add(Material.REDSTONE_BLOCK);
        isRedstoneBlock.add(Material.HOPPER);
        isRedstoneBlock.add(Material.ACTIVATOR_RAIL);
        isRedstoneBlock.add(Material.DROPPER);
        isRedstoneBlock.add(Material.DAYLIGHT_DETECTOR);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isRedstoneBlock(Material id) {
        return isRedstoneBlock.contains(id);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        checkBlockChange(event.getBlock(), false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        checkBlockChange(event.getBlock(), true);
    }

    private static void checkBlockChange(Block block, boolean build) {
        switch(block.getType()) {
            case REDSTONE_TORCH:
            case REDSTONE_WALL_TORCH:
            case REDSTONE_BLOCK:
                handleRedstoneForBlock(block, build ? 0 : 15, build ? 15 : 0);
                break;
            case ACACIA_BUTTON:
            case BIRCH_BUTTON:
            case DARK_OAK_BUTTON:
            case JUNGLE_BUTTON:
            case OAK_BUTTON:
            case SPRUCE_BUTTON:
            case STONE_BUTTON:
            case LEVER:
            case DETECTOR_RAIL:
            case STONE_PRESSURE_PLATE:
            case ACACIA_PRESSURE_PLATE:
            case BIRCH_PRESSURE_PLATE:
            case DARK_OAK_PRESSURE_PLATE:
            case JUNGLE_PRESSURE_PLATE:
            case OAK_PRESSURE_PLATE:
            case SPRUCE_PRESSURE_PLATE:
            case COMPARATOR:
            case REPEATER:
                if (block.getBlockData() instanceof Powerable powerable) {
                    if (powerable.isPowered())
                        handleRedstoneForBlock(block, build ? 0 : 15, build ? 15 : 0);
                }
                break;
            case HEAVY_WEIGHTED_PRESSURE_PLATE:
            case LIGHT_WEIGHTED_PRESSURE_PLATE:
            case REDSTONE_WIRE:
                if (block.getBlockData() instanceof AnaloguePowerable analoguePowerable) {
                    if (analoguePowerable.getPower() > 0)
                        handleRedstoneForBlock(block, build ? 0 : analoguePowerable.getPower(), build ? analoguePowerable.getPower() : 0);
                }
                break;
            default:
                break;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        handleRedstoneForBlock(event.getBlock(), event.getOldCurrent(), event.getNewCurrent());
    }

    private static void handleRedstoneForBlock(Block block, int oldLevel, int newLevel) {

        World world = block.getWorld();

        // Give the method a BlockWorldVector instead of a Block
        boolean wasOn = oldLevel >= 1;
        boolean isOn = newLevel >= 1;
        boolean wasChange = wasOn != isOn;

        // For efficiency reasons, we're only going to consider changes between
        // off and on state, and ignore simple current changes (i.e. 15->13)
        if (!wasChange) return;

        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        // When this hook has been called, the level in the world has not
        // yet been updated, so we're going to do this very ugly thing of
        // faking the value with the new one whenever the data value of this
        // block is requested -- it is quite ugly
        switch(block.getType()) {
            case REDSTONE_WIRE:
                if (ALLOW_INDIRECT_REDSTONE) {
                    // power all blocks around the redstone wire on the same y level
                    // north/south
                    handleDirectWireInput(x - 1, y, z, block);
                    handleDirectWireInput(x + 1, y, z, block);
                    // east/west
                    handleDirectWireInput(x, y, z - 1, block);
                    handleDirectWireInput(x, y, z + 1, block);

                    // Can be triggered from below
                    handleDirectWireInput(x, y + 1, z, block);

                    // Can be triggered from above (Eg, glass->glowstone like redstone lamps)
                    handleDirectWireInput(x, y - 1, z, block);
                } else {

                    Material above = world.getBlockAt(x, y + 1, z).getType();

                    Material westSide = world.getBlockAt(x, y, z + 1).getType();
                    Material westSideAbove = world.getBlockAt(x, y + 1, z + 1).getType();
                    Material westSideBelow = world.getBlockAt(x, y - 1, z + 1).getType();
                    Material eastSide = world.getBlockAt(x, y, z - 1).getType();
                    Material eastSideAbove = world.getBlockAt(x, y + 1, z - 1).getType();
                    Material eastSideBelow = world.getBlockAt(x, y - 1, z - 1).getType();

                    Material northSide = world.getBlockAt(x - 1, y, z).getType();
                    Material northSideAbove = world.getBlockAt(x - 1, y + 1, z).getType();
                    Material northSideBelow = world.getBlockAt(x - 1, y - 1, z).getType();
                    Material southSide = world.getBlockAt(x + 1, y, z).getType();
                    Material southSideAbove = world.getBlockAt(x + 1, y + 1, z).getType();
                    Material southSideBelow = world.getBlockAt(x + 1, y - 1, z).getType();

                    // Make sure that the wire points to only this block
                    if (!isRedstoneBlock(westSide) && !isRedstoneBlock(eastSide)
                            && (!isRedstoneBlock(westSideAbove) || westSide == Material.AIR || above != Material.AIR)
                            && (!isRedstoneBlock(eastSideAbove) || eastSide == Material.AIR || above != Material.AIR)
                            && (!isRedstoneBlock(westSideBelow) || westSide != Material.AIR)
                            && (!isRedstoneBlock(eastSideBelow) || eastSide != Material.AIR)) {
                        // Possible blocks north / south
                        handleDirectWireInput(x - 1, y, z, block);
                        handleDirectWireInput(x + 1, y, z, block);
                        handleDirectWireInput(x - 1, y - 1, z, block);
                        handleDirectWireInput(x + 1, y - 1, z, block);
                    }

                    if (!isRedstoneBlock(northSide) && !isRedstoneBlock(southSide)
                            && (!isRedstoneBlock(northSideAbove) || northSide == Material.AIR || above != Material.AIR)
                            && (!isRedstoneBlock(southSideAbove) || southSide == Material.AIR || above != Material.AIR)
                            && (!isRedstoneBlock(northSideBelow) || northSide != Material.AIR)
                            && (!isRedstoneBlock(southSideBelow) || southSide != Material.AIR)) {
                        // Possible blocks west / east
                        handleDirectWireInput(x, y, z - 1, block);
                        handleDirectWireInput(x, y, z + 1, block);
                        handleDirectWireInput(x, y - 1, z - 1, block);
                        handleDirectWireInput(x, y - 1, z + 1, block);
                    }

                    // Can be triggered from below
                    handleDirectWireInput(x, y + 1, z, block);

                    // Can be triggered from above
                    handleDirectWireInput(x, y - 1, z, block);
                }
                return;
            case REPEATER:
            case COMPARATOR:
                Directional diode = (Directional) block.getBlockData();
                BlockFace f = diode.getFacing();
                handleDirectWireInput(x + f.getModX(), y, z + f.getModZ(), block);
                if(block.getRelative(f).getType() != Material.AIR) {
                    handleDirectWireInput(x + f.getModX(), y - 1, z + f.getModZ(), block);
                    handleDirectWireInput(x + f.getModX(), y + 1, z + f.getModZ(), block);
                    handleDirectWireInput(x + f.getModX() + 1, y - 1, z + f.getModZ(), block);
                    handleDirectWireInput(x + f.getModX() - 1, y - 1, z + f.getModZ(), block);
                    handleDirectWireInput(x + f.getModX() + 1, y - 1, z + f.getModZ() + 1, block);
                    handleDirectWireInput(x + f.getModX() - 1, y - 1, z + f.getModZ() - 1, block);
                }
                return;
            case ACACIA_BUTTON:
            case BIRCH_BUTTON:
            case DARK_OAK_BUTTON:
            case JUNGLE_BUTTON:
            case OAK_BUTTON:
            case SPRUCE_BUTTON:
            case STONE_BUTTON:
            case LEVER:
                if (block.getBlockData() instanceof Directional directional) {
                    var face = directional.getFacing().getOppositeFace();
                    handleDirectWireInput(x + face.getModX() * 2, y + face.getModY() * 2, z + face.getModZ() * 2, block);
                }
                break;
            case POWERED_RAIL:
            case ACTIVATOR_RAIL:
                return;
        }

        // For redstone wires and repeaters, the code already exited this method
        // Non-wire blocks proceed

        handleDirectWireInput(x - 1, y, z, block);
        handleDirectWireInput(x + 1, y, z, block);
        handleDirectWireInput(x - 1, y - 1, z, block);
        handleDirectWireInput(x + 1, y - 1, z, block);
        handleDirectWireInput(x, y, z - 1, block);
        handleDirectWireInput(x, y, z + 1, block);
        handleDirectWireInput(x, y - 1, z - 1, block);
        handleDirectWireInput(x, y - 1, z + 1, block);

        // Can be triggered from below
        handleDirectWireInput(x, y + 1, z, block);

        // Can be triggered from above
        handleDirectWireInput(x, y - 1, z, block);
    }

    private static void handleDirectWireInput(int x, int y, int z, Block sourceBlock) {
        var block = sourceBlock.getWorld().getBlockAt(x, y, z);

        if (sourceBlock.getLocation().equals(block.getLocation()))
            return;

        var event = new PipeRedstoneEvent(block);

        Bukkit.getServer().getPluginManager().callEvent(event);
    }
}