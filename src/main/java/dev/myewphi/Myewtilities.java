package dev.myewphi;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import dev.myewphi.commands.ExampleCommand;
import dev.myewphi.config.ExampleConfig;
import dev.myewphi.events.ExampleEvent;
import dev.myewphi.interactions.FixedChangeStatInteraction;
import dev.myewphi.interactions.FixedStatsConditionInteraction;

import javax.annotation.Nonnull;

public class Myewtilities extends JavaPlugin {

    private static Config<ExampleConfig> config = null;

    public Myewtilities(@Nonnull JavaPluginInit init) {
        super(init);
        config = this.withConfig("example_config", ExampleConfig.CODEC);
    }

    @Override
    protected void setup() {
        config.save();
        this.getCommandRegistry().registerCommand(new ExampleCommand("example", "An example command"));
        if (getConfig().get().isEnabledWelcomeMessage()) {
            this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);
        }

        this.getCodecRegistry(Interaction.CODEC).register("FixedChangeStat", FixedChangeStatInteraction.class, FixedChangeStatInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("FixedStatsCondition", FixedStatsConditionInteraction.class, FixedStatsConditionInteraction.CODEC);
    }

    public static Config<ExampleConfig> getConfig() {
        return config;
    }
}