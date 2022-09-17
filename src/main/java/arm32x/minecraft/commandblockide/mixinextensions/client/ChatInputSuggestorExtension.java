package arm32x.minecraft.commandblockide.mixinextensions.client;

import arm32x.minecraft.commandblockide.client.processor.CommandProcessor;
import org.jetbrains.annotations.Nullable;

public interface ChatInputSuggestorExtension {
	void ide$setAllowComments(boolean allowComments);
	void ide$setSlashForbidden(boolean slashForbidden);
	@Nullable CommandProcessor ide$getCommandProcessor();
	void ide$setCommandProcessor(@Nullable CommandProcessor processor);
}