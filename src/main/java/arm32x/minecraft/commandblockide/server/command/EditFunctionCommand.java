package arm32x.minecraft.commandblockide.server.command;

import arm32x.minecraft.commandblockide.Packets;
import arm32x.minecraft.commandblockide.server.function.FunctionIO;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Either;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.CommandFunctionArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.FunctionCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.List;
import java.util.Optional;
import static net.minecraft.command.argument.CommandFunctionArgumentType.commandFunction;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class EditFunctionCommand {
	/**
	 * Like {@link FunctionCommand#SUGGESTION_PROVIDER}, but only returns
	 * functions and not tags.
	 */
	public static final SuggestionProvider<ServerCommandSource> SUGGESTION_PROVIDER = (ctx, builder) -> {
		CommandFunctionManager functionManager = ctx.getSource().getServer().getCommandFunctionManager();
		return CommandSource.suggestIdentifiers(functionManager.getAllFunctions(), builder);
	};

	private static final SimpleCommandExceptionType EDIT_TAG_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("arguments.editfunction.tag.unsupported"));
	private static final SimpleCommandExceptionType MOD_NOT_INSTALLED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.editfunction.failed.modNotInstalled"));
	// loadFunction returns a Text on failure, so we use that.
	private static final DynamicCommandExceptionType FUNCTION_LOAD_FAILED_EXCEPTION = new DynamicCommandExceptionType(text -> (Message)text);

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("editfunction")
				.requires(source -> source.hasPermissionLevel(2))
				.then(argument("name", commandFunction())
						.suggests(SUGGESTION_PROVIDER)
						.executes(ctx -> {
							Optional<CommandFunction<ServerCommandSource>> function = CommandFunctionArgumentType.getFunctionOrTag(ctx, "name").getSecond().left();
							if (function.isPresent()) {
								return execute(ctx.getSource(), function.get());
							} else {
								throw EDIT_TAG_EXCEPTION.create();
							}
						})
				)
		);
	}

	private static int execute(ServerCommandSource source, CommandFunction<ServerCommandSource> function) throws CommandSyntaxException {
		ServerPlayerEntity player = source.getPlayer();
		if (player == null || !ServerPlayNetworking.canSend(player, Packets.EDIT_FUNCTION)) {
			throw MOD_NOT_INSTALLED_EXCEPTION.create();
		}

		Either<List<String>, Text> loadResult = FunctionIO.loadFunction(source.getServer(), function.id());
		if (loadResult.right().isPresent()) {
			throw FUNCTION_LOAD_FAILED_EXCEPTION.create(loadResult.right().get());
		}
		var lines = loadResult.left().orElseThrow(); // Should be present

		PacketByteBuf headerBuf = PacketByteBufs.create();
		headerBuf.writeIdentifier(function.id());
		headerBuf.writeVarInt(lines.size());
		ServerPlayNetworking.send(player, Packets.EDIT_FUNCTION, headerBuf);

		for (int index = 0; index < lines.size(); index++) {
			PacketByteBuf lineBuf = PacketByteBufs.create();
			lineBuf.writeVarInt(index);
			lineBuf.writeString(lines.get(index)); // TODO: Make sure this doesn’t exceed size limits.
			ServerPlayNetworking.send(player, Packets.UPDATE_FUNCTION_COMMAND, lineBuf);
		}

		return 1;
	}
}