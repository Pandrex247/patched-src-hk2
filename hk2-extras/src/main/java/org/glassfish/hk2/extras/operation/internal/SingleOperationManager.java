/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.hk2.extras.operation.internal;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extras.operation.OperationContext;
import org.glassfish.hk2.extras.operation.OperationIdentifier;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

/**
 * @author jwells
 *
 */
public class SingleOperationManager {
    private final static String ID_PREAMBLE = "OperationIdentifier(";
    
    private final Object operationLock = new Object();
    private final OperationManagerImpl parent;
    private final Annotation scope;
    private final HashMap<OperationIdentifier, OperationHandleImpl> openScopes = new HashMap<OperationIdentifier, OperationHandleImpl>();
    private final HashMap<Long, OperationHandleImpl> threadToHandleMap = new HashMap<Long, OperationHandleImpl>();
    private final ServiceLocator locator;
    private long scopedIdentifier;
    private final ActiveDescriptor<?> operationDescriptor;
    
    /* package */ SingleOperationManager(OperationManagerImpl parent,
            Annotation scope,
            ServiceLocator locator) {
        this.parent = parent;
        this.scope = scope;
        this.locator = locator;
        
        OperationContext<?> found = null;
        for (OperationContext<?> context : locator.getAllServices(OperationContext.class)) {
            if (context.getScope().equals(scope.annotationType())) {
                found = context;
                break;
            }
        }
        
        if (found == null) {
            throw new IllegalStateException("Could not find the OperationContext for scope " + scope);
        }
        
        found.setOperationManager(this);
        
        OperationDescriptor opDesc = new OperationDescriptor(scope, this);
        
        operationDescriptor = ServiceLocatorUtilities.addOneDescriptor(locator, opDesc);
    }
    
    private OperationIdentifierImpl allocateNewIdentifier() {
        return new OperationIdentifierImpl(
                ID_PREAMBLE + scopedIdentifier++ + "," + scope.annotationType().getName() + ")",
                scope);
    }
    
    public OperationHandleImpl createOperation() {
        
        synchronized (operationLock) {
            OperationIdentifierImpl id = allocateNewIdentifier();
            OperationHandleImpl created = new OperationHandleImpl(this, id, operationLock, locator);
            
            openScopes.put(id, created);
            
            return created;
        }
    }

    /**
     * Called with the operationLock held
     * 
     * @param closeMe The non-null operation to close
     */
    /* package */ void closeOperation(OperationHandleImpl closeMe) {
        openScopes.remove(closeMe.getIdentifier());
    }
    
    /**
     * OperationLock must be held
     * 
     * @param threadId The threadId to associate with this handle
     * @param handle The handle to be associated with this thread
     */
    /* package */ void associateWithThread(long threadId, OperationHandleImpl handle) {
        threadToHandleMap.put(threadId, handle);
    }
    
    /**
     * OperationLock must be held
     * 
     * @param threadId The threadId to disassociate with this handle
     */
    /* package */ OperationHandleImpl disassociateThread(long threadId) {
        return threadToHandleMap.remove(threadId);
    }
    
    /**
     * OperationLock must be held
     * 
     * @return The operation associated with the given thread
     */
    /* package */ OperationHandleImpl getCurrentOperationOnThisThread(long threadId) {
        return threadToHandleMap.get(threadId);
    }
    
    /**
     * OperationLock need NOT be held
     * 
     * @return The operation associated with the current thread
     */
    public OperationHandleImpl getCurrentOperationOnThisThread() {
        long threadId = Thread.currentThread().getId();
        
        synchronized (operationLock) {
            return getCurrentOperationOnThisThread(threadId);
        }
    }
}
