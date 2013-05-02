/**
 *  Copyright 2011 Terracotta, Inc.
 *  Copyright 2011 Oracle, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jsr107.tck;

import org.jsr107.tck.util.ExcludeListExcluder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.CachingShutdownException;
import javax.cache.MutableConfiguration;
import javax.cache.OptionalFeature;
import javax.cache.Status;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import static javax.cache.Status.STARTED;
import static javax.cache.Status.STOPPED;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for CacheManager
 * <p/>
 *
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public class CacheManagerTest extends TestSupport {
    protected final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Rule used to exclude tests
     */
    @Rule
    public ExcludeListExcluder rule = new ExcludeListExcluder(this.getClass()) {

        /* (non-Javadoc)
         * @see javax.cache.util.ExcludeListExcluder#isExcluded(java.lang.String)
         */
        @Override
        protected boolean isExcluded(String methodName) {
            if ("testUnwrap".equals(methodName) && getUnwrapClass(CacheManager.class) == null) {
                return true;
            }

            return super.isExcluded(methodName);
        }
    };

    @Before
    public void startUp() {
        try {
            Caching.getCachingProvider().close();
        }   catch (CachingShutdownException e) {
            //this will happen if we call close twice in a row.
        }
    }

    @Test
    public void configureCache_NullCacheName() {
        CacheManager cacheManager = getCacheManager();
        try {
            cacheManager.configureCache(null, new MutableConfiguration());
            fail("should have thrown an exception - null cache name not allowed");
        } catch (NullPointerException e) {
            //good
        }
    }

    @Test
    public void configureCache_NullCacheConfiguration() {
        CacheManager cacheManager = getCacheManager();
        try {
            cacheManager.configureCache("cache", null);
            fail("should have thrown an exception - null cache configuration not allowed");
        } catch (NullPointerException e) {
            //good
        }
    }

    @Test
    public void createCache_Same() {
        String name = "c1";
        CacheManager cacheManager = getCacheManager();
        Cache cache = cacheManager.configureCache(name, new MutableConfiguration());
        assertSame(cache, cacheManager.getCache(name));
    }

    @Test
    public void testReuseCacheManager() throws Exception {
        URI uri = new URI(this.getClass().getName());

        CachingProvider provider = Caching.getCachingProvider();

        CacheManager cacheManager = provider.getCacheManager(uri, provider.getDefaultClassLoader());
        assertThat(cacheManager.getStatus(), is(STARTED));
        cacheManager.close();
        assertThat(cacheManager.getStatus(), is(STOPPED));

        try {
            cacheManager.configureCache("Dog", null);
            fail();
        } catch (IllegalStateException e) {
            //expected
        }

        CacheManager otherCacheManager = provider.getCacheManager(uri, provider.getDefaultClassLoader());
        assertThat(otherCacheManager.getStatus(), is(STARTED));

        assertNotSame(cacheManager, otherCacheManager);
    }


    @Test
    public void createCache_NameOK() {
        String name = "c1";
        Cache cache = getCacheManager().configureCache(name, new MutableConfiguration());
        assertEquals(name, cache.getName());
    }

    @Test
    public void createCache_StatusOK() {
        String name = "c1";
        Cache cache = getCacheManager().configureCache(name, new MutableConfiguration());
        assertSame(Status.STARTED, cache.getStatus());
    }

    @Test
    public void createCache_Different() {
        String name1 = "c1";
        CacheManager cacheManager = getCacheManager();
        Cache cache1 = cacheManager.configureCache(name1, new MutableConfiguration());
        assertEquals(Status.STARTED, cache1.getStatus());

        String name2 = "c2";
        Cache cache2 = cacheManager.configureCache(name2, new MutableConfiguration());
        assertEquals(Status.STARTED, cache2.getStatus());

        assertEquals(cache1, cacheManager.getCache(name1));
        assertEquals(cache2, cacheManager.getCache(name2));
    }

    @Test
    public void createCache_DifferentSameName() {
        CacheManager cacheManager = getCacheManager();
        String name1 = "c1";
        Cache cache1 = cacheManager.configureCache(name1, new MutableConfiguration());
        assertEquals(cache1, cacheManager.getCache(name1));
        checkStarted(cache1);

        Cache cache2 = cacheManager.configureCache(name1, new MutableConfiguration());
        assertSame(cache1, cache2);
    }

    @Test
    public void removeCache_Null() {
        CacheManager cacheManager = getCacheManager();
        try {
            cacheManager.removeCache(null);
            fail("should have thrown an exception - cache name null");
        } catch (NullPointerException e) {
            //good
        }
    }

    @Test
    public void removeCache_There() {
        CacheManager cacheManager = getCacheManager();
        String name1 = "c1";
        cacheManager.configureCache(name1, new MutableConfiguration());
        assertTrue(cacheManager.removeCache(name1));
        assertFalse(cacheManager.getCaches().iterator().hasNext());
    }

    @Test
    public void removeCache_CacheStopped() {
        CacheManager cacheManager = getCacheManager();
        String name1 = "c1";
        Cache cache1 = cacheManager.configureCache(name1, new MutableConfiguration());
        cacheManager.removeCache(name1);
        checkStopped(cache1);
    }

    @Test
    public void removeCache_NotThere() {
        CacheManager cacheManager = getCacheManager();
        assertFalse(cacheManager.removeCache("c1"));
    }

    @Test
    public void removeCache_Stopped() {
        CacheManager cacheManager = getCacheManager();
        cacheManager.close();
        try {
            cacheManager.removeCache("c1");
            fail();
        } catch (IllegalStateException e) {
            //ok
        }
    }

    @Test
    public void shutdown_stopCalled() {
        CacheManager cacheManager = getCacheManager();

        Cache cache1 = cacheManager.configureCache("c1", new MutableConfiguration());
        Cache cache2 = cacheManager.configureCache("c2", new MutableConfiguration());

        cacheManager.close();

        checkStopped(cache1);
        checkStopped(cache2);
    }

    @Test
    public void shutdown_status() {
        CacheManager cacheManager = getCacheManager();

        assertEquals(Status.STARTED, cacheManager.getStatus());
        cacheManager.close();
        assertEquals(STOPPED, cacheManager.getStatus());
    }

    @Test
    public void shutdown_statusTwice() {
        CacheManager cacheManager = getCacheManager();

        cacheManager.close();
        try {
            cacheManager.close();
            fail();
        } catch (IllegalStateException e) {
            // good
        }
    }

    @Test
    public void shutdown_cachesEmpty() {
        CacheManager cacheManager = getCacheManager();

        cacheManager.configureCache("c1", new MutableConfiguration());
        cacheManager.configureCache("c2", new MutableConfiguration());

        cacheManager.close();
        assertFalse(cacheManager.getCaches().iterator().hasNext());
    }

    @Test
    public void getUserTransaction() {
        boolean transactions = Caching.getCachingProvider().isSupported(OptionalFeature.TRANSACTIONS);
        try {
            getCacheManager().getUserTransaction();
            if (!transactions) {
                fail();
            }
        } catch (UnsupportedOperationException e) {
            assertFalse(transactions);
        }
    }

    @Test
    public void getCache_Missing() {
        CacheManager cacheManager = getCacheManager();
        assertNull(cacheManager.getCache("notThere"));
    }

    @Test
    public void getCache_There() {
        String name = this.toString();
        CacheManager cacheManager = getCacheManager();
        Cache cache = cacheManager.configureCache(name, new MutableConfiguration());
        assertSame(cache, cacheManager.getCache(name));
    }

    @Test
    public void getCache_Missing_Stopped() {
        CacheManager cacheManager = getCacheManager();
        cacheManager.close();
        try {
            cacheManager.getCache("notThere");
            fail();
        } catch (IllegalStateException e) {
            //good
        }
    }

    @Test
    public void getCache_There_Stopped() {
        String name = this.toString();
        CacheManager cacheManager = getCacheManager();
        cacheManager.configureCache(name, new MutableConfiguration());
        cacheManager.close();
        try {
            cacheManager.getCache(name);
            fail();
        } catch (IllegalStateException e) {
            //good
        }
    }

    @Test
    public void getCaches_Empty() {
        CacheManager cacheManager = getCacheManager();
        assertFalse(cacheManager.getCaches().iterator().hasNext());
    }

    @Test
    public void getCaches_NotEmpty() {
        CacheManager cacheManager = getCacheManager();

        ArrayList<Cache> caches1 = new ArrayList<Cache>();
        caches1.add(cacheManager.configureCache("c1", new MutableConfiguration()));
        caches1.add(cacheManager.configureCache("c2", new MutableConfiguration()));
        caches1.add(cacheManager.configureCache("c3", new MutableConfiguration()));

        checkCollections(caches1, cacheManager.getCaches());
    }

    @Test
    public void getCaches_MutateReturn() {
        CacheManager cacheManager = getCacheManager();

        cacheManager.configureCache("c1", new MutableConfiguration());

        try {
            cacheManager.getCaches().iterator().remove();
            fail();
        } catch (UnsupportedOperationException e) {
            // immutable
        }
    }

    @Test
    public void getCaches_MutateCacheManager() {
        CacheManager cacheManager = getCacheManager();

        String removeName = "c2";
        ArrayList<Cache> caches1 = new ArrayList<Cache>();
        caches1.add(cacheManager.configureCache("c1", new MutableConfiguration()));
        cacheManager.configureCache(removeName, new MutableConfiguration());
        caches1.add(cacheManager.configureCache("c3", new MutableConfiguration()));

        Iterable<Cache<?, ?>> it;
        int size;

        it = cacheManager.getCaches();
        size = 0;
        for (Cache<?, ?> c : it) {
            size++;
        }
        assertEquals(3, size);
        cacheManager.removeCache(removeName);
        size = 0;
        for (Cache<?, ?> c : it) {
            size++;
        }
        assertEquals(3, size);

        it = cacheManager.getCaches();
        size = 0;
        for (Cache<?, ?> c : it) {
            size++;
        }
        assertEquals(2, size);
        checkCollections(caches1, it);
    }

    @Test
    public void isSupported() {
        CacheManager cacheManager = getCacheManager();

        for (OptionalFeature feature : OptionalFeature.values()) {
            assertSame(feature.toString(), Caching.getCachingProvider().isSupported(feature), cacheManager.isSupported(feature));
        }
    }

    @Test
    public void testUnwrap() {
        //Assumes rule will exclude this test when no unwrapClass is specified
        final Class<?> unwrapClass = getUnwrapClass(CacheManager.class);
        final CacheManager cacheManager = getCacheManager();
        final Object unwrappedCacheManager = cacheManager.unwrap(unwrapClass);

        assertTrue(unwrapClass.isAssignableFrom(unwrappedCacheManager.getClass()));
    }

    // ---------- utilities ----------

    private <T> void  checkCollections(Collection<T> collection1, Iterable<?> iterable2) {
        ArrayList<Object> collection2 = new ArrayList<Object>();
        for (Object element : iterable2) {
            assertTrue(collection1.contains(element));
            collection2.add(element);
        }
        assertEquals(collection1.size(), collection2.size());
        for (T element : collection1) {
            assertTrue(collection2.contains(element));
        }
    }

    private void checkStarted(Cache cache) {
        Status status = cache.getStatus();
        //may be asynchronous
        assertTrue(status == Status.STARTED);
    }

    private void checkStopped(Cache cache) {
        Status status = cache.getStatus();
        //may be asynchronous
        assertTrue(status == STOPPED);
    }

//    todo GL adapt this test to its new home @Test
//    public void setStatisticsEnabled() {
//        Configuration<?, ?> config = getConfiguration();
//        boolean isStatisticsEnabled = config.isStatisticsEnabled();
//        config.setStatisticsEnabled(!isStatisticsEnabled);
//        assertEquals(!isStatisticsEnabled, config.isStatisticsEnabled());
//    }

}
