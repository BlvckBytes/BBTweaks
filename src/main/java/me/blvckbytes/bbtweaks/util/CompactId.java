package me.blvckbytes.bbtweaks.util;

import org.bukkit.block.Block;

public class CompactId {

    public static long computeWorldlessBlockId(int x, int y, int z) {
        // y in [-64;320], adding 64 will result in [0;384], 9 bits
        // x/z in [-30M;30M], adding 30M will result in [0;60M], 26 bits

        // <3b unused><26b z><26b x><9b y>

        return (
          // 2^9 - 1 = 0x1FF
          // 2^26 - 1 = 0x3FFFFFF
          // 2^3 - 1 = 0x7
          ((y + 64) & 0x1FF)
            | (((x + 30_000_000L) & 0x3FFFFFF) << 9)
            | (((z + 30_000_000L) & 0x3FFFFFF) << (9 + 26))
        );
    }

    public static long computeWorldlessBlockId(Block block) {
        return computeWorldlessBlockId(block.getX(), block.getY(), block.getZ());
    }
}
