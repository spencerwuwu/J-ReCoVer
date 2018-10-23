// https://searchcode.com/api/result/92688330/

package net.canarymod.api.factory;

import net.canarymod.api.DataWatcher;
import net.canarymod.api.chat.CanaryChatComponent;
import net.canarymod.api.chat.ChatComponent;
import net.canarymod.api.entity.CanaryEntity;
import net.canarymod.api.entity.CanaryXPOrb;
import net.canarymod.api.entity.Entity;
import net.canarymod.api.entity.XPOrb;
import net.canarymod.api.entity.hanging.CanaryPainting;
import net.canarymod.api.entity.hanging.Painting;
import net.canarymod.api.entity.living.CanaryLivingBase;
import net.canarymod.api.entity.living.LivingBase;
import net.canarymod.api.entity.living.humanoid.CanaryHuman;
import net.canarymod.api.entity.living.humanoid.CanaryHumanCapabilities;
import net.canarymod.api.entity.living.humanoid.Human;
import net.canarymod.api.entity.living.humanoid.HumanCapabilities;
import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.api.inventory.CanaryItem;
import net.canarymod.api.inventory.Item;
import net.canarymod.api.nbt.CanaryCompoundTag;
import net.canarymod.api.nbt.CompoundTag;
import net.canarymod.api.packet.CanaryBlockChangePacket;
import net.canarymod.api.packet.CanaryPacket;
import net.canarymod.api.packet.InvalidPacketConstructionException;
import net.canarymod.api.packet.Packet;
import net.canarymod.api.potion.CanaryPotionEffect;
import net.canarymod.api.potion.PotionEffect;
import net.canarymod.api.statistics.CanaryStat;
import net.canarymod.api.statistics.Stat;
import net.canarymod.api.world.CanaryChunk;
import net.canarymod.api.world.Chunk;
import net.canarymod.api.world.blocks.CanaryBlock;
import net.canarymod.api.world.position.Position;
import net.canarymod.api.world.position.Vector3D;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.*;
import net.minecraft.stats.StatBase;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.canarymod.Canary.log;

/**
 * Packet Factory implementation
 *
 * @author Jason (darkdiplomat)
 */
public class CanaryPacketFactory implements PacketFactory {
    private final static String toofewargs = "Not enough arguments (Expected: %d Got: %d)",
            invalidArg = "Argument at Index: '%d' does not match a valid type. (Expected: '%s' Got: '%s')";

    public Packet createPacket(int id, Object... args) throws InvalidPacketConstructionException {
        if (args == null || args.length < 1) {
            throw new IllegalArgumentException("Arguments cannot be null or empty");
        }
        switch (id) {
            case 0x00: // 0
                throw new InvalidPacketConstructionException(id, "KeepAlive", "Keep Alive packets should only be hanled by the Server");
            case 0x01: // 1
                throw new InvalidPacketConstructionException(id, "JoinGame", "Join Game packets should only be handled by the Server");
            case 0x02: // 2
                verify(id, "Chat", 1, args, test(CanaryChatComponent.class, 1));
                return new CanaryPacket(new S02PacketChat(((CanaryChatComponent) args[0]).getNative()));
            case 0x03: // 3
                verify(id, "UpdateTime", 2, args, test(Long.class, 2));
                return new CanaryPacket(new S03PacketTimeUpdate((Long) args[0], (Long) args[1], false));
            case 0x04: // 4
                verify(id, "EntityEquipment", 3, args, test(Integer.class, 2), test(CanaryItem.class, 1));
                return new CanaryPacket(new S04PacketEntityEquipment((Integer) args[0], (Integer) args[1], ((CanaryItem) args[2]).getHandle()));
            case 0x05: // 5
                verify(id, "SpawnPosition", 3, args, test(Integer.class, 3));
                return new CanaryPacket(new S05PacketSpawnPosition((Integer) args[0], (Integer) args[1], (Integer) args[2]));
            case 0x06: // 6
                verify(id, "UpdateHealth", 3, args, test(Float.class, 1), test(Integer.class, 1), test(Float.class, 1));
                return new CanaryPacket(new S06PacketUpdateHealth((Float) args[0], (Integer) args[1], (Float) args[2]));
            case 0x07: // 7
                //int EnumDifficulty WorldType WorldSettings.GameType
                return new CanaryPacket(new S07PacketRespawn());
            case 0x08: // 8
                verify(id, "PlayerPosLook", 6, args, test(Double.class, 3), test(Float.class, 2), test(Boolean.class, 1));
                return new CanaryPacket(new S08PacketPlayerPosLook((Double) args[0], (Double) args[1], (Double) args[2], (Float) args[3], (Float) args[4], (Boolean) args[5]));
            case 0x09: // 9
                verify(id, "HeldItemChange", 1, args, test(Integer.class, 1));
                return new CanaryPacket(new S09PacketHeldItemChange((Integer) args[0]));
            case 0x0A: // 10
                verify(id, "UseBed", 4, args, test(CanaryHuman.class, 1), test(Integer.class, 3));
                return new CanaryPacket(new S0APacketUseBed(((CanaryHuman) args[0]).getHandle(), (Integer) args[1], (Integer) args[2], (Integer) args[3]));
            case 0x0B: // 11
                verify(id, "Animation", 2, args, test(CanaryEntity.class, 1), test(Integer.class, 1));
                return new CanaryPacket(new S0BPacketAnimation(((CanaryEntity) args[0]).getHandle(), (Integer) args[1]));
            case 0x0C: // 12
                verify(id, "SpawnPlayer", 1, args, test(CanaryHuman.class, 1));
                return new CanaryPacket(new S0CPacketSpawnPlayer(((CanaryHuman) args[0]).getHandle()));
            case 0x0D: // 13
                verify(id, "CollectItem", 2, args, test(Integer.class, 2));
                return new CanaryPacket(new S0DPacketCollectItem((Integer) args[0], (Integer) args[1]));
            case 0x0E: // 14
                if (args.length > 2) {
                    verify(id, "SpawnObject", 2, args, test(CanaryEntity.class, 1), test(Integer.class, 2));
                    return new CanaryPacket(new S0EPacketSpawnObject(((CanaryEntity) args[0]).getHandle(), (Integer) args[1], (Integer) args[2]));
                }
                verify(id, "SpawnObject", 2, args, test(CanaryEntity.class, 1), test(Integer.class, 1));
                return new CanaryPacket(new S0EPacketSpawnObject(((CanaryEntity) args[0]).getHandle(), (Integer) args[1]));
            case 0x0F: // 15
                verify(id, "SpawnMob", 1, args, test(CanaryLivingBase.class, 1));
                return new CanaryPacket(new S0FPacketSpawnMob(((CanaryLivingBase) args[0]).getHandle()));
            case 0x10: // 16
                verify(id, "SpawnPainting", 1, args, test(CanaryPainting.class, 1));
                return new CanaryPacket(new S10PacketSpawnPainting(((CanaryPainting) args[0]).getHandle()));
            case 0x11: // 17
                verify(id, "SpawnExperienceOrb", 1, args, test(CanaryXPOrb.class, 1));
                return new CanaryPacket(new S11PacketSpawnExperienceOrb(((CanaryXPOrb) args[0]).getHandle()));
            case 0x12: // 18
                if (args.length > 1) {
                    verify(id, "EntityVelocity", 4, args, test(Integer.class, 1), test(Double.class, 3));
                    return new CanaryPacket(new S12PacketEntityVelocity((Integer) args[0], (Double) args[1], (Double) args[2], (Double) args[3]));
                }
                verify(id, "EntityVelocity", 1, args, test(CanaryEntity.class, 1));
                return new CanaryPacket(new S12PacketEntityVelocity(((CanaryEntity) args[0]).getHandle()));
            case 0x13: // 19
                verify(id, "DestroyEntities", 1, args, test(int[].class, 1));
                return new CanaryPacket(new S13PacketDestroyEntities((int[]) args[0]));
            case 0x14: // 20
                throw new InvalidPacketConstructionException(id, "Entity", "Is abstract packet for ids 15, 16, and 17");
            case 0x15: // 21
                verify(id, "EntityRelMove", 4, args, test(Integer.class, 1), test(Byte.class, 3));
                return new CanaryPacket(new S14PacketEntity.S15PacketEntityRelMove((Integer) args[0], (Byte) args[1], (Byte) args[2], (Byte) args[3]));
            case 0x16: // 22
                verify(id, "EntityLook", 3, args, test(Integer.class, 1), test(Byte.class, 2));
                return new CanaryPacket(new S14PacketEntity.S16PacketEntityLook((Integer) args[0], (Byte) args[1], (Byte) args[2]));
            case 0x17: // 23
                verify(id, "EntityLookMove", 6, args, test(Integer.class, 1), test(Byte.class, 5));
                return new CanaryPacket(new S14PacketEntity.S17PacketEntityLookMove((Integer) args[0], (Byte) args[1], (Byte) args[2], (Byte) args[3], (Byte) args[4], (Byte) args[5]));
            case 0x18: // 24
                if (args.length > 1) {
                    verify(id, "EntityTeleport", 6, args, test(Integer.class, 4), test(Byte.class, 2));
                    return new CanaryPacket(new S18PacketEntityTeleport((Integer) args[0], (Integer) args[1], (Integer) args[2], (Integer) args[3], (Byte) args[4], (Byte) args[5]));
                }
                verify(id, "EntityTeleport", 1, args, test(CanaryEntity.class, 1));
                return new CanaryPacket(new S18PacketEntityTeleport(((CanaryEntity) args[0]).getHandle()));
            case 0x19: // 25
                verify(id, "EntityStatus", 2, args, test(CanaryEntity.class, 1), test(Byte.class, 1));
                return new CanaryPacket(new S19PacketEntityStatus(((CanaryEntity) args[0]).getHandle(), (Byte) args[1]));
            //case 0x1A: UNKNOWN PACKET // 26
            case 0x1B: // 27
                verify(id, "EntityAttach", 3, args, test(Integer.class, 1), test(CanaryEntity.class, 2));
                return new CanaryPacket(new S1BPacketEntityAttach((Integer) args[0], ((CanaryEntity) args[1]).getHandle(), ((CanaryEntity) args[2]).getHandle()));
            case 0x1C: // 28
                throw new InvalidPacketConstructionException(id, "EntityMetadata", "CanaryMod is currently unable to handle creation of this packet.");
            case 0x1D: // 29
                verify(id, "EntityEffect", 2, args, test(Integer.class, 1), test(CanaryPotionEffect.class, 1));
                return new CanaryPacket(new S1DPacketEntityEffect((Integer) args[0], ((CanaryPotionEffect) args[1]).getHandle()));
            case 0x1E: // 30
                verify(id, "RemoveEntityEffect", 2, args, test(Integer.class, 1), test(CanaryPotionEffect.class, 1));
                return new CanaryPacket(new S1EPacketRemoveEntityEffect((Integer) args[0], ((CanaryPotionEffect) args[1]).getHandle()));
            case 0x1F: // 31
                verify(id, "SetExperience", 3, args, test(Float.class, 1), test(Integer.class, 2));
                return new CanaryPacket(new S1FPacketSetExperience((Float) args[0], (Integer) args[1], (Integer) args[2]));
            case 0x20: // 32
                throw new InvalidPacketConstructionException(id, "EntityProperties", "CanaryMod is currently unable to handle creation of this packet.");
            case 0x21: // 33
                verify(id, "ChunkData", 3, args, test(CanaryChunk.class, 1), test(Boolean.class, 1), test(Integer.class, 1));
                return new CanaryPacket(new S21PacketChunkData(((CanaryChunk) args[0]).getHandle(), (Boolean) args[1], (Integer) args[2]));
            case 0x22: // 34
                verify(id, "MultiBlockChange", 3, args, test(Integer.class, 1), test(short[].class, 1), test(CanaryChunk.class, 1));
                return new CanaryPacket(new S22PacketMultiBlockChange((Integer) args[0], (short[]) args[1], ((CanaryChunk) args[2]).getHandle()));
            case 0x23: // 35
                if (args.length > 1) {
                    verify(id, "BlockChange", 5, args, test(Integer.class, 5));
                    return new CanaryBlockChangePacket((Integer) args[0], (Integer) args[1], (Integer) args[2], (Integer) args[3], (Integer) args[4]);
                }
                verify(id, "BlockChange", 1, args, test(CanaryBlock.class, 1));
                return new CanaryBlockChangePacket((CanaryBlock) args[0]);
            case 0x24: // 36
                verify(id, "BlockAction", 6, args, test(Integer.class, 6));
                return new CanaryPacket(new S24PacketBlockAction((Integer) args[0], (Integer) args[1], (Integer) args[2], Block.e((Integer) args[3]), (Integer) args[4], (Integer) args[5]));
            case 0x25: // 37
                verify(id, "BlockBreakAnim", 5, args, test(Integer.class, 5));
                return new CanaryPacket(new S25PacketBlockBreakAnim((Integer) args[0], (Integer) args[1], (Integer) args[2], (Integer) args[3], (Integer) args[4]));
            case 0x26: // 38
                verify(id, "MapChunkBulk", 1, args, test(List.class, 1));
                ArrayList<net.minecraft.world.chunk.Chunk> nmsChunks = new ArrayList<net.minecraft.world.chunk.Chunk>();
                for (Object chunk : (List) args[0]) {
                    nmsChunks.add(((CanaryChunk) chunk).getHandle());
                }
                return new CanaryPacket(new S26PacketMapChunkBulk(nmsChunks));
            case 0x27: // 39
                verify(id, "Explosion", 6, args, test(Double.class, 3), test(Float.class, 1), test(List.class, 1), test(Vector3D.class, 1));
                ArrayList<ChunkPosition> cp = new ArrayList<ChunkPosition>();
                for (Object position : (List) args[4]) {
                    cp.add(new ChunkPosition(((Position) position).getBlockX(), ((Position) position).getBlockY(), ((Position) position).getBlockZ()));
                }
                Vector3D v3D = (Vector3D) args[5];
                return new CanaryPacket(new S27PacketExplosion((Double) args[0], (Double) args[1], (Double) args[2], (Float) args[3], cp, Vec3.a(v3D.getX(), v3D.getY(), v3D.getZ())));
            case 0x28: // 40
                verify(id, "Effect", 6, args, test(Integer.class, 5), test(Boolean.class, 1));
                return new CanaryPacket(new S28PacketEffect((Integer) args[0], (Integer) args[1], (Integer) args[2], (Integer) args[3], (Integer) args[4], (Boolean) args[5]));
            case 0x29: // 41
                verify(id, "SoundEffect", 6, args, test(String.class, 1), test(Double.class, 3), test(Float.class, 2));
                return new CanaryPacket(new S29PacketSoundEffect((String) args[0], (Double) args[1], (Double) args[2], (Double) args[3], (Float) args[4], (Float) args[5]));
            case 0x2A: // 42
                verify(id, "Particles", 9, args, test(String.class, 1), test(Float.class, 7), test(Integer.class, 1));
            case 0x2B: // 43
                verify(id, "ChangeGameState", 2, args, test(Integer.class, 1), test(Float.class, 1));
                return new CanaryPacket(new S2BPacketChangeGameState((Integer) args[0], (Float) args[1]));
            case 0x2C: // 44
                verify(id, "SpawnGlobalEntity", 1, args, test(CanaryEntity.class, 1));
                return new CanaryPacket(new S2CPacketSpawnGlobalEntity(((CanaryEntity) args[0]).getHandle()));
            case 0x2D: // 45
                verify(id, "OpenWindow", 5, args, test(Integer.class, 2), test(String.class, 1), test(Integer.class, 1), test(Boolean.class, 1));
                return new CanaryPacket(new S2DPacketOpenWindow((Integer) args[0], (Integer) args[1], (String) args[2], (Integer) args[3], (Boolean) args[4]));
            case 0x2E: // 46
                verify(id, "CloseWindow", 1, args, test(Integer.class, 1));
                return new CanaryPacket(new S2EPacketCloseWindow((Integer) args[0]));
            case 0x2F: // 47
                verify(id, "SetSlot", 3, args, test(Integer.class, 2), test(CanaryItem.class, 1));
                return new CanaryPacket(new S2FPacketSetSlot((Integer) args[0], (Integer) args[1], ((CanaryItem) args[2]).getHandle()));
            case 0x30: // 48
                verify(id, "WindowItems", 2, args, test(Integer.class, 1), test(List.class, 1));
                ArrayList<ItemStack> nmsItems = new ArrayList<ItemStack>();
                for (Object item : (List) args[1]) {
                    nmsItems.add(item == null ? null : ((CanaryItem) item).getHandle());
                }
                return new CanaryPacket(new S30PacketWindowItems((Integer) args[0], nmsItems));
            case 0x31: // 49
                verify(id, "WindowProperty", 3, args, test(Integer.class, 3));
                return new CanaryPacket(new S31PacketWindowProperty((Integer) args[0], (Integer) args[1], (Integer) args[2]));
            case 0x32: // 50
                verify(id, "ConfirmTransaction", 3, args, test(Integer.class, 1), test(Short.class, 1), test(Boolean.class, 1));
                return new CanaryPacket(new S32PacketConfirmTransaction((Integer) args[0], (Short) args[1], (Boolean) args[2]));
            case 0x33: // 51
                verify(id, "UpdateSign", 4, args, test(Integer.class, 3), test(String[].class, 1));
                return new CanaryPacket(new S33PacketUpdateSign((Integer) args[0], (Integer) args[1], (Integer) args[2], (String[]) args[3]));
            case 0x34: // 52
                verify(id, "Maps", 2, args, test(Integer.class, 1), test(byte[].class, 1));
                return new CanaryPacket(new S34PacketMaps((Integer) args[0], (byte[]) args[1]));
            case 0x35: // 53
                verify(id, "UpdateTileEntity", 5, args, test(Integer.class, 4), test(CanaryCompoundTag.class, 1));
                return new CanaryPacket(new S35PacketUpdateTileEntity((Integer) args[0], (Integer) args[1], (Integer) args[2], (Integer) args[3], ((CanaryCompoundTag) args[4]).getHandle()));
            case 0x36: // 54
                verify(id, "SignEditorOpen", 3, args, test(Integer.class, 3));
                return new CanaryPacket(new S36PacketSignEditorOpen((Integer) args[0], (Integer) args[1], (Integer) args[2]));
            case 0x37: // 55
                verify(id, "Statistics", 1, args, test(Map.class, 1));
                HashMap<StatBase, Integer> nmsStatMap = new HashMap<StatBase, Integer>();
                for (Map.Entry<Stat, Integer> entry : ((Map<Stat, Integer>) args[0]).entrySet()) {
                    nmsStatMap.put(((CanaryStat) entry.getKey()).getHandle(), entry.getValue());
                }
                return new CanaryPacket(new S37PacketStatistics(nmsStatMap));
            case 0x38:
                verify(id, "PlayerListItem", 3, args, test(String.class, 1), test(Boolean.class, 1), test(Integer.class, 1));
                return new CanaryPacket(new S38PacketPlayerListItem((String) args[0], (Boolean) args[1], (Integer) args[2]));
            case 0x39:
                verify(id, "PlayerAbilities", 1, args, test(HumanCapabilities.class, 1));
                return new CanaryPacket(new S39PacketPlayerAbilities(((CanaryHumanCapabilities) args[0]).getHandle()));
            case 0x3A:
                throw new InvalidPacketConstructionException(id, "TabComplete", "No function unless requested and client waiting.");
            case 0x3B:
                throw new InvalidPacketConstructionException(id, "ScoreboardObjective", "Use the Scoreboard API instead.");
            case 0x3C:
                throw new InvalidPacketConstructionException(id, "UpdateScore", "Use the Scoreboard API instead.");
            case 0x3D:
                throw new InvalidPacketConstructionException(id, "DisplayScoreboard", "Use the Scoreboard API instead.");
            case 0x3E:
                throw new InvalidPacketConstructionException(id, "Teams", "Use the Scoreboard API instead.");
            case 0x3F:
                throw new InvalidPacketConstructionException(id, "CustomPayload", "Use ChannelManager instead.");
            case 0x40:
                throw new InvalidPacketConstructionException(id, "Disconnect", "Use kick methods instead.");
            default:
                throw new InvalidPacketConstructionException(id, "UNKNOWN", "Unknown Packet ID");
        }
    }

    @Override
    public Packet chat(ChatComponent chatComponent) {
        try {
            return createPacket(2, chatComponent);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet updateTime(long world_age, long time) {
        try {
            return createPacket(3, world_age, time);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet entityEquipment(int entityID, int slot, Item item) {
        try {
            return createPacket(4, entityID, slot, item);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet spawnPosition(int x, int y, int z) {
        try {
            return createPacket(5, x, y, z);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet updateHealth(float health, int foodLevel, float saturation) {
        try {
            return createPacket(6, health, foodLevel, saturation);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet playerPositionLook(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        try {
            return createPacket(7, x, y, z, yaw, pitch, onGround);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet heldItemChange(int slot) {
        try {
            return createPacket(8, slot);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet useBed(Player player, int x, int y, int z) {
        try {
            return createPacket(9, player, x, y, z);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet animation(Player player, int animation) {
        try {
            return createPacket(10, player, animation);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet spawnPlayer(Human human) {
        try {
            return createPacket(11, human);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet collectItem(int entityItemID, int collectorID) {
        try {
            return createPacket(12, entityItemID, collectorID);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet spawnObject(Entity entity, int objectID) {
        try {
            return createPacket(13, entity, objectID);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet spawnObject(Entity entity, int objectID, int throwerID) {
        try {
            return createPacket(13, entity, objectID, throwerID);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet spawnMob(LivingBase livingbase) {
        try {
            return createPacket(14, livingbase);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet spawnPainting(Painting painting) {
        try {
            return createPacket(15, painting);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet spawnXPOrb(XPOrb xporb) {
        try {
            return createPacket(16, xporb);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet entityVelocity(int entityID, double motX, double motY, double motZ) {
        try {
            return createPacket(17, entityID, motX, motY, motZ);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet destroyEntities(int... ids) {
        try {
            return createPacket(18, new Object[]{ ids });
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet entityRelativeMove(int entityID, byte x, byte y, byte z) {
        try {
            return createPacket(21, entityID, x, y, z);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet entityLook(int entityID, byte yaw, byte pitch) {
        try {
            return createPacket(22, entityID, yaw, pitch);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet entityLookRelativeMove(int entityID, byte x, byte y, byte z, byte yaw, byte pitch) {
        try {
            return createPacket(23, entityID, x, y, z, yaw, pitch);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet entityTeleport(Entity entity) {
        try {
            return createPacket(24, entity);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet entityTeleport(int entityID, int x, int y, int z, byte yaw, byte pitch) {
        try {
            return createPacket(24, entityID, x, y, z, yaw, pitch);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet entityStatus(int entityID, byte status) {
        try {
            return createPacket(25, entityID, status);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet attachEntity(int leashId, Entity attaching, Entity vehicle) {
        try {
            return createPacket(27, leashId, attaching, vehicle);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet entityMetaData(int entityID, DataWatcher watcher) {
        try {
            return createPacket(28, entityID, watcher);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet entityEffect(int entityID, PotionEffect effect) {
        try {
            return createPacket(29, entityID, effect);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet removeEntityEffect(int entityID, PotionEffect effect) {
        try {
            return createPacket(30, entityID, effect);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet setExperience(float bar, int level, int totalXp) {
        try {
            return createPacket(31, bar, level, totalXp);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet chunkData(Chunk chunk, boolean initialize, int bitflag) {
        try {
            return createPacket(33, chunk, initialize, bitflag);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet multiBlockChange(int size, short[] chunkBlocks, Chunk chunk) {
        try {
            return createPacket(34, size, chunkBlocks, chunk);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet blockChange(int x, int y, int z, int typeId, int data) {
        try {
            return createPacket(35, x, y, z, typeId, data);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet blockAction(int x, int y, int z, int stat1, int stat2, int targetId) {
        try {
            return createPacket(36, x, y, z, stat1, stat2, targetId);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet blockBreakAnimation(int entityId, int x, int y, int z, int state) {
        try {
            return createPacket(37, entityId, x, y, z, state);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet mapChunkBulk(List<Chunk> chunks) {
        try {
            return createPacket(38, chunks);
        }
        catch (Exception ex) {
            log.error("Failed to construct a MapChunkBulk packet", ex);
            return null;
        }
    }

    @Override
    public Packet explosion(double explodeX, double explodeY, double explodeZ, float power, List<Position> affectedPositions, Vector3D playerVelocity) {
        try {
            return createPacket(39, explodeX, explodeY, explodeZ, power, affectedPositions, playerVelocity);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet effect(int sfxID, int x, int y, int z, int aux, boolean disableRelVol) {
        try {
            return createPacket(40, sfxID, x, y, z, aux, disableRelVol);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet soundEffect(String name, double x, double y, double z, float volume, float pitch) {
        try {
            return createPacket(41, name, x, y, z, volume, pitch);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet particles(String name, float f1, float f2, float f3, float f4, float f5, float f6, float f7, int i1) {
        try {
            return createPacket(42, name, f1, f2, f3, f4, f5, f6, f7, i1);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet changeGameState(int state, int mode) {
        try {
            return createPacket(43, state, mode);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet spawnGlobalEntity(Entity entity) {
        try {
            return createPacket(44, entity);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet openWindow(int windowId, int type, String title, int slots, boolean useTitle) {
        try {
            return createPacket(45, windowId, type, title, slots, useTitle);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet closeWindow(int windowId) {
        try {
            return createPacket(46, windowId);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet setSlot(int windowId, int slotId, Item item) {
        try {
            return createPacket(47, windowId, slotId, item);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet setWindowItems(int windowId, List<Item> items) {
        try {
            return createPacket(48, windowId, items);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet updateWindowProperty(int windowId, int bar, int value) {
        try {
            return createPacket(49, windowId, bar, value);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet updateSign(int x, int y, int z, String[] text) {
        try {
            return createPacket(51, x, y, z, text);
        }
        catch (Exception ex) {
            log.error("Failed to construct a UpdateSign packet", ex);
            return null;
        }
    }

    @Override
    public Packet maps(short mapId, byte[] data) {
        try {
            return createPacket(52, mapId, data);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet updateTileEntity(int x, int y, int z, int action, CompoundTag compoundTag) {
        try {
            return createPacket(53, x, y, z, action, compoundTag);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet signEditorOpen(int x, int y, int z) {
        try {
            return createPacket(54, x, y, z);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet statistics(Map<Stat, Integer> stats) {
        try {
            return createPacket(55, stats);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    @Override
    public Packet playerListItem(String name, boolean connected, int ping) {
        try {
            return createPacket(56, name, connected, ping);
        }
        catch (InvalidPacketConstructionException ipcex) {
            log.trace(ipcex);
        }
        return null;
    }

    // Magical darkdiplomat stuff
    private void verify(int packetId, String packetName, int minParam, Object[] arguments, ParameterTest... testParams) throws InvalidPacketConstructionException {
        if (arguments.length < minParam) {
            throw new InvalidPacketConstructionException(packetId, packetName, String.format(toofewargs, minParam, arguments.length));
        }

        int index = 0;
        for (int test = 0; test < testParams.length; test++) {
            for (int repeated = 0; repeated < testParams[test].repeat; repeated++) {
                if (!testParams[test].type.isAssignableFrom(arguments[index].getClass())) {
                    throw new InvalidPacketConstructionException(packetId, packetName, String.format(invalidArg, index, testParams[test].type.getSimpleName(), arguments[index].getClass().getSimpleName()));
                }
                index++;
            }
        }
    }

    private <T> ParameterTest test(Class<T> type, int repeat) { // Reduce typing out new ParameterTest a ton of times
        return new ParameterTest(type, repeat);
    }

    private class ParameterTest<T> { // Container for Test Parameters
        final Class<T> type;
        final int repeat;

        ParameterTest(Class<T> type, int repeat) {
            this.type = type;
            this.repeat = repeat;
        }
    }
    //
}

