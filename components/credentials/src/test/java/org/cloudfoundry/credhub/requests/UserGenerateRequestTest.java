package org.cloudfoundry.credhub.requests;

import org.cloudfoundry.credhub.helpers.JsonTestHelper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class UserGenerateRequestTest {

  @Test
  public void getUsername_whenUsernameIsInParams_shouldReturnUsername() {
    final String json = "{"
      + "\"name\": \"/example/user\","
      + "\"type\": \"user\","
      + "\"parameters\": {"
      + "\"username\":\"darth-vader\","
      + "\"exclude_lower\":\"true\""
      + "}"
      + "}";

    final UserGenerateRequest deserialize = (UserGenerateRequest) JsonTestHelper
      .deserialize(json, BaseCredentialGenerateRequest.class);

    assertThat(deserialize, instanceOf(UserGenerateRequest.class));
    assertThat(deserialize.getUserName(), equalTo("darth-vader"));
  }

  @Test
  public void getUsername_whenUsernameIsInValue_shouldReturnUsername() {
    final String json = "{"
      + "\"name\": \"/example/user\","
      + "\"type\": \"user\","
      + "\"value\": {"
      + "\"username\":\"darth-vader\""
      + "}"
      + "}";

    final UserGenerateRequest deserialize = (UserGenerateRequest) JsonTestHelper
      .deserialize(json, BaseCredentialGenerateRequest.class);

    assertThat(deserialize, instanceOf(UserGenerateRequest.class));
    assertThat(deserialize.getUserName(), equalTo("darth-vader"));
  }

  @Test
  public void getUsername_whenUsernameIsInBothValueAndParameters_prefersParameters() {
    final String json = "{"
      + "\"name\": \"/example/user\","
      + "\"type\": \"user\","
      + "\"parameters\": {"
      + "\"username\":\"darth-vader\""
      + "},"
      + "\"value\": {"
      + "\"username\":\"fnu\""
      + "}"
      + "}";

    final UserGenerateRequest deserialize = (UserGenerateRequest) JsonTestHelper
      .deserialize(json, BaseCredentialGenerateRequest.class);

    assertThat(deserialize, instanceOf(UserGenerateRequest.class));
    assertThat(deserialize.getUserName(), equalTo("darth-vader"));
  }
}
