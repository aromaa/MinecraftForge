package net.minecraftforge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class TransferMessages
{
    public static class S2CTransferServer
    {
        private final FriendlyByteBuf data;

        public S2CTransferServer(FriendlyByteBuf data)
        {
            if (data.writerIndex() > 1024)
            {
                throw new IllegalArgumentException("Payload may not be larger than 1024 bytes");
            }

            this.data = data;
        }

        public void encode(FriendlyByteBuf buffer)
        {
            buffer.writeBytes(data.slice());
        }

        public static S2CTransferServer decode(FriendlyByteBuf buffer)
        {
            int i = buffer.readableBytes();
            if (i > 1024)
            {
                throw new IllegalArgumentException("Payload may not be larger than 1024 bytes");
            }

            return new S2CTransferServer(new FriendlyByteBuf(buffer.readBytes(i)));
        }

        public static void handle(S2CTransferServer message, Supplier<NetworkEvent.Context> contextSupplier)
        {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() ->
            {
                try
                {
                    Connection connection = context.getNetworkManager();
                    if (!(connection.getPacketListener() instanceof ClientPacketListener listener) || listener.getServerData() == null)
                    {
                        return;
                    }

                    ServerData serverData = listener.getServerData();
                    ServerAddress address = ServerAddress.parseString(serverData.ip);

                    connection.disconnect(Component.translatable("multiplayer.status.quitting"));

                    byte[] data = new byte[message.data.readableBytes()];
                    message.data.readBytes(data);

                    serverData.transferData = data;

                    ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), Minecraft.getInstance(), address, serverData);
                }
                finally
                {
                    message.data.release();
                }
            });

            context.setPacketHandled(true);
        }
    }
}
