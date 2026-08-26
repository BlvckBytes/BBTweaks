package me.blvckbytes.bbtweaks.item_piling;

import org.jetbrains.annotations.Nullable;

public interface PileEntityMetadataKeeper {

  void storePileMetadata(int entityId, AmountAndType data);

  @Nullable AmountAndType getPileMetadata(int entityId);

}
