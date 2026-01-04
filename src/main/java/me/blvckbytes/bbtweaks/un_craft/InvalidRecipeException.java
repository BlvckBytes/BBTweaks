package me.blvckbytes.bbtweaks.un_craft;

public class InvalidRecipeException extends RuntimeException {

  public final String reason;

  public InvalidRecipeException(String reason) {
    this.reason = reason;
  }
}
