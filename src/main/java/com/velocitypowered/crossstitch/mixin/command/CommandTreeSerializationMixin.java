package com.velocitypowered.crossstitch.mixin.command;

import com.mojang.brigadier.arguments.ArgumentType;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings({"rawtypes", "unchecked", "AddedMixinMembersNamePattern"})
@Mixin(ClientboundCommandsPacket.class)
public class CommandTreeSerializationMixin {
    @Unique
    private static final ResourceLocation MOD_ARGUMENT_INDICATOR = new ResourceLocation("crossstitch:mod_argument");

    @Redirect(method = "writeNode", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/synchronization/ArgumentTypes;serialize(Lnet/minecraft/network/FriendlyByteBuf;Lcom/mojang/brigadier/arguments/ArgumentType;)V"))
    private static void writeNode$wrapInVelocityModArgument(FriendlyByteBuf buf, ArgumentType<?> type) {
        ArgumentTypes.Entry entry = ArgumentTypes.BY_CLASS.get(type.getClass());
        
        if (entry == null) {
            buf.writeResourceLocation(new ResourceLocation(""));
            return;
        }
        
        if (entry.name.getNamespace().equals("minecraft") || entry.name.getNamespace().equals("brigadier")) {
            buf.writeResourceLocation(entry.name);
            entry.serializer.serializeToNetwork(type, buf);
            return;
        }

        // Not a standard Minecraft argument type - so we need to wrap it
        serializeWrappedArgumentType(buf, type, entry);
    }

    @Unique
    private static void serializeWrappedArgumentType(FriendlyByteBuf packetByteBuf, ArgumentType argumentType, ArgumentTypes.Entry entry) {
        packetByteBuf.writeResourceLocation(MOD_ARGUMENT_INDICATOR);
        packetByteBuf.writeResourceLocation(entry.name);

        FriendlyByteBuf extraData = new FriendlyByteBuf(Unpooled.buffer());
        entry.serializer.serializeToNetwork(argumentType, extraData);

        packetByteBuf.writeVarInt(extraData.readableBytes());
        packetByteBuf.writeBytes(extraData);
    }
}
