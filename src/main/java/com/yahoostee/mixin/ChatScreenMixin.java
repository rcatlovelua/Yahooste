package com.yahoostee.mixin;

import com.yahoostee.client.YahoosteePanel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.input.KeyInput;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    
    @Shadow
    protected TextFieldWidget chatField;

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        YahoosteePanel.init((ChatScreen) (Object) this, this.width, this.height, this::addDrawableChild, this.chatField);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        YahoosteePanel.drawBackground(context, this.width, this.height);
    }
    
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        int key = input.key();

        // Блокируем ВСЁ - навигацию, Enter, Backspace
        if (key == GLFW.GLFW_KEY_TAB ||
        key == GLFW.GLFW_KEY_UP ||
            key == GLFW.GLFW_KEY_DOWN ||
            key == GLFW.GLFW_KEY_LEFT ||
            key == GLFW.GLFW_KEY_RIGHT ||
            key == GLFW.GLFW_KEY_PAGE_UP ||
            key == GLFW.GLFW_KEY_PAGE_DOWN ||
            key == GLFW.GLFW_KEY_HOME ||
            key == GLFW.GLFW_KEY_END ||
            key == GLFW.GLFW_KEY_ENTER ||
            key == GLFW.GLFW_KEY_KP_ENTER ||
            key == GLFW.GLFW_KEY_BACKSPACE) {  // Добавили Backspace
            
            cir.setReturnValue(true);
        }
    }
}