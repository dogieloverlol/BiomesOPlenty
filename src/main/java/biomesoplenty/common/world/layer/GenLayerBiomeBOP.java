/*******************************************************************************
 * Copyright 2015-2016, the Biomes O' Plenty Team
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * 
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/

package biomesoplenty.common.world.layer;

import biomesoplenty.api.enums.BOPClimates;
import biomesoplenty.api.generation.BOPGenLayer;
import biomesoplenty.common.init.ModBiomes;
import biomesoplenty.common.world.BOPWorldSettings;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.common.BiomeManager.BiomeType;

public class GenLayerBiomeBOP extends BOPGenLayer
{
    
    private BOPWorldSettings settings;
    private GenLayer landMassLayer;
    private GenLayerClimate climateLayer;
    
    public GenLayerBiomeBOP(long seed, GenLayer landMassLayer, GenLayerClimate climateLayer, BOPWorldSettings settings)
    {
        super(seed);
        this.landMassLayer = landMassLayer;
        this.climateLayer = climateLayer;
        this.settings = settings;
        
        // debugging
        //BOPClimates.printWeights();
    }
    
    // Get array of biome IDs covering the requested area
    @Override
    public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight)
    {
        int[] landSeaValues = this.landMassLayer.getInts(areaX, areaY, areaWidth, areaHeight);
        int[] climateValues = this.climateLayer.getInts(areaX, areaY, areaWidth, areaHeight);
        int[] out = IntCache.getIntCache(areaWidth * areaHeight);
        
        for (int x = 0; x < areaHeight; ++x)
        {
            for (int z = 0; z < areaWidth; ++z)
            {
                int index = z + x * areaWidth;
                this.initChunkSeed((long)(z + areaX), (long)(x + areaY));
                int landSeaVal = landSeaValues[index];
                int climateOrdinal = climateValues[index];
                
                BOPClimates climate;
                try {
                    climate = BOPClimates.lookup(climateOrdinal);
                } catch (java.lang.ArrayIndexOutOfBoundsException e) {
                    // This shouldn't happen - but apparently it (rarely) does (https://github.com/Glitchfiend/BiomesOPlenty/issues/983)
                    // If it does it means that something weird happened with the climate layer / lookup
                    // Rethrow with hopefully a more useful message
                    String msg = "Climate lookup failed climateOrdinal: " + climateOrdinal + " climate layer mapping: " + climateLayer.debugClimateMappingInts();
                    throw new java.lang.RuntimeException(msg,e);
                }
                
                // At this point, oceans and land have been assigned, and so have mushroom islands
                if (landSeaVal == Biome.getIdForBiome(Biomes.DEEP_OCEAN))
                {
                    out[index] = Biome.getIdForBiome(climate.getRandomOceanBiome(this, true));
                }
                else if ((landSeaVal == Biome.getIdForBiome(Biomes.MUSHROOM_ISLAND) || ModBiomes.islandBiomesMap.containsKey(landSeaVal)) && climate.biomeType != BiomeType.ICY)
                {
                    // keep islands, unless it's in an icy climate in which case, replace
                    out[index] = landSeaVal;
                }
                else if (landSeaVal == 0)
                {
                    out[index] = Biome.getIdForBiome(climate.getRandomOceanBiome(this, false));
                }
                else
                {
                    out[index] = Biome.getIdForBiome(climate.getRandomBiome(this));
                }
            }
        }

        return out;
    }

}
