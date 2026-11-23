package com.toolx.mixin;

import java.util.Map;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DebugScreenEntryList.class)
public interface DebugScreenEntryListAccessor {
	@Accessor("allStatuses")
	Map<Identifier, DebugScreenEntryStatus> toolx$getAllStatuses();

	@Accessor("profile")
	@Nullable DebugScreenProfile toolx$getProfile();

	@Accessor("profile")
	void toolx$setProfile(@Nullable DebugScreenProfile profile);
}
