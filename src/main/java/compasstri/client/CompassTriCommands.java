package compasstri.client;

import com.mojang.brigadier.CommandDispatcher;
import compasstri.Readings;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * /compasstri [show | record | undo | clear | waypoint | item] - all on this computer only; nothing is sent to the
 * server.
 */
public final class CompassTriCommands {
    private CompassTriCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext context) {
        dispatcher.register(literal("compasstri")
            .executes(c -> show(c.getSource()))
            .then(literal("show").executes(c -> show(c.getSource())))
            .then(literal("record").executes(c -> record(c.getSource())))
            .then(literal("undo").executes(c -> {
                c.getSource().sendFeedback(Readings.undo()
                    ? Component.translatable("compasstri.msg.undone", Readings.entries().size())
                    : Component.translatable("compasstri.msg.no_readings"));
                return 1;
            }))
            .then(literal("clear").executes(c -> {
                Readings.clear();
                c.getSource().sendFeedback(Component.translatable("compasstri.msg.cleared"));
                return 1;
            }))
            .then(literal("waypoint").executes(c -> {
                Waypoints.addInGame(c.getSource().getPlayer());
                return 1;
            }))
            .then(literal("item").executes(c -> item(c.getSource()))));
    }

    private static int show(FabricClientCommandSource source) {
        source.sendFeedback(Texts.status());
        return 1;
    }

    private static int record(FabricClientCommandSource source) {
        Readings.Entry entry = Readings.record(source.getPlayer());
        source.sendFeedback(Texts.recorded(entry, Readings.entries().size()));
        source.sendFeedback(Texts.status());
        return 1;
    }

    /** What the server sent for the items in the hands: to see how a server's compass works. */
    private static int item(FabricClientCommandSource source) {
        boolean any = false;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = source.getPlayer().getItemInHand(hand);
            if (stack.isEmpty()) continue;
            any = true;
            source.sendFeedback(Component.translatable("compasstri.msg.item", stack.getHoverName(),
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).withStyle(ChatFormatting.GOLD));
            var tracker = stack.get(DataComponents.LODESTONE_TRACKER);
            source.sendFeedback(tracker == null
                ? Component.translatable("compasstri.msg.item_no_tracker")
                : Component.translatable("compasstri.msg.item_tracker",
                    tracker.target().map(t -> t.dimension().identifier() + " " + t.pos().toShortString()).orElse("-"),
                    Boolean.toString(tracker.tracked())));
            var model = stack.get(DataComponents.ITEM_MODEL);
            if (model != null) source.sendFeedback(Component.translatable("compasstri.msg.item_model", model.toString()));
            var data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
            if (data != null) source.sendFeedback(Component.translatable("compasstri.msg.item_model_data", data.toString()));
        }
        if (!any) source.sendFeedback(Component.translatable("compasstri.msg.item_none"));
        return 1;
    }
}
