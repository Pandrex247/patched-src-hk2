/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.hk2.tests.locator.validating;

import javax.inject.Singleton;

import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ValidationService;
import org.glassfish.hk2.api.Validator;
import org.glassfish.hk2.tests.locator.utilities.TestModule;
import org.glassfish.hk2.utilities.BuilderHelper;

/**
 * @author jwells
 *
 */
public class ValidatingModule implements TestModule {

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Module#configure(org.glassfish.hk2.api.Configuration)
     */
    @Override
    public void configure(DynamicConfiguration configurator) {
        configurator.bind(BuilderHelper.link(SuperSecretService.class).qualifiedBy(Secret.class.getName()).build());
        configurator.bind(BuilderHelper.link(SystemService.class).build());
        configurator.bind(BuilderHelper.link(UserService.class).build());
        configurator.bind(BuilderHelper.link(NeverUnbindMeService.class).build());
        
        // Add validation services
        configurator.addActiveDescriptor(ValidationServiceImpl.class);
        configurator.bind(BuilderHelper.link(BindValidatorService.class.getName()).
                to(ValidationService.class.getName()).
                in(Singleton.class.getName()).
                build());
        configurator.bind(BuilderHelper.link(UnbindValidatorService.class.getName()).
                to(ValidationService.class.getName()).
                in(Singleton.class.getName()).
                build());
        
        // This is to test validation in the parent locator
        configurator.bind(BuilderHelper.link(DynamicValidator.class.getName()).
                to(Validator.class.getName()).
                in(Singleton.class.getName()).
                build());
        configurator.bind(BuilderHelper.link(DynamicServiceImpl1.class.getName()).
                to(DynamicService.class.getName()).
                in(Singleton.class.getName()).
                build());
        configurator.bind(BuilderHelper.link(DynamicServiceImpl2.class.getName()).
                to(DynamicService.class.getName()).
                in(Singleton.class.getName()).
                build());
        configurator.bind(BuilderHelper.link(DynamicValidationServiceImpl.class.getName()).
                to(ValidationService.class.getName()).
                in(Singleton.class.getName()).
                build());
        
    }

}
