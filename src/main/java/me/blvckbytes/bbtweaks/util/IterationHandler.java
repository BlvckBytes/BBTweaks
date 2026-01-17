package me.blvckbytes.bbtweaks.util;

@FunctionalInterface
public interface IterationHandler<DataType> {

  IterationDecision handle(DataType data);

}
