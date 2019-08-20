package amas_traffic.amak;

import msi.gama.metamodel.shape.ILocation;

public class MobileEntitySnapshot {
  private final String mobileEntityName;
  private final ILocation mobileEntityLocation;
  private final double mobileEntitySpeed;

  public MobileEntitySnapshot(final String mobileEntityName, final ILocation mobileEntityLocation,
      final double mobileEntitySpeed) {
    this.mobileEntityName = mobileEntityName;
    this.mobileEntityLocation = mobileEntityLocation;
    this.mobileEntitySpeed = mobileEntitySpeed;
  }

  public String getMobileEntityName() {
    return this.mobileEntityName;
  }

  public ILocation getMobileEntityLocation() {
    return this.mobileEntityLocation;
  }

  public double getMobileEntitySpeed() {
    return this.mobileEntitySpeed;
  }

  @Override
  public String toString() {
    return String.format("%s;%s;%f", this.mobileEntityName, this.mobileEntityLocation.toString(),
        this.mobileEntitySpeed);
  }
}
