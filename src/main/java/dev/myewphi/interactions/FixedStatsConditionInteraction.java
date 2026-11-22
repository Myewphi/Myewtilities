package dev.myewphi.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.Interaction;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.ValueType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.none.StatsConditionBaseInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.util.AbilityCastUtil;
import com.hypixel.hytale.server.core.modules.interaction.interaction.util.InteractionTarget;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FixedStatsConditionInteraction extends StatsConditionBaseInteraction {
    @Nonnull
    public static final BuilderCodec<FixedStatsConditionInteraction> CODEC = BuilderCodec.builder(
                    FixedStatsConditionInteraction.class, FixedStatsConditionInteraction::new, StatsConditionBaseInteraction.CODEC
            )
            .appendInherited(
                    new KeyedCodec<>("Entity", InteractionTarget.CODEC),
                    (o, i) -> o.entityTarget = i,
                    o -> o.entityTarget,
                    (o, p) -> o.entityTarget = p.entityTarget
            )
            .documentation("The entity to target for this interaction.")
            .addValidator(Validators.nonNull())
            .add()
            .documentation("Interaction that is successful if the given stat conditions match.")
            .<Boolean>append(new KeyedCodec<>("UseAbilityCost", Codec.BOOLEAN), (o, v) -> o.useAbilityCost = v, o -> o.useAbilityCost)
            .documentation(
                    "Gate on the casting ability's declared Cost/CostType instead of an authored Costs map, so the ability item stays the single source. An ability without a cost passes. Only meaningful in ability cast chains."
            )
            .add()
            .build();
    protected boolean useAbilityCost;
    protected InteractionTarget entityTarget = InteractionTarget.USER;

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        assert commandBuffer != null;
        Int2FloatMap effectiveCosts = this.costs;
        if (this.useAbilityCost) {
            effectiveCosts = AbilityCastUtil.resolveCostMap(context, false);
            if (effectiveCosts == null) {
                return;
            }
        }

        Ref<EntityStore> ref = context.getEntity();
        final var targetRef = entityTarget.getEntity(context, ref);
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }
        if (!this.canAfford(targetRef, commandBuffer, effectiveCosts)) {
            context.getState().state = InteractionState.Failed;
        }
    }

    @Override
    protected boolean canAfford(@Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
        return this.canAfford(ref, componentAccessor, this.costs);
    }

    private boolean canAfford(@Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> componentAccessor, @Nullable Int2FloatMap costs) {
        EntityStatMap entityStatMapComponent = componentAccessor.getComponent(ref, EntityStatMap.getComponentType());
        if (entityStatMapComponent == null) {
            return false;
        }

        if (costs == null) {
            return false;
        }

        ObjectIterator var5 = costs.int2FloatEntrySet().iterator();

        while (var5.hasNext()) {
            Int2FloatMap.Entry cost = (Int2FloatMap.Entry)var5.next();
            EntityStatValue stat = entityStatMapComponent.get(cost.getIntKey());
            if (stat == null) {
                return false;
            }

            float statValue = this.valueType == ValueType.Absolute ? stat.get() : stat.asPercentage() * 100.0F;
            if (this.lessThan) {
                if (statValue >= cost.getFloatValue()) {
                    return false;
                }
            } else if (statValue < cost.getFloatValue() && !this.canOverdraw(statValue, stat.getMin())) {
                return false;
            }
        }

        return true;
    }

    @Nonnull
    @Override
    protected Interaction generatePacket() {
        return new com.hypixel.hytale.protocol.StatsConditionInteraction();
    }

    @Override
    protected void configurePacket(Interaction packet) {
        super.configurePacket(packet);
        com.hypixel.hytale.protocol.StatsConditionInteraction p = (com.hypixel.hytale.protocol.StatsConditionInteraction)packet;
        p.costs = this.costs;
        p.lessThan = this.lessThan;
        p.lenient = this.lenient;
        p.valueType = this.valueType;
        p.useAbilityCost = this.useAbilityCost;
    }

    @Nonnull
    @Override
    public String toString() {
        return "StatsConditionInteraction{}" + super.toString();
    }
}
