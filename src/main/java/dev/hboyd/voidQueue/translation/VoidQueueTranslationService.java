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
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import dev.hboyd.voidQueue.utils.JarUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: Finish implementation
public class VoidQueueTranslationService {
    public static final String TRANSLATIONS_DIR_NAME = "translations";
    private static final Pattern LANGUAGE_TAG_PATTERN = Pattern.compile("[a-z]{2}_[A-Z]{2}");

    private final Path dataDirPath;
    private final Path translationsDir;

    private NodeTranslationStore translationStore;

    public VoidQueueTranslationService(Path dataDirPath) throws IOException {
        this.dataDirPath = dataDirPath;
        translationsDir = dataDirPath.resolve(TRANSLATIONS_DIR_NAME);
        Files.createDirectories(translationsDir);

        loadTranslations();
    }

    private void loadTranslations() throws IOException {
        translationStore = (NodeTranslationStore) (TranslationStore.StringBased<String>) MiniMessageTranslationStore.create(Key.key("voidqueue:lang"));
        GlobalTranslator.translator().addSource(translationStore);

        JarUtil.copyFolderFromJar(TRANSLATIONS_DIR_NAME, dataDirPath, JarUtil.CopyOption.COPY_IF_NOT_EXIST);

        registerAllTranslations(translationsDir);
    }

    public void reloadTranslations() {
        GlobalTranslator.translator().removeSource(translationStore);
    }

    private void registerAllTranslations(Path translationsDir) throws IOException {
        if (!translationsDir.toFile().isDirectory()) throw new IOException("Not a directory: " + translationsDir);

        for (File file : translationsDir.toFile().listFiles(File::isFile)) {
            Matcher matcher = LANGUAGE_TAG_PATTERN.matcher(file.getName());
            if (!matcher.find()) continue;

            String languageTag = matcher.group().replace('_', '-');
            Locale locale = Locale.forLanguageTag(languageTag);

            if (locale != null) registerTranslations(locale, file);
        }
    }

    private void registerTranslations(Locale locale, File translationYamlFile) throws ConfigurateException {
        //Message config
        ConfigurationNode translationsNode;

        translationsNode = YamlConfigurationLoader.builder().file(translationYamlFile).build().load();
        translationStore.registerAll(locale, translationsNode, false);

    }

}
