package org.cloudfoundry.credhub.generators;

import java.security.KeyPair;

import org.cloudfoundry.credhub.credential.RsaCredentialValue;
import org.cloudfoundry.credhub.requests.RsaGenerationParameters;
import org.cloudfoundry.credhub.utils.CertificateFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RsaGeneratorTest {

  private RsaKeyPairGenerator keyPairGenerator;
  private RsaGenerator subject;
  private FakeKeyPairGenerator fakeKeyPairGenerator;

  private KeyPair keyPair;

  @BeforeEach
  public void beforeEach() throws Exception {
    keyPairGenerator = mock(RsaKeyPairGenerator.class);
    fakeKeyPairGenerator = new FakeKeyPairGenerator();
    keyPair = fakeKeyPairGenerator.generate();
    when(keyPairGenerator.generateKeyPair(anyInt())).thenReturn(keyPair);

    subject = new RsaGenerator(keyPairGenerator);
  }

  @Test
  public void generateCredential_shouldReturnAGeneratedCredential() throws Exception {
    final RsaCredentialValue rsa = subject.generateCredential(new RsaGenerationParameters());

    verify(keyPairGenerator).generateKeyPair(2048);

    assertThat(rsa.getPublicKey(), equalTo(CertificateFormatter.pemOf(keyPair.getPublic())));
    assertThat(rsa.getPrivateKey(), equalTo(CertificateFormatter.pemOf(keyPair.getPrivate())));
  }

  @Test
  public void generateCredential_shouldUseTheProvidedKeyLength() throws Exception {
    final RsaGenerationParameters rsaGenerationParameters = new RsaGenerationParameters();
    rsaGenerationParameters.setKeyLength(4096);

    subject.generateCredential(rsaGenerationParameters);

    verify(keyPairGenerator).generateKeyPair(4096);
  }
}
