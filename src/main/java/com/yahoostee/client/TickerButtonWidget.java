package com.yahoostee.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class TickerButtonWidget extends ClickableWidget {
    private final Text fullMessage;
    private final PressAction onPress;
    private float scrollOffset = 0.0f;
    private long lastTime = 0;
    private boolean scrollForward = true;

    public interface PressAction {
        void onPress(TickerButtonWidget button);
    }

    public TickerButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message);
        this.fullMessage = message;
        this.onPress = onPress;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рисуем рамку кнопки (ванильный стиль)
        this.renderButton(context, mouseX, mouseY);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        var orderedText = this.fullMessage.asOrderedText();
        int textWidth = textRenderer.getWidth(orderedText);
        int padding = 6;
        int maxTextWidth = this.getWidth() - padding * 2;

        int textX = this.getX() + padding;
        int textY = this.getY() + (this.getHeight() - 8) / 2;
        int textColor = this.active ? 0xFFFFFF : 0xA0A0A0;

        if (textWidth <= maxTextWidth) {
            int centeredX = this.getX() + (this.getWidth() - textWidth) / 2;
            context.drawText(textRenderer, orderedText, centeredX, textY, textColor, true);
        } else {
            long currentTime = System.currentTimeMillis();
            if (lastTime == 0) lastTime = currentTime;
            long elapsed = currentTime - lastTime;
            lastTime = currentTime;

            float maxScroll = textWidth - maxTextWidth;
            float speed = 0.05f;

            if (scrollForward) {
                scrollOffset += speed * elapsed;
                if (scrollOffset >= maxScroll + 15) {
                    scrollOffset = maxScroll + 15;
                    scrollForward = false;
                }
            } else {
                scrollOffset -= speed * elapsed;
                if (scrollOffset <= -15) {
                    scrollOffset = -15;
                    scrollForward = true;
                }
            }
            float displayOffset = Math.max(0, Math.min(maxScroll, scrollOffset));

            context.enableScissor(this.getX() + padding, this.getY(), this.getX() + this.getWidth() - padding, this.getY() + this.getHeight());
            context.drawText(textRenderer, orderedText, (int) (textX - displayOffset), textY, textColor, true);
            context.disableScissor();
        }
    }

    private void renderButton(DrawContext context, int mouseX, int mouseY) {
        // Упрощенная отрисовка фона кнопки
        int color = isHovered() ? 0xFFFF0000 : 0xFF00FF00;
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);
    }

    public void onClick(double mouseX, double mouseY) {
        this.onPress.onPress(this);
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
