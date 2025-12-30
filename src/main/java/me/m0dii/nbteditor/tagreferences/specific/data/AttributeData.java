package me.m0dii.nbteditor.tagreferences.specific.data;

import lombok.Getter;
import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.TextInst;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.UUID;

public record AttributeData(EntityAttribute attribute, double value, Optional<AttributeModifierData> modifierData) {

    public AttributeData(EntityAttribute attribute, double value) {
        this(attribute, value, Optional.empty());
    }

    public AttributeData(EntityAttribute attribute, double value, AttributeModifierData.Operation operation, AttributeModifierData.Slot slot, AttributeModifierData.AttributeModifierId id) {
        this(attribute, value, Optional.of(new AttributeModifierData(operation, slot, id)));
    }

    public static AttributeData fromComponentEntry(AttributeModifiersComponent.Entry entry) {
        return new AttributeData(
                entry.attribute().value(),
                entry.modifier().value(),
                Optional.of(AttributeModifierData.fromMinecraft(entry.modifier(), entry.slot())));
    }

    public AttributeModifiersComponent.Entry toComponentEntry() {
        return new AttributeModifiersComponent.Entry(
                Registries.ATTRIBUTE.getEntry(attribute),
                modifierData.get().toMinecraft(value),
                (AttributeModifierSlot) modifierData.get().slot().toMinecraft());
    }

    public record AttributeModifierData(Operation operation, Slot slot, AttributeModifierId id) {

        public static AttributeModifierData fromMinecraft(EntityAttributeModifier modifier, AttributeModifierSlot slot) {
            return new AttributeModifierData(
                    Operation.fromMinecraft(modifier.operation()),
                    Slot.fromMinecraft(slot),
                    AttributeModifierId.fromMinecraft(modifier));
        }

        public EntityAttributeModifier toMinecraft(double value) {
            return id.toMinecraft(value, operation.toMinecraft());
        }

        public enum Operation {
            ADD("nbteditor.attributes.operation.add"),
            ADD_MULTIPLIED_BASE("nbteditor.attributes.operation.add_multiplied_base"),
            ADD_MULTIPLIED_TOTAL("nbteditor.attributes.operation.add_multiplied_total");

            private final Text name;

            Operation(String key) {
                this.name = TextInst.translatable(key);
            }

            public static Operation fromMinecraft(EntityAttributeModifier.Operation operation) {
                return switch (operation) {
                    case ADD_VALUE -> ADD;
                    case ADD_MULTIPLIED_BASE -> ADD_MULTIPLIED_BASE;
                    case ADD_MULTIPLIED_TOTAL -> ADD_MULTIPLIED_TOTAL;
                };
            }

            public EntityAttributeModifier.Operation toMinecraft() {
                return switch (this) {
                    case ADD -> EntityAttributeModifier.Operation.ADD_VALUE;
                    case ADD_MULTIPLIED_BASE -> EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    case ADD_MULTIPLIED_TOTAL -> EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                };
            }

            @Override
            public String toString() {
                return name.getString();
            }
        }

        public enum Slot {
            ANY("nbteditor.attributes.slot.any", false),
            HAND("nbteditor.attributes.slot.hand", true),
            MAINHAND("nbteditor.attributes.slot.mainhand", false),
            OFFHAND("nbteditor.attributes.slot.offhand", false),
            ARMOR("nbteditor.attributes.slot.armor", true),
            HEAD("nbteditor.attributes.slot.head", false),
            CHEST("nbteditor.attributes.slot.chest", false),
            LEGS("nbteditor.attributes.slot.legs", false),
            FEET("nbteditor.attributes.slot.feet", false),
            BODY("nbteditor.attributes.slot.body", true);

            private final Text name;

            @Getter
            private final boolean onlyForComponents;

            Slot(String key, boolean onlyForComponents) {
                this.name = TextInst.translatable(key);
                this.onlyForComponents = onlyForComponents;
            }

            public static Slot fromMinecraft(Object slot) {
                return switch ((AttributeModifierSlot) slot) {
                    case ANY -> ANY;
                    case HAND -> HAND;
                    case MAINHAND -> MAINHAND;
                    case OFFHAND -> OFFHAND;
                    case ARMOR -> ARMOR;
                    case HEAD -> HEAD;
                    case CHEST -> CHEST;
                    case LEGS -> LEGS;
                    case FEET -> FEET;
                    case BODY -> BODY;
                };
            }

            public Object toMinecraft() {
                return switch (this) {
                    case ANY -> AttributeModifierSlot.ANY;
                    case HAND -> AttributeModifierSlot.HAND;
                    case MAINHAND -> AttributeModifierSlot.MAINHAND;
                    case OFFHAND -> AttributeModifierSlot.OFFHAND;
                    case ARMOR -> AttributeModifierSlot.ARMOR;
                    case HEAD -> AttributeModifierSlot.HEAD;
                    case CHEST -> AttributeModifierSlot.CHEST;
                    case LEGS -> AttributeModifierSlot.LEGS;
                    case FEET -> AttributeModifierSlot.FEET;
                    case BODY -> AttributeModifierSlot.BODY;
                };
            }

            public boolean isInThisVersion() {
                return !onlyForComponents;
            }

            @Override
            public String toString() {
                return name.getString();
            }
        }

        public static class AttributeModifierId {

            public static final boolean ID_IS_IDENTIFIER = true;

            private final Object id;

            public AttributeModifierId(UUID id) {
                this.id = id;
            }

            public AttributeModifierId(Identifier id) {
                this.id = id;
            }

            public static AttributeModifierId randomUUID() {
                return new AttributeModifierId(UUID.randomUUID());
            }

            public static AttributeModifierId fromMinecraft(EntityAttributeModifier modifier) {
                return new AttributeModifierId(modifier.id());
            }

            public UUID getUUID() {
                return (UUID) id;
            }

            public Identifier getIdentifier() {
                if (id instanceof UUID uuid) {
                    return IdentifierInst.of("minecraft", uuid.toString());
                }
                return (Identifier) id;
            }

            public EntityAttributeModifier toMinecraft(double value, EntityAttributeModifier.Operation operation) {
                return new EntityAttributeModifier(getIdentifier(), value, operation);

            }

        }

    }

}
