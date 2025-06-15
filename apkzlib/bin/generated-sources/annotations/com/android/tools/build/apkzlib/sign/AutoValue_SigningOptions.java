package com.android.tools.build.apkzlib.sign;

import com.android.apksig.util.RunnablesExecutor;
import com.google.common.collect.ImmutableList;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.annotation.Nullable;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_SigningOptions extends SigningOptions {

  private final PrivateKey key;

  private final ImmutableList<X509Certificate> certificates;

  private final boolean v1SigningEnabled;

  private final boolean v2SigningEnabled;

  private final int minSdkVersion;

  private final SigningOptions.Validation validation;

  private final RunnablesExecutor executor;

  private final byte[] sdkDependencyData;

  private AutoValue_SigningOptions(
      PrivateKey key,
      ImmutableList<X509Certificate> certificates,
      boolean v1SigningEnabled,
      boolean v2SigningEnabled,
      int minSdkVersion,
      SigningOptions.Validation validation,
      @Nullable RunnablesExecutor executor,
      @Nullable byte[] sdkDependencyData) {
    this.key = key;
    this.certificates = certificates;
    this.v1SigningEnabled = v1SigningEnabled;
    this.v2SigningEnabled = v2SigningEnabled;
    this.minSdkVersion = minSdkVersion;
    this.validation = validation;
    this.executor = executor;
    this.sdkDependencyData = sdkDependencyData;
  }

  @Override
  public PrivateKey getKey() {
    return key;
  }

  @Override
  public ImmutableList<X509Certificate> getCertificates() {
    return certificates;
  }

  @Override
  public boolean isV1SigningEnabled() {
    return v1SigningEnabled;
  }

  @Override
  public boolean isV2SigningEnabled() {
    return v2SigningEnabled;
  }

  @Override
  public int getMinSdkVersion() {
    return minSdkVersion;
  }

  @Override
  public SigningOptions.Validation getValidation() {
    return validation;
  }

  @Nullable
  @Override
  public RunnablesExecutor getExecutor() {
    return executor;
  }

  @SuppressWarnings("mutable")
  @Nullable
  @Override
  public byte[] getSdkDependencyData() {
    return sdkDependencyData;
  }

  @Override
  public String toString() {
    return "SigningOptions{"
        + "key=" + key + ", "
        + "certificates=" + certificates + ", "
        + "v1SigningEnabled=" + v1SigningEnabled + ", "
        + "v2SigningEnabled=" + v2SigningEnabled + ", "
        + "minSdkVersion=" + minSdkVersion + ", "
        + "validation=" + validation + ", "
        + "executor=" + executor + ", "
        + "sdkDependencyData=" + Arrays.toString(sdkDependencyData)
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof SigningOptions) {
      SigningOptions that = (SigningOptions) o;
      return this.key.equals(that.getKey())
          && this.certificates.equals(that.getCertificates())
          && this.v1SigningEnabled == that.isV1SigningEnabled()
          && this.v2SigningEnabled == that.isV2SigningEnabled()
          && this.minSdkVersion == that.getMinSdkVersion()
          && this.validation.equals(that.getValidation())
          && (this.executor == null ? that.getExecutor() == null : this.executor.equals(that.getExecutor()))
          && Arrays.equals(this.sdkDependencyData, (that instanceof AutoValue_SigningOptions) ? ((AutoValue_SigningOptions) that).sdkDependencyData : that.getSdkDependencyData());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= key.hashCode();
    h$ *= 1000003;
    h$ ^= certificates.hashCode();
    h$ *= 1000003;
    h$ ^= v1SigningEnabled ? 1231 : 1237;
    h$ *= 1000003;
    h$ ^= v2SigningEnabled ? 1231 : 1237;
    h$ *= 1000003;
    h$ ^= minSdkVersion;
    h$ *= 1000003;
    h$ ^= validation.hashCode();
    h$ *= 1000003;
    h$ ^= (executor == null) ? 0 : executor.hashCode();
    h$ *= 1000003;
    h$ ^= Arrays.hashCode(sdkDependencyData);
    return h$;
  }

  static final class Builder extends SigningOptions.Builder {
    private PrivateKey key;
    private ImmutableList<X509Certificate> certificates;
    private boolean v1SigningEnabled;
    private boolean v2SigningEnabled;
    private int minSdkVersion;
    private SigningOptions.Validation validation;
    private RunnablesExecutor executor;
    private byte[] sdkDependencyData;
    private byte set$0;
    Builder() {
    }
    @Override
    public SigningOptions.Builder setKey(PrivateKey key) {
      if (key == null) {
        throw new NullPointerException("Null key");
      }
      this.key = key;
      return this;
    }
    @Override
    public SigningOptions.Builder setCertificates(ImmutableList<X509Certificate> certificates) {
      if (certificates == null) {
        throw new NullPointerException("Null certificates");
      }
      this.certificates = certificates;
      return this;
    }
    @Override
    public SigningOptions.Builder setCertificates(X509Certificate... certificates) {
      this.certificates = ImmutableList.copyOf(certificates);
      return this;
    }
    @Override
    public SigningOptions.Builder setV1SigningEnabled(boolean v1SigningEnabled) {
      this.v1SigningEnabled = v1SigningEnabled;
      set$0 |= 1;
      return this;
    }
    @Override
    public SigningOptions.Builder setV2SigningEnabled(boolean v2SigningEnabled) {
      this.v2SigningEnabled = v2SigningEnabled;
      set$0 |= 2;
      return this;
    }
    @Override
    public SigningOptions.Builder setMinSdkVersion(int minSdkVersion) {
      this.minSdkVersion = minSdkVersion;
      set$0 |= 4;
      return this;
    }
    @Override
    public SigningOptions.Builder setValidation(SigningOptions.Validation validation) {
      if (validation == null) {
        throw new NullPointerException("Null validation");
      }
      this.validation = validation;
      return this;
    }
    @Override
    public SigningOptions.Builder setExecutor(@Nullable RunnablesExecutor executor) {
      this.executor = executor;
      return this;
    }
    @Override
    public SigningOptions.Builder setSdkDependencyData(@Nullable byte[] sdkDependencyData) {
      this.sdkDependencyData = sdkDependencyData;
      return this;
    }
    @Override
    SigningOptions autoBuild() {
      if (set$0 != 7
          || this.key == null
          || this.certificates == null
          || this.validation == null) {
        StringBuilder missing = new StringBuilder();
        if (this.key == null) {
          missing.append(" key");
        }
        if (this.certificates == null) {
          missing.append(" certificates");
        }
        if ((set$0 & 1) == 0) {
          missing.append(" v1SigningEnabled");
        }
        if ((set$0 & 2) == 0) {
          missing.append(" v2SigningEnabled");
        }
        if ((set$0 & 4) == 0) {
          missing.append(" minSdkVersion");
        }
        if (this.validation == null) {
          missing.append(" validation");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_SigningOptions(
          this.key,
          this.certificates,
          this.v1SigningEnabled,
          this.v2SigningEnabled,
          this.minSdkVersion,
          this.validation,
          this.executor,
          this.sdkDependencyData);
    }
  }

}
