package dev.myewphi.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.Interaction;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.ChangeStatBaseInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.util.AbilityCastUtil;

import javax.annotation.Nonnull;

public class FixedChangeStatInteraction extends ChangeStatBaseInteraction {
    public static final BuilderCodec<FixedChangeStatInteraction> CODEC = BuilderCodec.builder(FixedChangeStatInteraction.class, FixedChangeStatInteraction::new, ChangeStatBaseInteraction.CODEC)
            .documentation("Changes the given stats.")
            .append(
                    new KeyedCodec<>("UseAbilityCost", Codec.BOOLEAN),
                    (o, v) -> o.useAbilityCost = v,
                    o -> Boolean.valueOf(o.useAbilityCost)
            )
            .documentation("Spend the casting ability's declared Cost/CostType (negated) instead of an authored StatModifiers map, so the ability item stays the single source. An ability without a cost spends nothing. Only meaningful in ability cast chains.")
            .add()
            .build();

    protected boolean useAbilityCost;

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        final var commandBuffer = context.getCommandBuffer();
        final var ref = context.getEntity();
        final var targetRef = entityTarget.getEntity(context, ref);
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }

        final var entityStatMapComponent = commandBuffer.getComponent(targetRef, EntityStatMap.getComponentType());
        if (entityStatMapComponent == null) return;

        var effectiveStats = entityStats;
        if (useAbilityCost) {
            effectiveStats = AbilityCastUtil.resolveCostMap(context, true);
        }
        if (effectiveStats == null) return;

        entityStatMapComponent.processStatChanges(EntityStatMap.Predictable.SELF, effectiveStats, valueType, changeStatBehaviour);
    }

    @Nonnull
    @Override
    protected Interaction generatePacket() {
        return new com.hypixel.hytale.protocol.ChangeStatInteraction();
    }

    @Override
    protected void configurePacket(Interaction packet) {
        super.configurePacket(packet);
        var p = (com.hypixel.hytale.protocol.ChangeStatInteraction) packet;
        p.statModifiers = entityStats;
        p.valueType = valueType;
        p.changeStatBehaviour = changeStatBehaviour;
        p.entityTarget = entityTarget.toProtocol();
        p.useAbilityCost = useAbilityCost;
    }

    @Nonnull
    @Override
    public String toString() {
        return "ChangeStatInteraction{}" + super.toString();
    }
}
