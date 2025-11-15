/*
 * VoidQueue, a high-performance velocity queueing solution
 *
 * Copyright (c) 2025 Harrison Boyd
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.hboyd.voidQueue.translation;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.AbstractTranslationStore;
import net.kyori.examination.Examiner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.NodePath;

import java.text.MessageFormat;
import java.util.Locale;

public class NodeTranslationStore extends AbstractTranslationStore.StringBased<String> {

    @Override
    public @NotNull String examinableName() {
        return super.examinableName();
    }

    @Override
    public <R> @NotNull R examine(@NotNull Examiner<R> examiner) {
        return super.examine(examiner);
    }

    @Override
    public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
        return null;
    }

    @Override
    public @Nullable Component translate(@NotNull TranslatableComponent component, @NotNull Locale locale) {
        return super.translate(component, locale);
    }


    /**
     * Creates a new abstract, string-based translation store with a given name.
     *
     * @param name the name
     * @since 4.20.0
     */
    protected NodeTranslationStore(@NotNull Key name) {
        super(name);
    }

    @Override
    protected @NotNull String parse(@NotNull String string, @NotNull Locale locale) {
        return "";
    }

    /**
     * Registers a {@link ConfigurationNode} of translations.
     *
     * <p>The path of each string contained within the node will be used as the key</p>
     *
     * @param locale a locale
     * @param node the node of translations
     * @param escapeSingleQuotes whether to escape single quotes
     * @throws IllegalArgumentException if a translation key already exists
     */
    void registerAll(final @NotNull Locale locale, final @NotNull ConfigurationNode node, final boolean escapeSingleQuotes) {
        String nodeStringValue = node.getString();
        if (nodeStringValue != null) {
            NodePath nodePath = node.path();
            //this.register(String.join(".", node.path()), locale, nodeStringValue);
            return;
        }
        for (ConfigurationNode childNode : node.childrenMap().values()) {
            registerAll(locale, childNode, escapeSingleQuotes);
        }
    }


}
