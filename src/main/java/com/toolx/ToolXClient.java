package com.toolx;

import net.fabricmc.api.ClientModInitializer;

public class ToolXClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ToolXConfig.load();
	}
}
