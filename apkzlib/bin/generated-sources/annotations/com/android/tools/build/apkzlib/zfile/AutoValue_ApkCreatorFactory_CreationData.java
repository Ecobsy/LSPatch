package com.android.tools.build.apkzlib.zfile;

import com.android.tools.build.apkzlib.sign.SigningOptions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import java.io.File;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ApkCreatorFactory_CreationData extends ApkCreatorFactory.CreationData {

  private final File apkPath;

  private final Optional<SigningOptions> signingOptions;

  private final String builtBy;

  private final String createdBy;

  private final NativeLibrariesPackagingMode nativeLibrariesPackagingMode;

  private final Predicate<String> noCompressPredicate;

  private final boolean incremental;

  private AutoValue_ApkCreatorFactory_CreationData(
      File apkPath,
      Optional<SigningOptions> signingOptions,
      @Nullable String builtBy,
      @Nullable String createdBy,
      NativeLibrariesPackagingMode nativeLibrariesPackagingMode,
      Predicate<String> noCompressPredicate,
      boolean incremental) {
    this.apkPath = apkPath;
    this.signingOptions = signingOptions;
    this.builtBy = builtBy;
    this.createdBy = createdBy;
    this.nativeLibrariesPackagingMode = nativeLibrariesPackagingMode;
    this.noCompressPredicate = noCompressPredicate;
    this.incremental = incremental;
  }

  @Override
  public File getApkPath() {
    return apkPath;
  }

  @Nonnull
  @Override
  public Optional<SigningOptions> getSigningOptions() {
    return signingOptions;
  }

  @Nullable
  @Override
  public String getBuiltBy() {
    return builtBy;
  }

  @Nullable
  @Override
  public String getCreatedBy() {
    return createdBy;
  }

  @Override
  public NativeLibrariesPackagingMode getNativeLibrariesPackagingMode() {
    return nativeLibrariesPackagingMode;
  }

  @Override
  public Predicate<String> getNoCompressPredicate() {
    return noCompressPredicate;
  }

  @Override
  public boolean isIncremental() {
    return incremental;
  }

  @Override
  public String toString() {
    return "CreationData{"
        + "apkPath=" + apkPath + ", "
        + "signingOptions=" + signingOptions + ", "
        + "builtBy=" + builtBy + ", "
        + "createdBy=" + createdBy + ", "
        + "nativeLibrariesPackagingMode=" + nativeLibrariesPackagingMode + ", "
        + "noCompressPredicate=" + noCompressPredicate + ", "
        + "incremental=" + incremental
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ApkCreatorFactory.CreationData) {
      ApkCreatorFactory.CreationData that = (ApkCreatorFactory.CreationData) o;
      return this.apkPath.equals(that.getApkPath())
          && this.signingOptions.equals(that.getSigningOptions())
          && (this.builtBy == null ? that.getBuiltBy() == null : this.builtBy.equals(that.getBuiltBy()))
          && (this.createdBy == null ? that.getCreatedBy() == null : this.createdBy.equals(that.getCreatedBy()))
          && this.nativeLibrariesPackagingMode.equals(that.getNativeLibrariesPackagingMode())
          && this.noCompressPredicate.equals(that.getNoCompressPredicate())
          && this.incremental == that.isIncremental();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= apkPath.hashCode();
    h$ *= 1000003;
    h$ ^= signingOptions.hashCode();
    h$ *= 1000003;
    h$ ^= (builtBy == null) ? 0 : builtBy.hashCode();
    h$ *= 1000003;
    h$ ^= (createdBy == null) ? 0 : createdBy.hashCode();
    h$ *= 1000003;
    h$ ^= nativeLibrariesPackagingMode.hashCode();
    h$ *= 1000003;
    h$ ^= noCompressPredicate.hashCode();
    h$ *= 1000003;
    h$ ^= incremental ? 1231 : 1237;
    return h$;
  }

  static final class Builder extends ApkCreatorFactory.CreationData.Builder {
    private File apkPath;
    private Optional<SigningOptions> signingOptions = Optional.absent();
    private String builtBy;
    private String createdBy;
    private NativeLibrariesPackagingMode nativeLibrariesPackagingMode;
    private Predicate<String> noCompressPredicate;
    private boolean incremental;
    private byte set$0;
    Builder() {
    }
    @Override
    public ApkCreatorFactory.CreationData.Builder setApkPath(File apkPath) {
      if (apkPath == null) {
        throw new NullPointerException("Null apkPath");
      }
      this.apkPath = apkPath;
      return this;
    }
    @Override
    public ApkCreatorFactory.CreationData.Builder setSigningOptions(SigningOptions signingOptions) {
      this.signingOptions = Optional.of(signingOptions);
      return this;
    }
    @Override
    public ApkCreatorFactory.CreationData.Builder setBuiltBy(@Nullable String builtBy) {
      this.builtBy = builtBy;
      return this;
    }
    @Override
    public ApkCreatorFactory.CreationData.Builder setCreatedBy(@Nullable String createdBy) {
      this.createdBy = createdBy;
      return this;
    }
    @Override
    public ApkCreatorFactory.CreationData.Builder setNativeLibrariesPackagingMode(NativeLibrariesPackagingMode nativeLibrariesPackagingMode) {
      if (nativeLibrariesPackagingMode == null) {
        throw new NullPointerException("Null nativeLibrariesPackagingMode");
      }
      this.nativeLibrariesPackagingMode = nativeLibrariesPackagingMode;
      return this;
    }
    @Override
    public ApkCreatorFactory.CreationData.Builder setNoCompressPredicate(Predicate<String> noCompressPredicate) {
      if (noCompressPredicate == null) {
        throw new NullPointerException("Null noCompressPredicate");
      }
      this.noCompressPredicate = noCompressPredicate;
      return this;
    }
    @Override
    public ApkCreatorFactory.CreationData.Builder setIncremental(boolean incremental) {
      this.incremental = incremental;
      set$0 |= 1;
      return this;
    }
    @Override
    ApkCreatorFactory.CreationData autoBuild() {
      if (set$0 != 1
          || this.apkPath == null
          || this.nativeLibrariesPackagingMode == null
          || this.noCompressPredicate == null) {
        StringBuilder missing = new StringBuilder();
        if (this.apkPath == null) {
          missing.append(" apkPath");
        }
        if (this.nativeLibrariesPackagingMode == null) {
          missing.append(" nativeLibrariesPackagingMode");
        }
        if (this.noCompressPredicate == null) {
          missing.append(" noCompressPredicate");
        }
        if ((set$0 & 1) == 0) {
          missing.append(" incremental");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_ApkCreatorFactory_CreationData(
          this.apkPath,
          this.signingOptions,
          this.builtBy,
          this.createdBy,
          this.nativeLibrariesPackagingMode,
          this.noCompressPredicate,
          this.incremental);
    }
  }

}
