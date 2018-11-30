package org.cloudfoundry.credhub.domain;

import java.io.IOException;

import org.cloudfoundry.credhub.credential.UserCredentialValue;
import org.cloudfoundry.credhub.entity.EncryptedValue;
import org.cloudfoundry.credhub.entity.UserCredentialVersionData;
import org.cloudfoundry.credhub.request.GenerationParameters;
import org.cloudfoundry.credhub.request.StringGenerationParameters;
import org.cloudfoundry.credhub.util.JsonObjectMapper;

public class UserCredentialVersion extends CredentialVersion<UserCredentialVersion> {
  private final UserCredentialVersionData delegate;
  private StringGenerationParameters generationParameters;
  private JsonObjectMapper jsonObjectMapper;
  private String password;

  public UserCredentialVersion() {
    this(new UserCredentialVersionData());
  }

  public UserCredentialVersion(UserCredentialVersionData delegate) {
    super(delegate);
    this.delegate = delegate;
    jsonObjectMapper = new JsonObjectMapper();
  }

  public UserCredentialVersion(String name) {
    this(new UserCredentialVersionData(name));
  }

  public UserCredentialVersion(
    UserCredentialValue userValue,
    StringGenerationParameters generationParameters,
    Encryptor encryptor
  ) {
    this();
    this.setEncryptor(encryptor);
    this.setPassword(userValue.getPassword());
    this.setUsername(userValue.getUsername());
    this.setGenerationParameters(generationParameters);
    this.setSalt(userValue.getSalt());
  }

  @Override
  public String getCredentialType() {
    return delegate.getCredentialType();
  }

  @Override
  public void rotate() {
    String decryptedPassword = getPassword();
    StringGenerationParameters decryptedGenerationParameters = getGenerationParameters();

    setPassword(decryptedPassword);
    setGenerationParameters(decryptedGenerationParameters);
  }

  public String getPassword() {
    this.password = (String) super.getValue();
    return this.password;
  }

  public UserCredentialVersion setPassword(String password) {
    if (password != null) {
      super.setValue(password);
    }
    return this;
  }

  public String getUsername() {
    return delegate.getUsername();
  }

  public UserCredentialVersion setUsername(String username) {
    delegate.setUsername(username);
    return this;
  }

  public String getSalt() {
    return delegate.getSalt();
  }

  public UserCredentialVersion setSalt(String salt) {
    delegate.setSalt(salt);
    return this;
  }

  public StringGenerationParameters getGenerationParameters() {
    String parameterJson = encryptor.decrypt(delegate.getEncryptedGenerationParameters());
    String password = this.password == null ? getPassword() : this.password;

    if (parameterJson == null) {
      return null;
    }

    try {
      StringGenerationParameters generationParameters = jsonObjectMapper
        .deserializeBackwardsCompatibleValue(parameterJson, StringGenerationParameters.class);
      generationParameters.setLength(password.length());
      return generationParameters;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public UserCredentialVersion setGenerationParameters(StringGenerationParameters generationParameters) {
    EncryptedValue encryptedParameters;
    try {
      String generationParameterJson =
        generationParameters != null ? jsonObjectMapper.writeValueAsString(generationParameters)
          : null;
      if (generationParameterJson != null) {
        encryptedParameters = encryptor.encrypt(generationParameterJson);
        delegate.setEncryptedGenerationParameters(encryptedParameters);
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  @Override
  public boolean matchesGenerationParameters(GenerationParameters generationParameters) {
    if (generationParameters == null) {
      return true;
    }

    return generationParameters.equals(getGenerationParameters());
  }
}
