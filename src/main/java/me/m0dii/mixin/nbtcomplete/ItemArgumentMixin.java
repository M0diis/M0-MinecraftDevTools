package me.m0dii.mixin.nbtcomplete;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.m0dii.modules.nbtcomplete.NbtCommandSuggestions;
import net.minecraft.commands.arguments.item.ItemArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Vanilla completes item component names ({@code /give @s stone[rar}) but not their values; this adds common
 * starting values after {@code component=}.
 */
@Mixin(ItemArgument.class)
public abstract class ItemArgumentMixin {
    @Inject(method = "listSuggestions", at = @At("RETURN"), cancellable = true, require = 0)
    private void m0dev$componentValues(CommandContext<?> context, SuggestionsBuilder builder,
                                       CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
        Suggestions extra = NbtCommandSuggestions.componentValues(builder);
        if (extra == null || extra.isEmpty()) {
            return;
        }
        String input = builder.getInput();
        CompletableFuture<Suggestions> original = cir.getReturnValue();
        cir.setReturnValue(original == null
                ? CompletableFuture.completedFuture(extra)
                : original.thenApply(vanilla -> Suggestions.merge(input, List.of(vanilla, extra))));
    }
}
