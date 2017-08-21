/*
 *    Copyright (c) 2014-2017 Neil Ellis
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sillelien.jas.jproxy;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Is the interface to monitor the files being compiled.
 *
 * @author Jose Maria Arranz Santamaria
 * @see JProxyConfig#setJProxyCompilerListener(JProxyCompilerListener)
 */
public interface JProxyCompilerListener {
    void beforeCompile(@NotNull File file);

    void afterCompile(@NotNull File file);
}
