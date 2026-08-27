package com.yahoostee.mixin;

import com.yahoostee.client.YahoosteePanel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    @Shadow
    protected TextFieldWidget chatField;

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        YahoosteePanel.init(
                (ChatScreen) (Object) this,
                this.width,
                this.height,
                this::addDrawableChild,
                this.chatField
        );

        // Всегда оставляем фокус на поле чата.
        this.setFocused(this.chatField);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        YahoosteePanel.drawBackground(
                context,
                this.width,
                this.height
        );
    }

    /*
     * Полностью блокируем клавиатурную навигацию
     * по кнопкам панели.
     *
     * Мышкой кнопки продолжают работать.
     */
    @Inject(
            method = "keyPressed",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onKeyPressed(
            KeyInput input,
            CallbackInfo ci
    ) {
        int key = input.key();

        // Стрелки
        if (key == 262 || // RIGHT
            key == 263 || // LEFT
            key == 264 || // DOWN
            key == 265 || // UP

            // ENTER
            key == 257 ||

            // NUMPAD ENTER
            key == 335) {

            /*
             * Если фокус каким-либо образом оказался
             * на элементе панели — просто возвращаем
             * фокус обратно в чат.
             */
            if (YahoosteePanel.isPanelWidgetFocused(this)) {
                this.setFocused(this.chatField);
            }

            /*
             * Главное:
             * ChatScreen не получает эти клавиши,
             * поэтому Screen не сможет переключать
             * фокус между кнопками панели.
             */
            ci.cancel();
        }
    }
}