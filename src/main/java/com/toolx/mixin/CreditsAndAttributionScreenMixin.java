package com.toolx.mixin;

import com.toolx.gui.ToolXSettingsScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreditsAndAttributionScreen.class, priority = 1500)
public abstract class CreditsAndAttributionScreenMixin extends Screen {
	@Unique
	private static final int BUTTON_SPACING = 8;

	@Unique
	private Button toolx$settingsButton;

	protected CreditsAndAttributionScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void toolx$addSettingsButton(CallbackInfo info) {
		this.toolx$settingsButton = Button.builder(Component.literal("ToolX"), pressed -> {
			this.minecraft.gui.setScreen(new ToolXSettingsScreen(this, this.minecraft.options));
		}).width(210).build();
		this.addRenderableWidget(this.toolx$settingsButton);
		this.toolx$positionSettingsButton();
	}

	@Inject(method = "repositionElements", at = @At("TAIL"))
	private void toolx$repositionSettingsButton(CallbackInfo info) {
		this.toolx$positionSettingsButton();
	}

	@Unique
	private void toolx$positionSettingsButton() {
		if (this.toolx$settingsButton == null) {
			return;
		}

		Component licenses = Component.translatable("credits_and_attribution.button.licenses");
		Button licensesButton = null;
		Button vanillaXButton = null;

		for (GuiEventListener child : this.children()) {
			if (child != this.toolx$settingsButton && child instanceof Button button) {
				String text = button.getMessage().getString();
				if (text.equals("VanillaX")) {
					vanillaXButton = button;
				} else if (text.equals(licenses.getString())) {
					licensesButton = button;
				}
			}
		}

		boolean vanillaXLoaded = FabricLoader.getInstance().isModLoaded("vanillax");

		if (vanillaXButton != null) {
			this.toolx$settingsButton.setX(vanillaXButton.getX());
			this.toolx$settingsButton.setY(vanillaXButton.getY() + vanillaXButton.getHeight() + BUTTON_SPACING);
			this.toolx$settingsButton.setWidth(vanillaXButton.getWidth());
		} else if (licensesButton != null) {
			int step = licensesButton.getHeight() + BUTTON_SPACING;
			this.toolx$settingsButton.setX(licensesButton.getX());
			this.toolx$settingsButton.setY(licensesButton.getY() + (vanillaXLoaded ? step * 2 : step));
			this.toolx$settingsButton.setWidth(licensesButton.getWidth());
		} else {
			this.toolx$settingsButton.setX((this.width - 210) / 2);
			this.toolx$settingsButton.setY(this.height / 2 + (vanillaXLoaded ? 28 : 0));
			this.toolx$settingsButton.setWidth(210);
		}
	}
}
