package com.toolx;

import com.mojang.blaze3d.platform.InputConstants;
import com.toolx.gui.ToolXSettingsScreen;
import com.toolx.mixin.AbstractRecipeBookScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDLVideo;

public final class KeyBindsController {
	public static final InputConstants.Key DEFAULT_PIE_ZERO_KEY = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_0);
	public static final InputConstants.Key DEFAULT_BACKSPACE_KEY = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_BACKSPACE);

	private static final int MOD_LSHIFT = 1;
	private static final int MOD_RSHIFT = 2;
	private static final int MOD_LCTRL = 64;
	private static final int MOD_RCTRL = 128;
	private static final int MOD_LALT = 256;
	private static final int MOD_RALT = 512;
	private static final int MOD_LGUI = 1024;
	private static final int MOD_RGUI = 2048;

	private static final long CHAR_SUPPRESS_WINDOW_MS = 50L;

	private static boolean dispatchingPieZero;
	private static long suppressCharUntilMs;
	private static boolean windowsKeySuppressed;

	private KeyBindsController() {
	}

	public static void resetTransientState() {
		dispatchingPieZero = false;
		suppressCharUntilMs = 0L;
	}

	public static boolean isDispatchingPieZero() {
		return dispatchingPieZero;
	}

	public static boolean allowVanillaPieChartKey(int digit) {
		return digit != 0 || ToolXConfig.get().pieZeroKey().equals(DEFAULT_PIE_ZERO_KEY);
	}

	public static void syncWindowsKeySuppression(@Nullable Minecraft minecraft) {
		boolean wantSuppress = shouldSuppressWindowsKey(minecraft);
		if (wantSuppress == windowsKeySuppressed) {
			return;
		}
		if (!wantSuppress && windowsKeySuppressed && minecraft != null && minecraft.isWindowActive() && isWinKeyHeld()) {
			return;
		}
		long windowHandle = (minecraft != null && minecraft.getWindow() != null) ? minecraft.getWindow().handle() : 0L;
		applyWindowsKeySuppression(windowHandle, wantSuppress);
	}

	public static void releaseWindowsKeySuppression(@Nullable Minecraft minecraft) {
		if (!windowsKeySuppressed) {
			return;
		}
		long windowHandle = (minecraft != null && minecraft.getWindow() != null) ? minecraft.getWindow().handle() : 0L;
		applyWindowsKeySuppression(windowHandle, false);
	}

	private static boolean shouldSuppressWindowsKey(@Nullable Minecraft minecraft) {
		if (Util.getPlatform() != Util.OS.WINDOWS
			|| minecraft == null
			|| !minecraft.isWindowActive()
			|| minecraft.getWindow() == null
			|| minecraft.gui == null
			|| minecraft.gui.overlay() != null) {
			return false;
		}
		Screen screen = minecraft.gui.screen();
		InputConstants.Key bound = ToolXConfig.get().backspaceKey();
		if (bound.getType() != InputConstants.Type.KEYBOARD
			|| (bound.getValue() != InputConstants.KEY_LGUI && bound.getValue() != InputConstants.KEY_RGUI)) {
			return false;
		}
		if (screen instanceof AbstractRecipeBookScreen<?> recipeScreen) {
			return ((AbstractRecipeBookScreenAccessor) recipeScreen).toolx$getRecipeBookComponent().isVisible();
		}
		return false;
	}

	private static boolean isWinKeyHeld() {
		return InputConstants.isKeyDown(InputConstants.KEY_LGUI) || InputConstants.isKeyDown(InputConstants.KEY_RGUI);
	}

	private static void applyWindowsKeySuppression(long windowHandle, boolean suppress) {
		if (suppress) {
			SDLHints.SDL_SetHint(SDLHints.SDL_HINT_WINDOWS_RAW_KEYBOARD, "1");
			SDLHints.SDL_SetHint(SDLHints.SDL_HINT_WINDOWS_RAW_KEYBOARD_EXCLUDE_HOTKEYS, "1");
			if (windowHandle != 0L) {
				SDLVideo.SDL_SetWindowKeyboardGrab(windowHandle, true);
			}
		} else {
			if (windowHandle != 0L) {
				SDLVideo.SDL_SetWindowKeyboardGrab(windowHandle, false);
			}
			SDLHints.SDL_SetHint(SDLHints.SDL_HINT_WINDOWS_RAW_KEYBOARD_EXCLUDE_HOTKEYS, "0");
			SDLHints.SDL_SetHint(SDLHints.SDL_HINT_WINDOWS_RAW_KEYBOARD, "0");
		}
		windowsKeySuppressed = suppress;
	}

	public static boolean handlePieZeroKey(@Nullable Minecraft minecraft, InputConstants.Key pressedKey) {
		if (minecraft == null
			|| minecraft.level == null
			|| minecraft.player == null
			|| minecraft.gui == null
			|| minecraft.gui.overlay() != null
			|| minecraft.getDebugOverlay() == null
			|| !minecraft.getDebugOverlay().showProfilerChart()
			|| PieChartController.isPieChartLocked()) {
			return false;
		}
		Screen screen = minecraft.gui.screen();
		if (screen != null && !(screen instanceof PauseScreen pause && !pause.showsPauseMenu())) {
			return false;
		}

		InputConstants.Key bound = ToolXConfig.get().pieZeroKey();
		if (bound.equals(InputConstants.UNKNOWN)
			|| bound.equals(DEFAULT_PIE_ZERO_KEY)
			|| !bound.equals(pressedKey)) {
			return false;
		}

		dispatchingPieZero = true;
		try {
			minecraft.getDebugOverlay().getProfilerPieChart().profilerPieChartKeyPress(0);
		} finally {
			dispatchingPieZero = false;
		}
		return true;
	}

	public static boolean handleScreenBackspaceKey(@Nullable Minecraft minecraft, KeyEvent event) {
		if (minecraft == null || minecraft.gui == null || minecraft.gui.overlay() != null) {
			return false;
		}
		Screen screen = minecraft.gui.screen();
		if (screen == null || screen instanceof ToolXSettingsScreen || screen instanceof KeyBindsScreen) {
			return false;
		}

		InputConstants.Key bound = ToolXConfig.get().backspaceKey();
		if (bound.equals(DEFAULT_BACKSPACE_KEY)) {
			return false;
		}

		InputConstants.Key pressed = InputConstants.getKey(event);
		if (pressed.equals(bound)) {
			int sanitizedMods = stripOwnModifier(event.modifiers(), event.key());
			KeyEvent synthetic = new KeyEvent(InputConstants.KEY_BACKSPACE, InputConstants.KEYCODE_BACKSPACE, sanitizedMods);
			screen.afterKeyboardAction();
			if (screen.keyPressed(synthetic)) {
				if (isPrintableKey(event)) {
					suppressCharUntilMs = Util.getMillis() + CHAR_SUPPRESS_WINDOW_MS;
				}
				return true;
			}
			return false;
		}

		return event.key() == InputConstants.KEY_BACKSPACE || event.shortcutKey() == InputConstants.KEYCODE_BACKSPACE;
	}

	public static boolean handleScreenBackspaceMouse(@Nullable Minecraft minecraft, MouseButtonInfo buttonInfo) {
		if (minecraft == null || minecraft.gui == null || minecraft.gui.overlay() != null) {
			return false;
		}
		Screen screen = minecraft.gui.screen();
		if (screen == null || screen instanceof ToolXSettingsScreen || screen instanceof KeyBindsScreen) {
			return false;
		}

		InputConstants.Key bound = ToolXConfig.get().backspaceKey();
		if (bound.equals(DEFAULT_BACKSPACE_KEY) || bound.equals(InputConstants.UNKNOWN)) {
			return false;
		}
		if (!InputConstants.Type.MOUSE.getOrCreate(buttonInfo.button()).equals(bound)) {
			return false;
		}

		screen.afterKeyboardAction();
		return screen.keyPressed(new KeyEvent(InputConstants.KEY_BACKSPACE, InputConstants.KEYCODE_BACKSPACE, buttonInfo.modifiers()));
	}

	public static boolean consumeSuppressedChar() {
		if (suppressCharUntilMs <= 0L) {
			return false;
		}
		boolean active = Util.getMillis() <= suppressCharUntilMs;
		suppressCharUntilMs = 0L;
		return active;
	}

	private static int stripOwnModifier(int modifiers, int key) {
		return switch (key) {
			case InputConstants.KEY_LSHIFT -> modifiers & ~MOD_LSHIFT;
			case InputConstants.KEY_RSHIFT -> modifiers & ~MOD_RSHIFT;
			case InputConstants.KEY_LCONTROL -> modifiers & ~MOD_LCTRL;
			case InputConstants.KEY_RCONTROL -> modifiers & ~MOD_RCTRL;
			case InputConstants.KEY_LALT -> modifiers & ~MOD_LALT;
			case InputConstants.KEY_RALT -> modifiers & ~MOD_RALT;
			case InputConstants.KEY_LGUI -> modifiers & ~MOD_LGUI;
			case InputConstants.KEY_RGUI -> modifiers & ~MOD_RGUI;
			default -> modifiers;
		};
	}

	private static boolean isPrintableKey(KeyEvent event) {
		int code = event.shortcutKey();
		if (code < 32 || code == 127) {
			return false;
		}
		int key = event.key();
		return key != InputConstants.KEY_LCONTROL
			&& key != InputConstants.KEY_RCONTROL
			&& key != InputConstants.KEY_LALT
			&& key != InputConstants.KEY_RALT
			&& key != InputConstants.KEY_LGUI
			&& key != InputConstants.KEY_RGUI
			&& key != InputConstants.KEY_LSHIFT
			&& key != InputConstants.KEY_RSHIFT
			&& key != InputConstants.KEY_CAPSLOCK
			&& key != InputConstants.KEY_TAB
			&& key != InputConstants.KEY_ESCAPE
			&& key != InputConstants.KEY_BACKSPACE;
	}
}
