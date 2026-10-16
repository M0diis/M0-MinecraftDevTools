package bettercommandblockui.main.ui.screen;

import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.world.level.BaseCommandBlock;

public class BetterMinecartCommandBlockScreen extends AbstractBetterCommandBlockScreen {

    public static BetterMinecartCommandBlockScreen instance;
    private final MinecartCommandBlock minecart;

    public BetterMinecartCommandBlockScreen(MinecartCommandBlock minecart) {
        this.commandExecutor = minecart.getCommandBlock();
        this.minecart = minecart;
        instance = this;
        updated = true;
    }

    @Override
    public void init() {
        super.init();
        consoleCommandTextField.setValue(commandExecutor.getCommand());
    }

    @Override
    protected void syncSettingsToServer(BaseCommandBlock commandExecutor) {
        this.minecraft.getConnection().send(new ServerboundSetCommandMinecartPacket(minecart.getId(), this.consoleCommandTextField.getValue(), commandExecutor.isTrackOutput()));
    }
}
