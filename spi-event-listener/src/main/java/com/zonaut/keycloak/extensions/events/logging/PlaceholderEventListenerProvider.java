package com.zonaut.keycloak.extensions.events.logging;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;

public class PlaceholderEventListenerProvider implements EventListenerProvider {

  private static final Logger log = Logger.getLogger(PlaceholderEventListenerProvider.class);

  private final KeycloakSession session;
  private final RealmProvider model;

  public PlaceholderEventListenerProvider(KeycloakSession session) {
    this.session = session;
    this.model = session.realms();
  }

  @Override
  public void onEvent(Event event) {
    log.infof("## NEW %s EVENT", event.getType());
    log.info("-----------------------------------------------------------");
    event.getDetails().forEach((key, value) -> log.info(key + ": " + value));

    // USE CASE SCENARIO, I'm sure there are better use case scenario's :p
    //
    // Let's assume for whatever reason you only want the user
    // to be able to verify his account if a transaction we make succeeds.
    // Let's say an external call to a service needs to return a 200 response code
    // or we throw an exception.

    // When the user tries to login after a failed attempt,
    // the user remains unverified and when trying to login will receive another
    // verify account email.

    if (EventType.REGISTER.equals(event.getType())) {
      RealmModel realm = this.model.getRealm(event.getRealmId());
      UserModel user = this.session.users().getUserById(event.getUserId(), realm);
      if (user != null && user.getEmail() != null && user.isEmailVerified()) {
        log.info("USER HAS REGISTERED : " + event.getUserId());

        // Example of adding an attribute when this event happens
        // user.setSingleAttribute("attribute-key", "attribute-value");

        UserUuidDto userUuidDto = new UserUuidDto(event.getType().name(), event.getUserId(), user.getEmail(),
            user.getFirstName(), user.getLastName());
        UserVerifiedTransaction userVerifiedTransaction = new UserVerifiedTransaction(userUuidDto);

        // enlistPrepare -> if our transaction fails than the user is NOT verified
        // enlist -> if our transaction fails than the user is still verified
        // enlistAfterCompletion -> if our transaction fails our user is still verified

        session.getTransactionManager().enlistPrepare(userVerifiedTransaction);
      }
    } else if (EventType.DELETE_ACCOUNT.equals(event.getType())) {

    }
    log.info("-----------------------------------------------------------");
  }

  @Override
  public void onEvent(AdminEvent adminEvent, boolean b) {
    log.info("## NEW ADMIN EVENT");
    log.info("-----------------------------------------------------------");
    log.info("Resource path" + ": " + adminEvent.getResourcePath());
    log.info("Resource type" + ": " + adminEvent.getResourceType());
    log.info("Operation type" + ": " + adminEvent.getOperationType());

    if (ResourceType.USER.equals(adminEvent.getResourceType())
        && OperationType.CREATE.equals(adminEvent.getOperationType())) {
      log.info("A new user has been created");
    }

    log.info("-----------------------------------------------------------");
  }

  @Override
  public void close() {
    // Nothing to close
  }

}
