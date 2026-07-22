package me.blvckbytes.bbtweaks.pipes.search.command;

public class UnknownCommandFlagException extends Exception {

  public final String flagValue;

  public UnknownCommandFlagException(String flagValue) {
    this.flagValue = flagValue;
  }
}
