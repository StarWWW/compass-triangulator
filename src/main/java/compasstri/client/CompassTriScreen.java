package compasstri.client;

import compasstri.CompassTriConfig;
import compasstri.Readings;
import compasstri.client.ui.SettingsList;
import compasstri.client.ui.SettingsScreen;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Settings, and the readings taken so far with their estimate below. */
public class CompassTriScreen extends SettingsScreen {
    private final CompassTriConfig config = CompassTriConfig.get();

    public CompassTriScreen(@Nullable Screen parent) {
        super(Component.translatable("compasstri.screen.title"), parent);
    }

    @Override
    protected void buildList(SettingsList list) {
        list.header(Component.translatable("compasstri.section.general"));
        list.row(
            list.checkbox(Component.translatable("compasstri.screen.hud"), config.hud, v -> config.hud = v,
                Component.translatable("compasstri.screen.hud.tooltip")),
            list.checkbox(Component.translatable("compasstri.screen.auto"), config.autoRecord, v -> config.autoRecord = v,
                Component.translatable("compasstri.screen.auto.tooltip")));
        list.row(sourceButton(), positionButton());
        list.row(new DistanceSlider(), new AngleSlider());

        list.header(Component.translatable("compasstri.section.readings"));
        list.row(
            Button.builder(Component.translatable("compasstri.screen.undo"), b -> Readings.undo()).build(),
            Button.builder(Component.translatable("compasstri.screen.clear"), b -> Readings.clear()).build());
    }

    private Button sourceButton() {
        return Button.builder(sourceLabel(), b -> {
                CompassTriConfig.Source[] all = CompassTriConfig.Source.values();
                config.source = all[(config.source.ordinal() + 1) % all.length];
                b.setMessage(sourceLabel());
            })
            .tooltip(Tooltip.create(Component.translatable("compasstri.screen.source.tooltip")))
            .build();
    }

    private Component sourceLabel() {
        return Component.translatable("compasstri.screen.source",
            Component.translatable("compasstri.source." + config.source.key));
    }

    private Button positionButton() {
        return Button.builder(positionLabel(), b -> {
            CompassTriConfig.HudPosition[] all = CompassTriConfig.HudPosition.values();
            config.hudPosition = all[(config.hudPosition.ordinal() + 1) % all.length];
            b.setMessage(positionLabel());
        }).build();
    }

    private Component positionLabel() {
        return Component.translatable("compasstri.screen.position",
            Component.translatable("compasstri.position." + config.hudPosition.key));
    }

    /** The readings and the estimate, kept up to date while the screen is open. */
    @Override
    protected List<Component> infoLines() {
        return List.of(Component.translatable("compasstri.hud.readings", Readings.entries().size()), Texts.status(false));
    }

    @Override
    protected void save() {
        config.save();
    }

    @Override
    protected Component resetLabel() {
        return Component.translatable("compasstri.screen.reset");
    }

    @Override
    protected Component resetQuestion() {
        return Component.translatable("compasstri.screen.reset.title");
    }

    @Override
    protected Component resetWarning() {
        return Component.translatable("compasstri.screen.reset.message");
    }

    @Override
    protected void reset() {
        config.resetToDefaults();
    }

    /** Blocks between automatic readings, in steps of 8. */
    private final class DistanceSlider extends AbstractSliderButton {
        private static final int STEP = 8;

        DistanceSlider() {
            super(0, 0, 150, 20, Component.empty(),
                (config.minDistance - CompassTriConfig.DISTANCE_MIN) / (double) (CompassTriConfig.DISTANCE_MAX - CompassTriConfig.DISTANCE_MIN));
            setTooltip(Tooltip.create(Component.translatable("compasstri.screen.min_distance.tooltip")));
            updateMessage();
        }

        private int distance() {
            double raw = CompassTriConfig.DISTANCE_MIN + value * (CompassTriConfig.DISTANCE_MAX - CompassTriConfig.DISTANCE_MIN);
            return (int) Math.round(raw / STEP) * STEP;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("compasstri.screen.min_distance", distance()));
        }

        @Override
        protected void applyValue() {
            config.minDistance = distance();
        }
    }

    /** How exactly readings taken by looking are lined up, 0.2 to 5 degrees. */
    private final class AngleSlider extends AbstractSliderButton {
        AngleSlider() {
            super(0, 0, 150, 20, Component.empty(),
                (config.angleError - CompassTriConfig.ANGLE_MIN) / (CompassTriConfig.ANGLE_MAX - CompassTriConfig.ANGLE_MIN));
            setTooltip(Tooltip.create(Component.translatable("compasstri.screen.angle_error.tooltip")));
            updateMessage();
        }

        private double angle() {
            double raw = CompassTriConfig.ANGLE_MIN + value * (CompassTriConfig.ANGLE_MAX - CompassTriConfig.ANGLE_MIN);
            return Math.round(raw * 10) / 10.0;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("compasstri.screen.angle_error", Texts.degrees(angle())));
        }

        @Override
        protected void applyValue() {
            config.angleError = angle();
        }
    }
}
