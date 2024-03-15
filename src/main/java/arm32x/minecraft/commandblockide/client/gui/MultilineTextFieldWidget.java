package arm32x.minecraft.commandblockide.client.gui;

import arm32x.minecraft.commandblockide.mixin.client.EditBoxAccessor;
import arm32x.minecraft.commandblockide.mixin.client.TextFieldWidgetAccessor;
import arm32x.minecraft.commandblockide.util.OrderedTexts;
import com.hyfata.najoan.koreanpatch.client.KoreanPatchClient;
import com.hyfata.najoan.koreanpatch.keyboard.KeyboardLayout;
import com.hyfata.najoan.koreanpatch.util.HangulProcessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.JigsawBlockScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CursorMovement;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public class MultilineTextFieldWidget extends TextFieldWidget {
	private Consumer<String> changedListener;
	private final MinecraftClient client = MinecraftClient.getInstance();

	public void writeText(String str) {
		this.write(str);
		this.sendTextChanged(str);
	}

	private void sendTextChanged(String str) {
		if (this.changedListener != null) {
			this.changedListener.accept(str);
		}
	}

	public void modifyText(char ch) {
		int cursorPosition = this.getCursor();
		this.setCursor(cursorPosition - 1, false);
		this.eraseCharacters(1);
		this.writeText(String.valueOf(Character.toChars(ch)));
	}

	boolean onBackspaceKeyPressed() {
		int cursorPosition = this.getCursor();
		if (cursorPosition == 0 || cursorPosition != KeyboardLayout.INSTANCE.assemblePosition) return false;

		String text = this.getText();

		char ch = text.toCharArray()[cursorPosition - 1];

		if (HangulProcessor.isHangulSyllables(ch)) {
			int code = ch - 0xAC00;
			int cho = code / (21 * 28);
			int jung = (code % (21 * 28)) / 28;
			int jong = (code % (21 * 28)) % 28;

			if (jong != 0) {
				char[] ch_arr = KeyboardLayout.INSTANCE.jongsung_ref_table.get(jong).toCharArray();
				if (ch_arr.length == 2) {
					jong = KeyboardLayout.INSTANCE.jongsung_table.indexOf(ch_arr[0]);
				} else {
					jong = 0;
				}
				char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, jong);
				this.modifyText(c);
				return true;
			} else {
				char[] ch_arr = KeyboardLayout.INSTANCE.jungsung_ref_table.get(jung).toCharArray();
				if (ch_arr.length == 2) {
					jung = KeyboardLayout.INSTANCE.jungsung_table.indexOf(ch_arr[0]);
					char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, 0);
					this.modifyText(c);
					return true;
				} else {
					char c = KeyboardLayout.INSTANCE.chosung_table.charAt(cho);
					this.modifyText(c);
					return true;
				}
			}
		} else if (HangulProcessor.isHangulCharacter(ch)) {
			KeyboardLayout.INSTANCE.assemblePosition = -1;
			return false;
		}
		return false;
	}

	boolean onHangulCharTyped(int keyCode, int modifiers) {
		boolean shift = (modifiers & 0x01) == 1;

		int codePoint = keyCode;

		if (codePoint >= 65 && codePoint <= 90) {
			codePoint += 32;
		}

		if (codePoint >= 97 && codePoint <= 122) {
			if (shift) {
				codePoint -= 32;
			}
		}

		int idx = "`1234567890-=~!@#$%^&*()_+qwertyuiop[]\\QWERTYUIOP{}|asdfghjkl;'ASDFGHJKL:\"zxcvbnm,./ZXCVBNM<>?".indexOf(codePoint);
		// System.out.println(String.format("idx: %d", idx));
		if (idx == -1) {
			KeyboardLayout.INSTANCE.assemblePosition = -1;
			return false;
		}

		int cursorPosition = this.getCursor();
		String text = this.getText();

		char prev = text.toCharArray()[cursorPosition - 1];
		char curr = KeyboardLayout.INSTANCE.layout.toCharArray()[idx];

		if (cursorPosition == 0) {
			if (!HangulProcessor.isHangulCharacter(curr)) return false;

			this.writeText(String.valueOf(curr));
			KeyboardLayout.INSTANCE.assemblePosition = this.getCursor();
		}
		else if (cursorPosition == KeyboardLayout.INSTANCE.assemblePosition) {

			// 자음 + 모음
			if (HangulProcessor.isJaeum(prev) && HangulProcessor.isMoeum(curr)) {
				int cho = KeyboardLayout.INSTANCE.chosung_table.indexOf(prev);
				int jung = KeyboardLayout.INSTANCE.jungsung_table.indexOf(curr);
				char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, 0);
				this.modifyText(c);
				KeyboardLayout.INSTANCE.assemblePosition = this.getCursor();
				return true;
			}

			if (HangulProcessor.isHangulSyllables(prev)) {
				int code = prev - 0xAC00;
				int cho = code / (21 * 28);
				int jung = (code % (21 * 28)) / 28;
				int jong = (code % (21 * 28)) % 28;

				// 중성 합성 (ㅘ, ㅙ)..
				if (jong == 0 && HangulProcessor.isJungsung(prev, curr)) {
					jung = HangulProcessor.getJungsung(prev, curr);
					char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, 0);
					this.modifyText(c);
					KeyboardLayout.INSTANCE.assemblePosition = this.getCursor();
					return true;
				}

				// 종성 추가
				if (jong == 0 && HangulProcessor.isJongsung(curr)) {
					char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, HangulProcessor.getJongsung(curr));
					this.modifyText(c);
					KeyboardLayout.INSTANCE.assemblePosition = this.getCursor();
					return true;
				}

				// 종성 받침 추가
				if (jong != 0 && HangulProcessor.isJongsung(prev, curr)) {
					jong = HangulProcessor.getJongsung(prev, curr);
					char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, jong);
					this.modifyText(c);
					KeyboardLayout.INSTANCE.assemblePosition = this.getCursor();
					return true;
				}

				// 종성에서 받침 하나 빼고 글자 만들기
				if (jong != 0 && HangulProcessor.isJungsung(curr)) {
					char[] tbl = KeyboardLayout.INSTANCE.jongsung_ref_table.get(jong).toCharArray();
					int newCho = 0;
					if (tbl.length == 2) {
						newCho = KeyboardLayout.INSTANCE.chosung_table.indexOf(tbl[1]);
						jong = KeyboardLayout.INSTANCE.jongsung_table.indexOf(tbl[0]);
					} else {
						newCho = KeyboardLayout.INSTANCE.chosung_table.indexOf(KeyboardLayout.INSTANCE.jongsung_table.charAt(jong));
						jong = 0;
					}

					char c = HangulProcessor.synthesizeHangulCharacter(cho, jung, jong);
					this.modifyText(c);

					cho = newCho;
					jung = KeyboardLayout.INSTANCE.jungsung_table.indexOf(curr);
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

	public void typedTextField(char chr, int modifiers) {
		int qwertyIndex = "`1234567890-=~!@#$%^&*()_+qwertyuiop[]\\QWERTYUIOP{}|asdfghjkl;'ASDFGHJKL:\"zxcvbnm,./ZXCVBNM<>?".indexOf(chr);
		if (chr == ' ') {
			this.writeText(String.valueOf(chr));
			KeyboardLayout.INSTANCE.assemblePosition = HangulProcessor.isHangulCharacter(chr) ? this.getCursor() : -1;
			return;
		}
		if (qwertyIndex == -1) {
			KeyboardLayout.INSTANCE.assemblePosition = -1;
			return;
		}

		char curr = KeyboardLayout.INSTANCE.layout.toCharArray()[qwertyIndex];
		if (this.getCursor() == 0 || !HangulProcessor.isHangulCharacter(curr) || !onHangulCharTyped(chr, modifiers)) {
			//Caps Lock/한글 상태면 쌍자음으로 입력되는 문제 수정
			if (HangulProcessor.isHangulCharacter(curr)) {
				boolean shift = (modifiers & 0x01) == 1;
				int codePoint = chr;

				if (codePoint >= 65 && codePoint <= 90) {
					codePoint += 32;
				}

				if (codePoint >= 97 && codePoint <= 122) {
					if (shift) {
						codePoint -= 32;
					}
				}
				int idx = "`1234567890-=~!@#$%^&*()_+qwertyuiop[]\\QWERTYUIOP{}|asdfghjkl;'ASDFGHJKL:\"zxcvbnm,./ZXCVBNM<>?".indexOf(codePoint);
				if (idx != -1) {
					curr = KeyboardLayout.INSTANCE.layout.toCharArray()[idx];
				}
			} //모두 소문자로 돌린 다음 shift modifier에 따라서 적절하게 처리

			this.writeText(String.valueOf(curr));
			KeyboardLayout.INSTANCE.assemblePosition = HangulProcessor.isHangulCharacter((curr)) ? this.getCursor() : -1;
		}
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (this.client.currentScreen != null &&
				!(this.client.currentScreen instanceof JigsawBlockScreen) &&
				!(this.client.currentScreen instanceof StructureBlockScreen) &&
				KoreanPatchClient.KOREAN && Character.charCount(chr) == 1)
		{
			typedTextField(chr, modifiers);
			return true;
		}
		return super.charTyped(chr, modifiers);
	}
	/**
	 * Allows easy and convenient access to private fields in the superclass.
	 */
	private final TextFieldWidgetAccessor self = (TextFieldWidgetAccessor)this;

    // TODO: Allow the user to configure this or to indent with tabs.
    // Note that both the text field renderer and the command processor do not
    // support tabs yet.
	private static final int INDENT_SIZE = 4;

	// The amount of time the cursor will spend being either visible or
	// invisible before switching to the other state.
	private static final long CURSOR_BLINK_INTERVAL_MS = 300;

	private final EditBox editBox;

	private boolean horizontalScrollEnabled;
	private int horizontalScroll = 0;
	private boolean verticalScrollEnabled;
	private int verticalScroll = 0;
	public static final double SCROLL_SENSITIVITY = 15.0;

	private int lineHeight = 12;
	private SyntaxHighlighter syntaxHighlighter = SyntaxHighlighter.NONE;

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
	@Deprecated
	public void setRenderTextProvider(BiFunction<String, Integer, OrderedText> renderTextProvider) {
		// Do nothing. I would love to throw an UnsupportedOperationException,
		// but this is called by ChatInputSuggestor.
	}

	public SyntaxHighlighter getSyntaxHighlighter() {
		return syntaxHighlighter;
	}

	public void setSyntaxHighlighter(SyntaxHighlighter syntaxHighlighter) {
		this.syntaxHighlighter = syntaxHighlighter;
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
	public void moveCursor(int offset, boolean hasShiftDown) {
		editBox.setSelecting(hasShiftDown);
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

		setCursor(charIndex, Screen.hasShiftDown());
    }

    @Override
	public void setCursor(int cursor, boolean hasShiftDown) {
		editBox.setSelecting(hasShiftDown);
		editBox.moveCursor(CursorMovement.ABSOLUTE, cursor);
	}

    @Override
    public void setSelectionStart(int cursor) {
		setCursor(cursor, true);
    }

	@Override
	public void setSelectionEnd(int index) {
		((EditBoxAccessor)editBox).setSelectionEnd(index);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		//KoreanPatch
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen != null &&
				!(client.currentScreen instanceof JigsawBlockScreen) &&
				!(client.currentScreen instanceof StructureBlockScreen))
		{
			if (keyCode == KoreanPatchClient.KEYCODE || scanCode == KoreanPatchClient.SCANCODE) {
				KoreanPatchClient.KOREAN = !KoreanPatchClient.KOREAN;
			}
			if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
				if (onBackspaceKeyPressed()) {
					return true;
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
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (this.isMouseOver(mouseX, mouseY)) {
			horizontalAmount = Screen.hasShiftDown() ? verticalAmount : horizontalAmount;
			verticalAmount = Screen.hasShiftDown() ? 0 : verticalAmount;

			boolean changed = setHorizontalScroll(getHorizontalScroll() - (int)Math.round(horizontalAmount * SCROLL_SENSITIVITY));
			changed = changed || setVerticalScroll(getVerticalScroll() - (int)Math.round(verticalAmount * SCROLL_SENSITIVITY));

			// This updates the position of the suggestions window.
			if (cursorChangeListener != null) {
				cursorChangeListener.run();
			}
			return changed;
		} else {
			return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}
	}

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		if (!isVisible()) {
			return;
		}

		if (drawsBackground()) {
			var textureId = TextFieldWidgetAccessor.getTextures().get(isNarratable(), isFocused());
			context.drawGuiTexture(textureId, getX(), getY(), getWidth(), getHeight());
		}

		context.enableScissor(
				this.getX() + 1,
				this.getY() + 1,
				this.getX() + this.getWidth() - 1,
				this.getY() + this.getHeight() - 1
		);

		int textColor = self.invokeIsEditable() ? self.getEditableColor() : self.getUneditableColor();
		int x = getInnerX() - horizontalScroll;
		int y = getInnerY() - verticalScroll;

		long timeSinceLastSwitchFocusMs = Util.getMeasuringTimeMs() - self.getLastSwitchFocusTime();
		boolean showCursor = isFocused() && timeSinceLastSwitchFocusMs / CURSOR_BLINK_INTERVAL_MS % 2 == 0;
		boolean lineCursor = getCursor() < getText().length() || getText().length() >= self.invokeGetMaxLength();

		int cursorLine = getCurrentLineIndex();
		int cursorX = x;
		int cursorY = y + lineHeight * cursorLine;

		// This assumes that the highlighter returns the same characters as the
		// original text, which is not enforced by the API.
		List<OrderedText> lines = getSyntaxHighlighter().highlight(getText());
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
				context.fill(RenderLayer.getGuiOverlay(), cursorX, cursorY - 1, cursorX + 1, cursorY + 10, 0xFFD0D0D0);
			} else {
				context.drawTextWithShadow(self.getTextRenderer(), "_", cursorX + 1, cursorY, textColor);
			}
		}

		if (isFocused() && editBox.hasSelection()) {
			renderSelection(context, x, y);
		}

		context.disableScissor();
	}

	private void renderSelection(DrawContext context, int x, int y) {
		var selection = editBox.getSelection();
		int normalizedSelectionStart = selection.beginIndex();
		int normalizedSelectionEnd = selection.endIndex();

		int startX = x + self.getTextRenderer().getWidth(getText().substring(getLineStartBefore(normalizedSelectionStart), normalizedSelectionStart)) - 1;
		int startY = y + lineHeight * getLineIndex(normalizedSelectionStart) - 1;
		int endX = x + self.getTextRenderer().getWidth(getText().substring(getLineStartBefore(normalizedSelectionEnd), normalizedSelectionEnd)) - 1;
		int endY = y + lineHeight * getLineIndex(normalizedSelectionEnd) - 1;

		int leftEdge = getInnerX();
		int rightEdge = leftEdge + this.getInnerWidth();

		Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
		VertexConsumer vertexConsumer = context.getVertexConsumers().getBuffer(RenderLayer.getGuiTextHighlight());

		float r = 0.0f, g = 0.0f, b = 1.0f, a = 1.0f;

		if (startY == endY) {
			// Selection spans one line
			vertexConsumer.vertex(matrix, endX, startY, 0.0f).color(r, g, b, a).next();
			vertexConsumer.vertex(matrix, startX, startY, 0.0f).color(r, g, b, a).next();
			vertexConsumer.vertex(matrix, startX, endY + lineHeight - 1, 0.0f).color(r, g, b, a).next();
			vertexConsumer.vertex(matrix, endX, endY + lineHeight - 1, 0.0f).color(r, g, b, a).next();
		} else {
			// Selection spans two or more lines
			vertexConsumer.vertex(matrix, rightEdge, startY, 0.0f).color(r, g, b, a).next();
			vertexConsumer.vertex(matrix, startX, startY, 0.0f).color(r, g, b, a).next();
			vertexConsumer.vertex(matrix, startX, startY + lineHeight, 0.0f).color(r, g, b, a).next();
			vertexConsumer.vertex(matrix, rightEdge, startY + lineHeight, 0.0f).color(r, g, b, a).next();

			if (!(startY - lineHeight == endY || endY - lineHeight == startY)) {
				// Selection spans three or more lines
				vertexConsumer.vertex(matrix, rightEdge, startY + lineHeight, 0.0f).color(r, g, b, a).next();
				vertexConsumer.vertex(matrix, leftEdge, startY + lineHeight, 0.0f).color(r, g, b, a).next();
				vertexConsumer.vertex(matrix, leftEdge, endY, 0.0f).color(r, g, b, a).next();
				vertexConsumer.vertex(matrix, rightEdge, endY, 0.0f).color(r, g, b, a).next();
			}

			vertexConsumer.vertex(matrix, endX, endY, 0.0f).color(r, g, b, a).next();
			vertexConsumer.vertex(matrix, leftEdge, endY, 0.0f).color(r, g, b, a).next();
			vertexConsumer.vertex(matrix, leftEdge, endY + lineHeight - 1, 0.0f).color(r, g, b, a).next();
			vertexConsumer.vertex(matrix, endX, endY + lineHeight - 1, 0.0f).color(r, g, b, a).next();
		}

		context.draw();
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
		return this.getX() + (drawsBackground() ? 4 : 0);
	}

	private int getInnerY() {
		return this.getY() + (drawsBackground() ? 4 : 0);
	}

	private int getInnerHeight() {
		return drawsBackground() ? this.height - 6 : this.height;
	}

    private static final Logger logger = LogManager.getLogger();

	@FunctionalInterface
	public interface SyntaxHighlighter {
		/**
		 * A syntax highlighter that performs no highlighting.
		 */
		SyntaxHighlighter NONE = text -> Arrays.stream(text.split("\n"))
				.map(line -> OrderedText.styledForwardsVisitedString(line, Style.EMPTY))
				.toList();

		List<OrderedText> highlight(String text);
	}
}
