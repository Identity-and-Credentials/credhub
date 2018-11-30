package org.cloudfoundry.credhub.integration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import org.cloudfoundry.credhub.CredentialManagerApp;
import org.cloudfoundry.credhub.helper.JsonTestHelper;
import org.cloudfoundry.credhub.request.PermissionOperation;
import org.cloudfoundry.credhub.util.DatabaseProfileResolver;
import org.cloudfoundry.credhub.view.PermissionsV2View;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.cloudfoundry.credhub.helper.RequestHelper.generatePassword;
import static org.cloudfoundry.credhub.util.AuthConstants.ALL_PERMISSIONS_TOKEN;
import static org.cloudfoundry.credhub.util.AuthConstants.USER_A_ACTOR_ID;
import static org.cloudfoundry.credhub.util.AuthConstants.USER_A_TOKEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ActiveProfiles(value = {"unit-test", "unit-test-permissions"}, resolver = DatabaseProfileResolver.class)
@SpringBootTest(classes = CredentialManagerApp.class)
@Transactional
public class CredentialFindTest {

  private final String credentialName = "/my-namespace/subTree/credential-name";
  @Autowired
  private WebApplicationContext webApplicationContext;
  private MockMvc mockMvc;

  @Before
  public void beforeEach() {
    mockMvc = MockMvcBuilders
      .webAppContextSetup(webApplicationContext)
      .apply(springSecurity())
      .build();
  }

  @Test
  public void findCredentials_byNameLike_whenSearchTermContainsNoSlash_returnsCredentialMetadata() throws Exception {
    generatePassword(mockMvc, credentialName, true, 20, ALL_PERMISSIONS_TOKEN);
    ResultActions response = findCredentialsByNameLike(credentialName.substring(4).toUpperCase(),
      ALL_PERMISSIONS_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials[0].name").value(credentialName));
  }

  @Test
  public void findCredentials_byNameLike_returnsNoCredentialsIfUserDoesNotHaveReadAccess() throws Exception {
    generateCredentials();
    ResultActions response = findCredentialsByNameLike("/", USER_A_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(0)));
  }

  @Test
  public void findCredentials_byNameLike_returnsAllCredentialsWhenUserHasAllPermissions() throws Exception {
    generateCredentials();

    setPermissions("/*", PermissionOperation.READ, USER_A_ACTOR_ID);

    ResultActions response = findCredentialsByNameLike("/", USER_A_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(5)));
  }

  @Test
  public void findCredentials_byNameLike_returnsSubsetWithFullPermissionPath() throws Exception {
    String credentialName = "/other_path/credentialC";
    generateCredentials();

    setPermissions(credentialName, PermissionOperation.READ, USER_A_ACTOR_ID);

    ResultActions response = findCredentialsByNameLike("/", USER_A_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(1)))
      .andExpect(jsonPath("$.credentials[0].name").value(credentialName));
  }

  @Test
  public void findCredentials_byPath_returnsCredentialMetaData() throws Exception {
    String substring = credentialName.substring(0, credentialName.lastIndexOf("/"));
    generatePassword(mockMvc, credentialName, true, 20, ALL_PERMISSIONS_TOKEN);

    ResultActions response = findCredentialsByPath(substring, ALL_PERMISSIONS_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials[0].name").value(credentialName));
  }

  @Test
  public void findCredentials_byPath_shouldOnlyFindPathsThatBeginWithSpecifiedSubstringCaseInsensitively()
    throws Exception {
    final String path = "namespace";

    assertTrue(credentialName.contains(path));

    ResultActions response = findCredentialsByPath(path.toUpperCase(), ALL_PERMISSIONS_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(0)));
  }

  @Test
  public void findCredentials_byPath_shouldReturnAllChildrenPrefixedWithThePathCaseInsensitively() throws Exception {
    final String path = "/my-namespace";
    generatePassword(mockMvc, credentialName, true, 20, ALL_PERMISSIONS_TOKEN);

    assertTrue(credentialName.startsWith(path));

    ResultActions response = findCredentialsByPath(path.toUpperCase(), ALL_PERMISSIONS_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(1)));
  }

  @Test
  public void findCredentials_byPath_shouldNotReturnCredentialsThatMatchThePathIncompletely() throws Exception {
    final String path = "/my-namespace/subTr";

    assertTrue(credentialName.startsWith(path));

    ResultActions response = findCredentialsByPath(path, ALL_PERMISSIONS_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(0)));
  }

  @Test
  public void findCredentials_byPath_returnsNoCredentialsIfUserDoesNotHaveReadAccess() throws Exception {
    generateCredentials();

    ResultActions response = findCredentialsByPath("/", USER_A_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(0)));
  }

  @Test
  public void findCredentials_byPath_returnsAllCredentialsWhenUserHasAllPermissions() throws Exception {
    generateCredentials();

    setPermissions("/*", PermissionOperation.READ, USER_A_ACTOR_ID);

    ResultActions response = findCredentialsByPath("/", USER_A_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(5)));
  }

  @Test
  public void findCredentials_byPath_returnsSubsetWithFullPermissionPath() throws Exception {
    String credentialName = "/other_path/credentialC";
    generateCredentials();

    setPermissions(credentialName, PermissionOperation.READ, USER_A_ACTOR_ID);

    ResultActions response = findCredentialsByPath("/", USER_A_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(1)))
      .andExpect(jsonPath("$.credentials[0].name").value(credentialName));
  }

  @Test
  public void findCredentials_byPath_returnsSubsetWithAsteriskInPermissionPath() throws Exception {
    generateCredentials();

    setPermissions("/path/to/*", PermissionOperation.READ, USER_A_ACTOR_ID);

    ResultActions response = findCredentialsByPath("/", USER_A_TOKEN);

    response.andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(2)))
      .andExpect(jsonPath("$.credentials[1].name").value("/path/to/credentialA"))
      .andExpect(jsonPath("$.credentials[0].name").value("/path/to/credentialB"));
  }

  @Test
  public void findCredentialsByPath_withExpiryDate() throws Exception {

    this.mockMvc.perform(post("/api/v1/data")
      .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
      .accept(APPLICATION_JSON)
      .contentType(APPLICATION_JSON)
      //language=JSON
      .content("{\n"
        + "  \"name\" : \"notExpiring\",\n"
        + "  \"type\" : \"certificate\",\n"
        + "  \"parameters\" : {\n"
        + "    \"common_name\" : \"federation\",\n"
        + "    \"is_ca\" : true,\n"
        + "    \"self_sign\" : true,\n"
        + "    \"duration\" : 32 \n"
        + "  }\n"
        + "}"))
      .andDo(print())
      .andExpect(status().isOk());

    this.mockMvc.perform(post("/api/v1/data")
      .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
      .accept(APPLICATION_JSON)
      .contentType(APPLICATION_JSON)
      //language=JSON
      .content("{\n"
        + "  \"name\" : \"willExpire\",\n"
        + "  \"type\" : \"certificate\",\n"
        + "  \"parameters\" : {\n"
        + "    \"common_name\" : \"federation\",\n"
        + "    \"is_ca\" : true,\n"
        + "    \"self_sign\" : true,\n"
        + "    \"duration\" : 29 \n"
        + "  }\n"
        + "}"))
      .andDo(print())
      .andExpect(status().isOk());

    String expiresWithinDays = "30";
    final MockHttpServletRequestBuilder request = get("/api/v1/data?path=/&expires-within-days=" + expiresWithinDays)
      .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
      .content("expires-within-days:30")
      .accept(APPLICATION_JSON);

    mockMvc.perform(request).andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(1)))
      .andExpect(jsonPath("$.credentials[0].name").value("/willExpire"));

  }

  @Test
  public void findCredentialsByName_withExpiryDate() throws Exception {

    this.mockMvc.perform(post("/api/v1/data")
      .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
      .accept(APPLICATION_JSON)
      .contentType(APPLICATION_JSON)
      //language=JSON
      .content("{\n"
        + "  \"name\" : \"notExpiring\",\n"
        + "  \"type\" : \"certificate\",\n"
        + "  \"parameters\" : {\n"
        + "    \"common_name\" : \"federation\",\n"
        + "    \"is_ca\" : true,\n"
        + "    \"self_sign\" : true,\n"
        + "    \"duration\" : 32 \n"
        + "  }\n"
        + "}"))
      .andDo(print())
      .andExpect(status().isOk());

    this.mockMvc.perform(post("/api/v1/data")
      .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
      .accept(APPLICATION_JSON)
      .contentType(APPLICATION_JSON)
      //language=JSON
      .content("{\n"
        + "  \"name\" : \"willExpire\",\n"
        + "  \"type\" : \"certificate\",\n"
        + "  \"parameters\" : {\n"
        + "    \"common_name\" : \"federation\",\n"
        + "    \"is_ca\" : true,\n"
        + "    \"self_sign\" : true,\n"
        + "    \"duration\" : 29 \n"
        + "  }\n"
        + "}"))
      .andDo(print())
      .andExpect(status().isOk());

    String expiresWithinDays = "30";
    final MockHttpServletRequestBuilder request = get(
      "/api/v1/data?name-like=ex&expires-within-days=" + expiresWithinDays)
      .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
      .content("expires-within-days:30")
      .accept(APPLICATION_JSON);

    mockMvc.perform(request).andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
      .andExpect(jsonPath("$.credentials", hasSize(1)))
      .andExpect(jsonPath("$.credentials[0].name").value("/willExpire"));

  }

  private void generateCredentials() throws Exception {
    List<String> names = Arrays.asList(new String[]{"/path/to/credentialA", "/path/something",
      "/path/to/credentialB", "/other_path/credentialC", "/another/credentialC"});

    for (String name : names) {
      generatePassword(mockMvc, name, true, 20, ALL_PERMISSIONS_TOKEN);
    }
  }

  private void setPermissions(String path, PermissionOperation operation, String actorID) throws Exception {
    MockHttpServletRequestBuilder addPermissionRequest = post("/api/v2/permissions")
      .header("Authorization", "Bearer " + ALL_PERMISSIONS_TOKEN)
      .accept(APPLICATION_JSON)
      .contentType(APPLICATION_JSON)
      .content("{"
        + "  \"actor\": \"" + actorID + "\",\n"
        + "  \"path\": \"" + path + "\",\n"
        + "  \"operations\": [\"" + operation.getOperation() + "\"]\n"
        + "}");

    String content = mockMvc.perform(addPermissionRequest).andExpect(status().isCreated()).andReturn().getResponse()
      .getContentAsString();
    PermissionsV2View returnValue = JsonTestHelper.deserialize(content, PermissionsV2View.class);
    assertThat(returnValue.getActor(), equalTo(USER_A_ACTOR_ID));
    assertThat(returnValue.getPath(), equalTo(path));
    assertThat(returnValue.getOperations(), equalTo(Collections.singletonList(operation)));

    assertThat(returnValue.getUuid(), notNullValue());
  }

  private ResultActions findCredentialsByNameLike(String pattern, String permissionsToken) throws Exception {
    final MockHttpServletRequestBuilder get = get("/api/v1/data?name-like=" + pattern)
      .header("Authorization", "Bearer " + permissionsToken)
      .accept(APPLICATION_JSON);

    return mockMvc.perform(get);
  }

  private ResultActions findCredentialsByPath(String path, String permissionsToken) throws Exception {
    final MockHttpServletRequestBuilder get = get("/api/v1/data?path=" + path)
      .header("Authorization", "Bearer " + permissionsToken)
      .accept(APPLICATION_JSON);

    return mockMvc.perform(get);
  }
}
