/*
 * Copyright 2026 Muteb AlMughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.testin.view;

import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BrowserOpened extends BrowserLauncher {
    private final @NotNull List<String> opened = new CopyOnWriteArrayList<>();

    public static @NotNull BrowserOpened recording(final @NotNull Disposable test) {
        final @NotNull BrowserOpened browser = new BrowserOpened();
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), BrowserLauncher.class, browser, test);
        return browser;
    }

    public @NotNull List<String> addresses() {
        return List.copyOf(opened);
    }

    @Override
    public void open(final @NotNull String url) {
        opened.add(url);
    }

    @Override
    public void browse(final @NotNull File file) {
        opened.add(file.toString());
    }

    @Override
    public void browse(final @NotNull Path path) {
        opened.add(path.toString());
    }

    @Override
    public void browse(final @NotNull String url, final WebBrowser browser, final Project p) {
        opened.add(url);
    }
}
