/*
 * Copyright 2015 Smartling, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.changefirst.keycloak.provider;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Remote user federation provider factory.
 *
 * @author Scott Rossillo
 */
public class DummyUserFederationProviderFactory implements UserStorageProviderFactory<DummyUserFederationProvider> {

    public static final String PROVIDER_NAME = "Dummy User Federation SPI";
    public static final String CONFIG_PASSMAP = "passwordMap";
    public static final String DEFAULT_PASSMAP = "(?<clientId>\\d+):(?<roleName>\\w+)";
    public static final String CONFIG_ATTRIBUTE_NAMES = "groupNames";

    private static final Logger LOG = Logger.getLogger(DummyUserFederationProviderFactory.class);

    protected static final List<ProviderConfigProperty> configMetadata;

    static {
        configMetadata = ProviderConfigurationBuilder.create()
                .property().name(CONFIG_PASSMAP)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Password field mapper")
                .defaultValue(DEFAULT_PASSMAP)
                .helpText("Using a regexp named groups to map a string to specific portions. E.g. The example has 2 named groups")
                .add()
                .property().name(CONFIG_ATTRIBUTE_NAMES)
                .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
                .label("Comma separated list of named groups in the expression to process as attributes")
                .defaultValue("clientId")
                .helpText("Using this property, one can pass in attrbitues onto the user")
                .add()
                .build();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
       String passwordMap = config.getConfig().getFirst(CONFIG_PASSMAP);
       boolean valid = false;

       if ( passwordMap != null && passwordMap.length() > 0 ) {
           valid =  passwordMap.contains("roleName") && isRegularExpressionValid(passwordMap);
       }

        LOG.debugf("validating module config %s", valid);

       if (!valid) {
           throw new ComponentValidationException("Password is invalid. Please use a regular expression with named groups that contains at least a roleName");
       }
    }

    private boolean isRegularExpressionValid(String pattern) {
        try {
            Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException exception) {
            LOG.warnf("Invalid regular expression: %s - %s", pattern, exception.getMessage());
            return false;
        }
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public String getHelpText() {
        return "Used to provide dummy user federation. it is used for testing only. Please do not use this in production";
    }

    @Override
    public DummyUserFederationProvider create(KeycloakSession session, ComponentModel model) {
        String passwordMap = model.getConfig().getFirst(CONFIG_PASSMAP);
        List<String> attribList = model.getConfig().getList(CONFIG_ATTRIBUTE_NAMES);
        DummyUserFederationProvider result = new DummyUserFederationProvider(session, model, passwordMap, attribList);
        return result;
    }

}
