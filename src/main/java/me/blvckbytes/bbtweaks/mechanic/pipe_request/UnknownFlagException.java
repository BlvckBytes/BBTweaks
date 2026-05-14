package me.blvckbytes.bbtweaks.mechanic.pipe_request;

public class UnknownFlagException extends Exception {

  public final String unknownFlag;

  public UnknownFlagException(String unknownFlag) {
    this.unknownFlag = unknownFlag;
  }
}
