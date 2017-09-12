
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

import com.sillelien.jas.impl.jproxy.JProxyConfigImpl;
import com.sillelien.jas.impl.jproxy.JProxyDefaultImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Is the class to create Java proxy objects based on Java objects and keep track of source code changes reloading classes when detected.
 *
 * @author Jose Maria Arranz Santamaria
 */
public final class JProxy {
    /**
     * Creates a proxy object using <code>java.lang.reflect.Proxy</code> based on the provided Java object and the class of the implemented Java interface.
     * <p>This method is a simplification for a single interface (the most common case) of {@link #create(Object, Class[])} .</p>
     *
     * @param <T>   the interface implemented by the original object and proxy object returned.
     * @param obj   the original object to proxy.
     * @param clasz the class of the interface implemented by the original object and proxy object returned.
     * @return the <code>java.lang.reflect.Proxy</code> object associated or the  original object when <code>GProxy</code> is disabled.
     */
    @Nullable
    public static <T> T create(@NotNull T obj, @NotNull Class<T> clasz) {
        return JProxyDefaultImpl.createStatic(obj, clasz);
    }

    /**
     * Creates a proxy object using <code>java.lang.reflect.Proxy</code> based on the provided Java object and the classes of the implemented Java interfaces.
     * <p>If <code>JProxy</code> has been configured and is enabled this method returns a <code>java.lang.reflect.Proxy</code> object implementing instead of
     * the original object provided. Methods called in proxy object are received by <code>JProxy</code> and forwarded to the original object, if source code
     * managed by <code>JProxy</code> has been changed, the class of the original object is reloaded based on the new source and the original object
     * is recreated with the new class and fields are re-set in the new object, then the method is called on the new original object.</p>
     * <p>If <code>JProxy</code> is disabled returns the original object provided with no performance penalty.</p>
     *
     * @param obj     the original object to proxy.
     * @param classes the classes of the interfaces implemented by the original object and proxy object returned.
     * @return the <code>java.lang.reflect.Proxy</code> object associated or the original object when <code>JProxy</code> is disabled.
     */
    @Nullable
    public static Object create(@NotNull Object obj, @NotNull Class<?>[] classes) {
        return JProxyDefaultImpl.createStatic(obj, classes);
    }

    /**
     * Creates a {@link JProxyConfig} object to be used to configure <code>JProxy</code> and {@link JProxyScriptEngineFactory}.
     *
     * @return a new configuration object.
     * @see #init(JProxyConfig)
     */
    public static JProxyConfig createJProxyConfig() {
        return JProxyDefaultImpl.createJProxyConfig();
    }

    /**
     * Initializes <code>JProxy</code> with the provided configuration object.
     *
     * @param config the configuration
     */
    public static void init(@NotNull JProxyConfig config) {
        JProxyDefaultImpl.initStatic((JProxyConfigImpl) config);
    }

    /**
     * Informs whether <code>JProxy</code> is configured and enabled.
     *
     * @return true if enabled.
     */
    public static boolean isEnabled() {
        return JProxyDefaultImpl.isEnabledStatic();
    }

    /**
     * Informs whether <code>JProxy</code> is enabled and started (timed checking for changes).
     *
     * @return true if running.
     */
    public static boolean isRunning() {
        return JProxyDefaultImpl.isRunningStatic();
    }

    /**
     * Starts source code periodic change detection.
     * <p>Periodicity of change detection is defined by {@link JProxyConfig#setScanPeriod(long)}.</p>
     * <p>By default when <code>JProxy</code> is initialized and enabled.</p>
     *
     * @return true if source change detection has been started again, false if it is already started or cannot start because <code>JProxy</code> is not enabled or initialized or scan period is not positive.
     * @see #start()
     */
    public static boolean start() {
        return JProxyDefaultImpl.startStatic();
    }

    /**
     * Stops source code periodic change detection.
     * <p>Periodicity of change detection is defined by {@link JProxyConfig#setScanPeriod(long)}</p>
     *
     * @return true if source change detection has been stopped, false if it is already stopped or <code>JProxy</code> is not enabled or initialized.
     * @see #stop()
     */
    public static boolean stop() {
        return JProxyDefaultImpl.stopStatic();
    }
}
