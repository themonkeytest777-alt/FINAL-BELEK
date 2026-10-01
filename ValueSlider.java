package com.turbominer.gui;

import com.turbominer.ModConfig;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.DoubleConsumer;

public class ValueSlider extends SliderWidget {
    private final String label;
    private final double min;
    private final double max;
    private final DoubleConsumer consumer;

    public ValueSlider(int x, int y, int width, int height, String label,
                       double min, double max, double current, DoubleConsumer consumer) {
        super(x, y, width, height, Text.empty(), (MathHelper.clamp(current, min, max) - min) / (max - min));
        this.label = label;
        this.min = min;
        this.max = max;
        this.consumer = consumer;
        updateMessage();
    }

    private double actual() {
        double v = min + this.value * (max - min);
        return Math.round(v * 10.0) / 10.0;
    }

    @Override
    protected void updateMessage() {
        if (label == null) return;
        setMessage(Text.literal(label + ModConfig.fmt(actual())));
    }

    @Override
    protected void applyValue() {
        consumer.accept(actual());
    }
}
