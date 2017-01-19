/*
 * Copyright 2015 Changefirst Ltd.
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
import org.keycloak.models.*;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.StorageId;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Dummy User federation for local development
 *
 * @author Istvan Orban
 */
public class DummyUserFederationProvider implements UserStorageProvider, UserLookupProvider, CredentialInputValidator  {

    private static final Logger LOG = Logger.getLogger(DummyUserFederationProvider.class);

    protected  KeycloakSession session;
    protected  ComponentModel model;
    protected  String passwordMap;
    protected  List<String> attributes;

    public DummyUserFederationProvider(KeycloakSession session, ComponentModel model, String passwordMap, List<String> attribList) {
        this.session = session;
        this.model = model;
        this.passwordMap = passwordMap;
        this.attributes = attribList;
    }

   // UserLookupProvider methods

    public UserModel createAdapter(RealmModel realm, String username) {
        LOG.infof("Creating user adapter for: %s", username);
        UserModel userModel = session.userLocalStorage().addUser(realm, username);

        userModel.setFederationLink(model.getId());
        userModel.setEnabled(true);
        userModel.setEmail(username);
        userModel.setEmailVerified(true);
        userModel.setFirstName("Dummy");
        userModel.setLastName("Migration");

        return userModel;
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        StorageId storageId = new StorageId(id);
        String username = storageId.getExternalId();
        return getUserByUsername(username, realm);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        UserModel adapter = createAdapter(realm, username);
        return adapter;
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return null;
    }

    // CredentialInputValidator methods

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return credentialType.equals(CredentialModel.PASSWORD);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return credentialType.equals(CredentialModel.PASSWORD);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {

        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;

        UserCredentialModel cred = (UserCredentialModel)input;
        String credValue = cred.getValue();

        String migrated = user.getFirstAttribute("migrated");

        boolean valid = true;

        if ( migrated == null || Boolean.valueOf(migrated).booleanValue() != true ) {

            Pattern pattern = Pattern.compile(passwordMap);
            Matcher matcher = pattern.matcher(credValue);

            valid = false;

            if ( matcher.find() && matcher.groupCount() >= 1 ) {

                user.setSingleAttribute("migrated", "true");

                String roleName = null;
                roleName = matcher.group("roleName");
                if ( roleName != null) {
                    RoleModel roleModel = realm.getRole(roleName);
                    if ( roleModel != null) {
                        user.grantRole(roleModel);
                        LOG.infof("User %s, Role %s", user.getId(), roleModel);
                    }
                }

                String attributeValue = null;
                for ( String attrib: this.attributes) {
                    attributeValue = matcher.group(attrib);
                    if (attributeValue != null) {
                        user.setSingleAttribute(attrib, attributeValue);
                        LOG.infof("User %s, Attribute %s with %s", user.getId(), attrib, attributeValue);
                    }
                }
                user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                valid = true;
            } else {
                LOG.warnf("Not possible to process password for user %s", user.getId());
            }

        }


        return valid;
    }

    @Override
    public void close() {
        //n/a
    }
}
