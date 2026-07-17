package me.blvckbytes.bbtweaks.mechanic.pool_crafter;

import org.bukkit.Material;

import java.util.List;

public interface SimilarMaterialsResolver {

  List<Material> resolveSimilarMaterials(Material material);

}
