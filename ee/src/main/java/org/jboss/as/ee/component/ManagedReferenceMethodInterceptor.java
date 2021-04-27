/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ee.component;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.Interceptors;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ManagedReferenceMethodInterceptor implements Interceptor {
    private final Object contextKey;
    private final Method method;
    private MethodHandle mh;

    ManagedReferenceMethodInterceptor(final Object contextKey, final Method method) {
        this.contextKey = contextKey;
        this.method = method;
    }

    /**
     * {@inheritDoc}
     */
    /* This is a hacky implementation to demonstrate switching virtual method lookup from
     * Reflection to MethodHandle.
     */
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final ManagedReference reference = (ManagedReference) context.getPrivateData(ComponentInstance.class).getInstanceData(contextKey);
        final Object instance = reference.getInstance();
        final Object[] params = context.getParameters();
        try {
            if (mh == null)
            {
               final Object o = method.invoke(instance, params);
               mh = MethodHandles.lookup().unreflect(method);
               return o;
            }
            else
            {
               if (params != null && params.length > 0)
               {
                   final List<Object> mtParams = new ArrayList<>(params.length + 1);
                   mtParams.add(instance);
                   for (Object o:params) {
                      mtParams.add(o);
                   }
                   return mh.invokeWithArguments(mtParams);
               } else {
                   return mh.invoke(instance);
               }
            }
        } catch (IllegalAccessException e) {
            final IllegalAccessError n = new IllegalAccessError(e.getMessage());
            n.setStackTrace(e.getStackTrace());
            throw n;
        } catch (InvocationTargetException e) {
           throw Interceptors.rethrow(e.getCause());
        } catch (Throwable t) {
           final Throwable cause = t.getCause();
           if (cause != null)
           {
              throw new Exception(cause.getMessage(), cause);
           } else
           {
              throw new Exception(t.getClass().getName()+":"+t.getMessage() );
           }
        }
    }
}

