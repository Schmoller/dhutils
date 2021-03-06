package me.desht.dhutils.nms.v1_8_R2;

import me.desht.dhutils.nms.api.NMSAbstraction;
import net.minecraft.server.v1_8_R2.*;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;

public class NMSHandler implements NMSAbstraction {

	@Override
    public boolean setBlockFast(World world, int x, int y, int z, int blockId, byte data) {
        net.minecraft.server.v1_8_R2.World w = ((CraftWorld) world).getHandle();
        Chunk chunk = w.getChunkAt(x >> 4, z >> 4);
        chunk.a(new BlockPosition(x & 0xF, y, z & 0xF), Block.getById(blockId).fromLegacyData(data));
        return true;
    }

	@Override
	public void forceBlockLightLevel(World world, int x, int y, int z, int level) {
		net.minecraft.server.v1_8_R2.World w = ((CraftWorld) world).getHandle();
		w.a(EnumSkyBlock.BLOCK, new BlockPosition(x, y, z), level);
	}

	@Override
	public int getBlockLightEmission(int blockId) {
		return Block.getById(blockId).r();
	}

	@Override
	public int getBlockLightBlocking(int blockId) {
		return Block.getById(blockId).p();
	}

	@Override
	public void queueChunkForUpdate(Player player, int cx, int cz) {
		((CraftPlayer) player).getHandle().chunkCoordIntPairQueue.add(new ChunkCoordIntPair(cx, cz));
	}

	@Override
	public Vector[] getBlockHitbox(org.bukkit.block.Block block) {
		net.minecraft.server.v1_8_R2.World w = ((CraftWorld)block.getWorld()).getHandle();
		IBlockData b = w.getType(new BlockPosition(block.getX(), block.getY(), block.getZ()));
		b.getBlock().updateShape(w, new BlockPosition(block.getX(), block.getY(), block.getZ()));
		return new Vector[] {
				new Vector(block.getX() + b.getBlock().B(), block.getY() + b.getBlock().D(), block.getZ() + b.getBlock().F()),
				new Vector(block.getX() + b.getBlock().C(), block.getY() + b.getBlock().E(), block.getZ() + b.getBlock().G())
		};
	}

    @Override
    public void recalculateBlockLighting(World world, int x, int y, int z) {
        // Don't consider blocks that are completely surrounded by other non-transparent blocks
        if (!canAffectLighting(world, x, y, z)) {
            return;
        }

        int i = x & 0x0F;
        int j = y & 0xFF;
        int k = z & 0x0F;
        
        BlockPosition pos = new BlockPosition(i, j, k);
        CraftChunk craftChunk = (CraftChunk)world.getChunkAt(x >> 4, z >> 4);
        Chunk nmsChunk = craftChunk.getHandle();

        int i1 = k << 4 | i;
        int maxY = nmsChunk.heightMap[i1];

        Block block = nmsChunk.getType(new BlockPosition(i, j, k));
        int j2 = block.p();

        if (j2 > 0) {
            if (j >= maxY) {
                invokeNmsD(nmsChunk, i, j + 1, k);
            }
        } else if (j == maxY - 1) {
            invokeNmsD(nmsChunk,i, j, k);
        }

        if (nmsChunk.getBrightness(EnumSkyBlock.SKY, pos) > 0 || nmsChunk.getBrightness(EnumSkyBlock.BLOCK, pos) > 0) {
            invokeNmsD(nmsChunk, i, k);
        }

        net.minecraft.server.v1_8_R2.World w = ((CraftWorld) world).getHandle();
        w.c(EnumSkyBlock.BLOCK, pos);
    }

    private Method d;
    private void invokeNmsD(Chunk nmsChunk, int i, int j, int k) {
        try {
            if (d == null) {
                Class[] classes = {int.class, int.class, int.class};
                d = Chunk.class.getDeclaredMethod("d", classes);
                d.setAccessible(true);
            }
            d.invoke(nmsChunk, i, j, k);
        } catch (Exception e) {
            System.out.println("Reflection exception: " + e);
        }
    }

    private Method e;
    private void invokeNmsD(Chunk nmsChunk, int i, int j) {
        try {
            if (e == null) {
                Class[] classes = {int.class, int.class};
                e = Chunk.class.getDeclaredMethod("d", classes);
                e.setAccessible(true);
            }
            e.invoke(nmsChunk, i, j);
        } catch (Exception e) {
            System.out.println("Reflection exception: " + e);
        }
    }

    private boolean canAffectLighting(World world, int x, int y, int z) {
        org.bukkit.block.Block base  = world.getBlockAt(x, y, z);
        org.bukkit.block.Block east  = base.getRelative(BlockFace.EAST);
        org.bukkit.block.Block west  = base.getRelative(BlockFace.WEST);
        org.bukkit.block.Block up    = base.getRelative(BlockFace.UP);
        org.bukkit.block.Block down  = base.getRelative(BlockFace.DOWN);
        org.bukkit.block.Block south = base.getRelative(BlockFace.SOUTH);
        org.bukkit.block.Block north = base.getRelative(BlockFace.NORTH);

        return east.getType().isTransparent() ||
                west.getType().isTransparent() ||
                up.getType().isTransparent() ||
                down.getType().isTransparent() ||
                south.getType().isTransparent() ||
                north.getType().isTransparent();
    }
}
