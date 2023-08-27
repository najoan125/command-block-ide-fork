package arm32x.minecraft.commandblockide.client.gui;

import arm32x.minecraft.commandblockide.mixin.client.EditBoxAccessor;
import arm32x.minecraft.commandblockide.mixin.client.TextFieldWidgetAccessor;
import arm32x.minecraft.commandblockide.util.OrderedTexts;
import com.hyfata.najoan.koreanpatch.client.KoreanPatchClient;
import com.hyfata.najoan.koreanpatch.keyboard.KeyboardLayout;
import com.hyfata.najoan.koreanpatch.util.HangulProcessor;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CursorMovement;
import net.minecraft.client.render.*;
import net.minecraft.client.util.Window;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public class MultilineTextFieldWidget extends TextFieldWidget {
	private Consumer<String> field_2088;
	private final MinecraftClient client = MinecraftClient.getInstance();
	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (this.client.currentScreen != null && KoreanPatchClient.KOREAN && Character.charCount(chr) == 1) {
			char[] chars;
			for (char ch : chars = Character.toChars(chr)) {
				int qwertyIndex = this.getQwertyIndexCodePoint(ch);
				if (ch == ' ') {
					this.writeText(String.valueOf(ch));
					KeyboardLayout.INSTANCE.assemblePosition = HangulProcessor.isHangulCharacter(ch) ? this.getCursor() : -1;
					continue;
				}
				if (qwertyIndex == -1) {
					KeyboardLayout.INSTANCE.assemblePosition = -1;
					continue;
				}
				Objects.requireNonNull(KeyboardLayout.INSTANCE);
				char curr = "`1234567890-=~!@#$%^&*()_+\u3142\u3148\u3137\u3131\u3145\u315b\u3155\u3151\u3150\u3154[]\\\u3143\u3149\u3138\u3132\u3146\u315b\u3155\u3151\u3152\u3156{}|\u3141\u3134\u3147\u3139\u314e\u3157\u3153\u314f\u3163;'\u3141\u3134\u3147\u3139\u314e\u3157\u3153\u314f\u3163:\"\u314b\u314c\u314a\u314d\u3160\u315c\u3161,./\u314b\u314c\u314a\u314d\u3160\u315c\u3161<>?".toCharArray()[qwertyIndex];
				int cursorPosition = this.getCursor();
				if (cursorPosition != 0 && HangulProcessor.isHangulCharacter(curr) && this.onHangulCharTyped(chr, modifiers)) continue;
				this.writeText(String.valueOf(curr));
				KeyboardLayout.INSTANCE.assemblePosition = HangulProcessor.isHangulCharacter(curr) ? this.getCursor() : -1;
			}
			return true;
		}
		return super.charTyped(chr, modifiers);
	}

	public void writeText(String str) {
		this.write(str);
		this.sendTextChanged(str);
		//this.onChanged(this.getText());
		this.updateScreen();
	}

	private void sendTextChanged(String str) {
		if (this.field_2088 != null) {
			this.field_2088.accept(str);
		}
	}

	private void updateScreen() {
		if (this.client.currentScreen == null) {
			return;
		}
	}

	public void modifyText(char ch) {
		int cursorPosition = this.getCursor();
		this.setCursor(cursorPosition - 1);
		this.eraseCharacters(1);
		this.writeText(String.valueOf(Character.toChars(ch)));
	}

	private int getQwertyIndexCodePoint(char ch) {
		Objects.requireNonNull(KeyboardLayout.INSTANCE);
		return "`1234567890-=~!@#$%^&*()_+qwertyuiop[]\\QWERTYUIOP{}|asdfghjkl;'ASDFGHJKL:\"zxcvbnm,./ZXCVBNM<>?".indexOf(ch);
	}

	boolean onHangulCharTyped(int keyCode, int modifiers) {
		boolean shift = (modifiers & 1) == 1;
		int codePoint = keyCode;
		if (codePoint >= 65 && codePoint <= 90) {
			codePoint += 32;
		}
		if (codePoint >= 97 && codePoint <= 122 && shift) {
			codePoint -= 32;
		}
		Objects.requireNonNull(KeyboardLayout.INSTANCE);
		int idx = "`1234567890-=~!@#$%^&*()_+qwertyuiop[]\\QWERTYUIOP{}|asdfghjkl;'ASDFGHJKL:\"zxcvbnm,./ZXCVBNM<>?".indexOf(codePoint);
		if (idx == -1) {
			KeyboardLayout.INSTANCE.assemblePosition = -1;
			return false;
		}
		int cursorPosition = this.getCursor();
		String text = this.getText();
		char prev = text.toCharArray()[cursorPosition - 1];
		Objects.requireNonNull(KeyboardLayout.INSTANCE);
		char curr = "`1234567890-=~!@#$%^&*()_+\u3142\u3148\u3137\u3131\u3145\u315b\u3155\u3151\u3150\u3154[]\\\u3143\u3149\u3138\u3132\u3146\u315b\u3155\u3151\u3152\u3156{}|\u3141\u3134\u3147\u3139\u314e\u3157\u3153\u314f\u3163;'\u3141\u3134\u3147\u3139\u314e\u3157\u3153\u314f\u3163:\"\u314b\u314c\u314a\u314d\u3160\u315c\u3161,./\u314b\u314c\u314a\u314d\u3160\u315c\u3161<>?".toCharArray()[idx];
		if (cursorPosition == 0) {
			if (!HangulProcessor.isHangulCharacter(curr)) {
				return false;
			}
			this.writeText(String.valueOf(curr));
			KeyboardLayout.INSTANCE.assemblePosition = this.getCursor();
		} else if (cursorPosition == KeyboardLayout.INSTANCE.assemblePosition) {
			if (HangulProcessor.isJaeum(prev) && HangulProcessor.isMoeum(curr)) {
				Objects.requireNonNull(KeyboardLayout.INSTANCE);
				int cho = "\u3131\u3132\u3134\u3137\u3138\u3139\u3141\u3142\u3143\u3145\u3146\u3147\u3148\u3149\u314a\u314b\u314c\u314d\u314e".indexOf(prev);
				Objects.requireNonNull(KeyboardLayout.INSTANCE);
				int jung = "\u314f\u3150\u3151\u3152\u3153\u3154\u3155\u3156\u3157\u3158\u3159\u315a\u315b\u315c\u315d\u315e\u315f\u3160\u3161\u3162\u3163".indexOf(curr);
				char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, 0);
				this.modifyText(c);
				KeyboardLayout.INSTANCE.assemblePosition = this.getCursor();
				return true;
			}
			if (HangulProcessor.isHangulSyllables(prev)) {
				int code = prev - 44032;
				int cho = code / 588;
				int jung = code % 588 / 28;
				int jong = code % 588 % 28;
				if (jong == 0 && HangulProcessor.isJungsung(prev, curr)) {
					jung = HangulProcessor.getJungsung(prev, curr);
					char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, 0);
					this.modifyText(c);
					KeyboardLayout.INSTANCE.assemblePosition = this.getCursor();
					return true;
				}
				if (jong == 0 && HangulProcessor.isJongsung(curr)) {
					char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, HangulProcessor.getJongsung(curr));
					this.modifyText(c);
					KeyboardLayout.INSTANCE.assemblePosition = this.getCursor();
					return true;
				}
				if (jong != 0 && HangulProcessor.isJongsung(prev, curr)) {
					jong = HangulProcessor.getJongsung(prev, curr);
					char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, jong);
					this.modifyText(c);
					KeyboardLayout.INSTANCE.assemblePosition = this.getCursor();
					return true;
				}
				if (jong != 0 && HangulProcessor.isJungsung(curr)) {
					int newCho;
					char[] tbl = KeyboardLayout.INSTANCE.jongsung_ref_table.get(jong).toCharArray();
					if (tbl.length == 2) {
						Objects.requireNonNull(KeyboardLayout.INSTANCE);
						newCho = "\u3131\u3132\u3134\u3137\u3138\u3139\u3141\u3142\u3143\u3145\u3146\u3147\u3148\u3149\u314a\u314b\u314c\u314d\u314e".indexOf(tbl[1]);
						Objects.requireNonNull(KeyboardLayout.INSTANCE);
						jong = "\u0000\u3131\u3132\u3133\u3134\u3135\u3136\u3137\u3139\u313a\u313b\u313c\u313d\u313e\u313f\u3140\u3141\u3142\u3144\u3145\u3146\u3147\u3148\u314a\u314b\u314c\u314d\u314e".indexOf(tbl[0]);
					} else {
						Objects.requireNonNull(KeyboardLayout.INSTANCE);
						Objects.requireNonNull(KeyboardLayout.INSTANCE);
						newCho = "\u3131\u3132\u3134\u3137\u3138\u3139\u3141\u3142\u3143\u3145\u3146\u3147\u3148\u3149\u314a\u314b\u314c\u314d\u314e".indexOf("\u0000\u3131\u3132\u3133\u3134\u3135\u3136\u3137\u3139\u313a\u313b\u313c\u313d\u313e\u313f\u3140\u3141\u3142\u3144\u3145\u3146\u3147\u3148\u314a\u314b\u314c\u314d\u314e".charAt(jong));
						jong = 0;
					}
					char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, jong);
					this.modifyText(c);
					cho = newCho;
					Objects.requireNonNull(KeyboardLayout.INSTANCE);
					jung = "\u314f\u3150\u3151\u3152\u3153\u3154\u3155\u3156\u3157\u3158\u3159\u315a\u315b\u315c\u315d\u315e\u315f\u3160\u3161\u3162\u3163".indexOf(curr);
					code = HangulProcessor.synthesizeHangulCharacter(cho, jung, 0);
					this.writeText(String.valueOf(Character.toChars(code)));
					KeyboardLayout.INSTANCE.assemblePosition = this.getCursor();
					return true;
				}
			}
		}
		this.writeText(String.valueOf(curr));
		KeyboardLayout.INSTANCE.assemblePosition = this.getCursor();
		return true;
	}

	/**
	 * Allows easy and convenient access to private fields in the superclass.
	 */
	private final TextFieldWidgetAccessor self = (TextFieldWidgetAccessor)this;

    // TODO: Allow the user to configure this or to indent with tabs.
    // Note that both the text field renderer and the command processor do not
    // support tabs yet.
	private static final int INDENT_SIZE = 4;

	private final EditBox editBox;

	private boolean horizontalScrollEnabled;
	private int horizontalScroll = 0;
	private boolean verticalScrollEnabled;
	private int verticalScroll = 0;
	public static final double SCROLL_SENSITIVITY = 15.0;

	private int lineHeight = 12;

	private @Nullable Runnable cursorChangeListener = null;

	public MultilineTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text, boolean horizontalScrollEnabled, boolean verticalScrollEnabled) {
		super(textRenderer, x, y, width, height, text);
		this.horizontalScrollEnabled = horizontalScrollEnabled;
		this.verticalScrollEnabled = verticalScrollEnabled;

		// TODO: Support soft wrap.
		editBox = new EditBox(textRenderer, Integer.MAX_VALUE);
	}

	public MultilineTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
		this(textRenderer, x, y, width, height, text, true, true);
		editBox.setCursorChangeListener(() -> {
			scrollToEnsureCursorVisible();
			if (cursorChangeListener != null) {
				cursorChangeListener.run();
			}
		});
	}

	@Override
	public void setChangedListener(@Nullable Consumer<String> changedListener) {
		editBox.setChangeListener(Objects.requireNonNullElseGet(changedListener, () -> text -> {}));
	}

	public void setCursorChangeListener(@Nullable Runnable cursorChangeListener) {
		this.cursorChangeListener = cursorChangeListener;
	}

    @Override
    public void setText(String text) {
        editBox.setText(text);
    }

	@Override
	public String getText() {
        return editBox.getText();
    }

	@Override
	public String getSelectedText() {
        return editBox.getSelectedText();
    }

    @Override
    public void setTextPredicate(Predicate<String> textPredicate) {
        throw new UnsupportedOperationException();
    }

	@Override
	public void write(String text) {
        editBox.replaceSelection(text);
	}

    @Override
    public void eraseWords(int wordOffset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void eraseCharacters(int characterOffset) {
        editBox.delete(characterOffset);
    }

    @Override
    public int getWordSkipPosition(int wordOffset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveCursor(int offset) {
        editBox.moveCursor(CursorMovement.RELATIVE, offset);
    }

    private void moveCursor(double mouseX, double mouseY) {
        double virtualX = mouseX - getInnerX() + getHorizontalScroll();
        double virtualY = mouseY - getInnerY() + getVerticalScroll();

		int lineIndex = MathHelper.floor(virtualY / getLineHeight());

		// Get a rough estimate of where the cursor should be.
		EditBox.Substring lineSubstring = editBox.getLine(lineIndex);
		String line = getText().substring(lineSubstring.beginIndex(), lineSubstring.endIndex());
		int charIndexInLine = self.getTextRenderer().trimToWidth(line, MathHelper.floor(virtualX)).length();
		int charIndex = lineSubstring.beginIndex() + charIndexInLine;

		// Refine the estimate by determining the nearest character boundary.
		double leftCharacterXDistance = Math.abs(getCharacterVirtualX(charIndex) - virtualX);
		double rightCharacterXDistance = Math.abs(getCharacterVirtualX(charIndex + 1) - virtualX);
		if (rightCharacterXDistance < leftCharacterXDistance) {
			charIndex++;
		}

		setCursor(charIndex);
    }

    @Override
    public void setCursor(int cursor) {
        editBox.moveCursor(CursorMovement.ABSOLUTE, cursor);
    }

    @Override
    public void setSelectionStart(int cursor) {
        editBox.setSelecting(true);
        setCursor(cursor);
    }

	@Override
	public void setSelectionEnd(int index) {
		((EditBoxAccessor)editBox).setSelectionEnd(index);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		//KoreanPatch
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen != null) {
			if (keyCode == KoreanPatchClient.KEYCODE || scanCode == KoreanPatchClient.SCANCODE) {
				boolean bl = KoreanPatchClient.KOREAN = !KoreanPatchClient.KOREAN;
			}
			if (keyCode == 259) {
				int cursorPosition = this.getCursor();
				if (cursorPosition == 0 || cursorPosition != KeyboardLayout.INSTANCE.assemblePosition) {
                    return editBox.handleSpecialKey(keyCode);
                }
				String text = this.getText();
				char ch = text.toCharArray()[cursorPosition - 1];
				if (HangulProcessor.isHangulSyllables(ch)) {
					int code = ch - 44032;
					int cho = code / 588;
					int jung = code % 588 / 28;
					int jong = code % 588 % 28;
					if (jong != 0) {
						char[] ch_arr = KeyboardLayout.INSTANCE.jongsung_ref_table.get(jong).toCharArray();
						if (ch_arr.length == 2) {
							Objects.requireNonNull(KeyboardLayout.INSTANCE);
							jong = "\u0000\u3131\u3132\u3133\u3134\u3135\u3136\u3137\u3139\u313a\u313b\u313c\u313d\u313e\u313f\u3140\u3141\u3142\u3144\u3145\u3146\u3147\u3148\u314a\u314b\u314c\u314d\u314e".indexOf(ch_arr[0]);
						} else {
							jong = 0;
						}
						char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, jong);
						this.modifyText(c);
					} else {
						char[] ch_arr = KeyboardLayout.INSTANCE.jungsung_ref_table.get(jung).toCharArray();
						if (ch_arr.length == 2) {
							Objects.requireNonNull(KeyboardLayout.INSTANCE);
							jung = "\u314f\u3150\u3151\u3152\u3153\u3154\u3155\u3156\u3157\u3158\u3159\u315a\u315b\u315c\u315d\u315e\u315f\u3160\u3161\u3162\u3163".indexOf(ch_arr[0]);
							char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, 0);
							this.modifyText(c);
						} else {
							Objects.requireNonNull(KeyboardLayout.INSTANCE);
							char c = "\u3131\u3132\u3134\u3137\u3138\u3139\u3141\u3142\u3143\u3145\u3146\u3147\u3148\u3149\u314a\u314b\u314c\u314d\u314e".charAt(cho);
							this.modifyText(c);
						}
					}
                    return true;
                } else if (HangulProcessor.isHangulCharacter(ch)) {
					KeyboardLayout.INSTANCE.assemblePosition = -1;
				}
			}
		}


		if (keyCode == GLFW.GLFW_KEY_TAB) {
            if (editBox.hasSelection()) {
                logger.warn("Indenting selected lines is not yet supported");
            } else {
                int cursorLeft = getCursor() - getLineStartBefore(getCursor());
                String indent = " ".repeat(4 - cursorLeft % INDENT_SIZE);
                editBox.replaceSelection(indent);
            }
            return true;
        } else {
			return editBox.handleSpecialKey(keyCode);
		}
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isVisible()) {
            return false;
        }
        if (self.isFocusUnlocked()) {
            setFocused(isMouseOver(mouseX, mouseY));
        }
        if (isFocused() && isMouseOver(mouseX, mouseY) && button == 0) {
            editBox.setSelecting(Screen.hasShiftDown());
            moveCursor(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!this.isVisible()) {
            return false;
        }
        if (self.isFocusUnlocked()) {
            setFocused(isMouseOver(mouseX, mouseY));
        }
        if (isFocused() && isMouseOver(mouseX, mouseY) && button == 0) {
            editBox.setSelecting(true);
            moveCursor(mouseX, mouseY);
            editBox.setSelecting(Screen.hasShiftDown());
            return true;
        }
        return false;
    }

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (this.isMouseOver(mouseX, mouseY)) {
			boolean changed = false;
			if (Screen.hasShiftDown() && isHorizontalScrollEnabled()) {
				changed = setHorizontalScroll(getHorizontalScroll() - (int)Math.round(amount * SCROLL_SENSITIVITY));
			} else if (isVerticalScrollEnabled()) {
				changed = setVerticalScroll(getVerticalScroll() - (int)Math.round(amount * SCROLL_SENSITIVITY));
			}
			// This updates the position of the suggestions window.
			if (cursorChangeListener != null) {
				cursorChangeListener.run();
			}
			return changed;
		} else {
			return super.mouseScrolled(mouseX, mouseY, amount);
		}
	}

	@Override
	public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
		if (!isVisible()) {
			return;
		}

		if (self.getDrawsBackground()) {
			int borderColor = this.isFocused() ? 0xFFFFFFFF : 0xFFA0A0A0;
			context.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, borderColor);
			context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF000000);
		}

		Window window = MinecraftClient.getInstance().getWindow();
		double scaleFactor = window.getScaleFactor();
		RenderSystem.enableScissor(
			(int)Math.round(this.getX() * scaleFactor),
			// OpenGL coordinates start from the bottom left.
			window.getHeight() - (int)Math.round(this.getY() * scaleFactor + this.height * scaleFactor),
			(int)Math.round(this.width * scaleFactor),
			(int)Math.round(this.height * scaleFactor)
		);

		int textColor = self.isEditable() ? self.getEditableColor() : self.getUneditableColor();
		int x = this.getX() + (self.getDrawsBackground() ? 4 : 0) - horizontalScroll;
		int y = this.getY() + (self.getDrawsBackground() ? 3 : 0) - verticalScroll;

		boolean showCursor = isFocused() && self.getFocusedTicks() / 6 % 2 == 0;
		boolean lineCursor = getCursor() < getText().length() || getText().length() >= self.invokeGetMaxLength();

		int cursorLine = getCurrentLineIndex();
		int cursorX = x - 1;
		int cursorY = y + lineHeight * cursorLine;

		OrderedText text = self.getRenderTextProvider().apply(getText(), 0);
		List<OrderedText> lines = OrderedTexts.split('\n', text);
		for (int index = 0; index < lines.size(); index++) {
			OrderedText line = lines.get(index);
			if (index == cursorLine) {
				int indexOfLastNewlineBeforeCursor = getLineStartBefore(getCursor()) - 1;
				int codePointsBeforeCursor;
				if (indexOfLastNewlineBeforeCursor != -1) {
					codePointsBeforeCursor = getText().codePointCount(indexOfLastNewlineBeforeCursor, Math.max(getCursor() - 1, 0));
				} else {
					codePointsBeforeCursor = getText().codePointCount(0, getCursor());
				}
				int endX = context.drawTextWithShadow(self.getTextRenderer(), OrderedTexts.limit(codePointsBeforeCursor, line), x, y + lineHeight * index, textColor) - 1;
				context.drawTextWithShadow(self.getTextRenderer(), OrderedTexts.skip(codePointsBeforeCursor, line), endX, y + lineHeight * index, textColor);
				cursorX = endX - 1;
			} else {
				context.drawTextWithShadow(self.getTextRenderer(), line, x, y + lineHeight * index, textColor);
			}
		}

		if (showCursor) {
			if (lineCursor) {
				context.fill(cursorX, cursorY - 1, cursorX + 1, cursorY + 10, 0xFFD0D0D0);
			} else {
				context.drawTextWithShadow(self.getTextRenderer(), "_", cursorX + 1, cursorY, textColor);
			}
		}

		if (isFocused() && editBox.hasSelection()) {
			renderSelection(context, x, y);
		}

		RenderSystem.disableScissor();
	}

	private void renderSelection(DrawContext context, int x, int y) {
		var selection = editBox.getSelection();
		int normalizedSelectionStart = selection.beginIndex();
		int normalizedSelectionEnd = selection.endIndex();

		int startX = x + self.getTextRenderer().getWidth(getText().substring(getLineStartBefore(normalizedSelectionStart), normalizedSelectionStart)) - 1;
		int startY = y + lineHeight * getLineIndex(normalizedSelectionStart) - 1;
		int endX = x + self.getTextRenderer().getWidth(getText().substring(getLineStartBefore(normalizedSelectionEnd), normalizedSelectionEnd)) - 1;
		int endY = y + lineHeight * getLineIndex(normalizedSelectionEnd) - 1;

		int leftEdge = this.getX() + (self.getDrawsBackground() ? 4 : 0);
		int rightEdge = leftEdge + this.getInnerWidth();

		Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		RenderSystem.setShaderColor(0.0F, 0.0F, 1.0F, 1.0F);
		RenderSystem.enableColorLogicOp();
		RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

		if (startY == endY) {
			// Selection spans one line
			bufferBuilder.vertex(matrix, endX, startY, 0.0f).next();
			bufferBuilder.vertex(matrix, startX, startY, 0.0f).next();
			bufferBuilder.vertex(matrix, startX, endY + lineHeight - 1, 0.0f).next();
			bufferBuilder.vertex(matrix, endX, endY + lineHeight - 1, 0.0f).next();
		} else {
			// Selection spans two or more lines
			bufferBuilder.vertex(matrix, rightEdge, startY, 0.0f).next();
			bufferBuilder.vertex(matrix, startX, startY, 0.0f).next();
			bufferBuilder.vertex(matrix, startX, startY + lineHeight, 0.0f).next();
			bufferBuilder.vertex(matrix, rightEdge, startY + lineHeight, 0.0f).next();

			if (!(startY - lineHeight == endY || endY - lineHeight == startY)) {
				// Selection spans three or more lines
				bufferBuilder.vertex(matrix, rightEdge, startY + lineHeight, 0.0f).next();
				bufferBuilder.vertex(matrix, leftEdge, startY + lineHeight, 0.0f).next();
				bufferBuilder.vertex(matrix, leftEdge, endY, 0.0f).next();
				bufferBuilder.vertex(matrix, rightEdge, endY, 0.0f).next();
			}

			bufferBuilder.vertex(matrix, endX, endY, 0.0f).next();
			bufferBuilder.vertex(matrix, leftEdge, endY, 0.0f).next();
			bufferBuilder.vertex(matrix, leftEdge, endY + lineHeight - 1, 0.0f).next();
			bufferBuilder.vertex(matrix, endX, endY + lineHeight - 1, 0.0f).next();
		}

		tessellator.draw();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableColorLogicOp();
	}

    @Override
    public void setMaxLength(int maxLength) {
        editBox.setMaxLength(maxLength);
    }

    @Override
    public int getCursor() {
        return editBox.getCursor();
    }

	public int getLineCount() {
        return editBox.getLineCount();
	}

	public int getCurrentLineIndex() {
		return getLineIndex(getCursor());
	}

	private int getLineIndex(int charIndex) {
		return (int)getText()
			.substring(0, charIndex)
			.codePoints()
			.filter(point -> point == '\n')
			.count();
	}

	private int getLineStartBefore(int charIndex) {
		return getText().lastIndexOf('\n', Math.max(charIndex, 0) - 1) + 1;
	}

    // Naming things is hard.
    public boolean isBeforeFirstNonWhitespaceCharacterInLine(int charIndex) {
        return getText()
            .substring(getLineStartBefore(charIndex), charIndex)
            .chars()
            .allMatch(Character::isWhitespace);
    }

	public String getLine(int lineIndex) {
		var line = editBox.getLine(lineIndex);
		return getText().substring(line.beginIndex(), line.endIndex());
	}

	protected int getHorizontalScroll() {
		return horizontalScroll;
	}

	protected int getMaxHorizontalScroll() {
		return Math.max(0, Arrays.stream(getText().split("\n"))
			.mapToInt(self.getTextRenderer()::getWidth)
			.max()
			.orElse(0) + 8 - width);
	}

	protected boolean setHorizontalScroll(int horizontalScroll) {
		int previous = this.horizontalScroll;
		this.horizontalScroll = MathHelper.clamp(horizontalScroll, 0, getMaxHorizontalScroll());
		return this.horizontalScroll != previous;
	}

	protected int getVerticalScroll() {
		return verticalScroll;
	}

	protected int getMaxVerticalScroll() {
		return Math.max(0, getLineCount() * getLineHeight() + 2 - height);
	}

	protected boolean setVerticalScroll(int verticalScroll) {
		int previous = this.verticalScroll;
		this.verticalScroll = MathHelper.clamp(verticalScroll, 0, getMaxVerticalScroll());
		return this.verticalScroll != previous;
	}

	public boolean isHorizontalScrollEnabled() {
		return horizontalScrollEnabled;
	}

	public void setHorizontalScrollEnabled(boolean enabled) {
		horizontalScrollEnabled = enabled;
		horizontalScroll = 0;
	}

	public boolean isVerticalScrollEnabled() {
		return verticalScrollEnabled;
	}

	public void setVerticalScrollEnabled(boolean enabled) {
		verticalScrollEnabled = enabled;
		verticalScroll = 0;
	}

	protected void scrollToEnsureCursorVisible() {
		int virtualX = getCharacterVirtualX(getCursor());
		int virtualY = getCharacterVirtualY(getCursor());

		setHorizontalScroll(MathHelper.clamp(horizontalScroll, virtualX - getInnerWidth(), virtualX));
		setVerticalScroll(MathHelper.clamp(verticalScroll, virtualY - getInnerHeight(), virtualY));
	}

	public int getLineHeight() {
		return lineHeight;
	}

	public void setLineHeight(int lineHeight) {
		this.lineHeight = lineHeight;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getCharacterVirtualX(int charIndex) {
		if (charIndex > getText().length()) {
			return 0;
		}
		String line = getLine(getLineIndex(charIndex));

		int indexInLine = charIndex - getLineStartBefore(charIndex);
		if (indexInLine > line.length()) {
			indexInLine = line.length();
		}

		return self.getTextRenderer().getWidth(line.substring(0, indexInLine));
	}

	public int getCharacterRealX(int charIndex) {
		return getInnerX() - horizontalScroll + getCharacterVirtualX(charIndex);
	}

	/**
	 * Gets the desired X position of the {@link ChatInputSuggestor} window.
	 *
	 * <p>This function is marked as deprecated because it <i>does not do what
	 * the method name says</i> and is only here to be called by
	 * {@code ChatInputSuggestor}.</p>
	 *
	 * @param charIndex The index of the character to place the suggestion
	 *                  window at.
	 * @return The desired X position of the suggestion window.
	 */
	@Deprecated
	@Override
	public int getCharacterX(int charIndex) {
		// Since getInnerX isn't a method in the original TextFieldWidget,
		// ChatInputSuggestor calls getCharacterX(0) instead.
		if (charIndex == 0) {
			return getInnerX();
		}
		// Enforce a lower bound on position. ChatInputSuggestor will enforce
		// the upper bound using getInnerWidth().
		return Math.max(getCharacterRealX(charIndex), getInnerX());
	}

	public int getCharacterVirtualY(int charIndex) {
		if (charIndex > getText().length()) {
			charIndex = getText().length();
		}
		int lineIndex = getLineIndex(charIndex);

		return lineIndex * getLineHeight();
	}

	public int getCharacterRealY(int charIndex) {
		return getInnerY() - verticalScroll + getCharacterVirtualY(charIndex);
	}

	private int getInnerX() {
		return this.getX() + (self.getDrawsBackground() ? 4 : 0);
	}

	private int getInnerY() {
		return this.getY() + (self.getDrawsBackground() ? 3 : 0);
	}

	private int getInnerHeight() {
		return self.getDrawsBackground() ? this.height - 6 : this.height;
	}

    private static final Logger logger = LogManager.getLogger();
}
