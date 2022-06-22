package net.fabricmc.tbmod.util;

import net.fabricmc.tbmod.items.MultiTool;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class ToolSettings {
    public static final String ERASE_MODE = "erase";
    public static final String BUILD_MODE = "build";
    public static final String PAINT_MODE = "paint";
    public static final String[] MODES = {ERASE_MODE, BUILD_MODE, PAINT_MODE};
    public static final DistanceComparator MODE_COMPARATOR = new DistanceComparator(MODES, 1000);

    public static final String SPHERE_SHAPE = "sphere";
    public static final String CUBIC_SHAPE = "cube";
    public static final String LINE_SHAPE = "line";
    public static final String BEAM_SHAPE = "beam";
    public static final String[] SHAPES = {SPHERE_SHAPE, CUBIC_SHAPE, LINE_SHAPE, BEAM_SHAPE};
    public static final DistanceComparator SHAPE_COMPARATOR = new DistanceComparator(SHAPES, 1000);

    public static final String MATERIAL_PARAMETER = "material";
    public static final String MODE_PARAMETER = "mode";
    public static final String SHAPE_PARAMETER = "shape";
    public static final String SIZE_PARAMETER = "size";
    public static final String OPACITY_PARAMETER = "opacity";
    public static final String MASKING_PARAMETER = "masking";
    public static final String CHECK_VOLUMES_PARAMETER = "volumeCheck";
    public static final String GRADIENT_PARAMETER = "gradient";
    public static final String STATE_MATCH_PARAMETER = "matchState";
    public static final String SURFACE_PARAMETER = "surface";
    public static final String[] PARAMETERS = {MATERIAL_PARAMETER, MODE_PARAMETER, SHAPE_PARAMETER,
            SIZE_PARAMETER, OPACITY_PARAMETER, MASKING_PARAMETER, CHECK_VOLUMES_PARAMETER, GRADIENT_PARAMETER,
            STATE_MATCH_PARAMETER, SURFACE_PARAMETER};
    public static final DistanceComparator PARAMETER_COMPARATOR = new DistanceComparator(PARAMETERS, 5);

    public static final double CUBIC_SIZE_LIMIT = 256.0;
    public static final double LINEAR_SIZE_LIMIT = 1024.0;

    public String mode = PAINT_MODE;
    public String shape = SPHERE_SHAPE;
    public double minSize = 0.0;
    public double maxSize = 0.0;
    public double size = 3.5;
    public int opacity = 100;
    public boolean masking = true;
    public boolean gradient = false;
    public boolean volumeCheck = false;
    public BlockState material = Blocks.STONE.getDefaultState();
    public boolean matchState = false;
    public boolean surfaceOnly = false;

    public ToolSettings(ItemStack stack) {
        if (!stack.hasNbt()) {return;}
        NbtCompound nbt = stack.getNbt();
        //if (Objects.equals(nbt, null)) return;
        if (!nbt.contains(MATERIAL_PARAMETER)) {return;}
        this.material = NbtHelper.toBlockState(nbt.getCompound(MATERIAL_PARAMETER));
        this.mode = nbt.getString(MODE_PARAMETER);
        this.shape = nbt.getString(SHAPE_PARAMETER);
        this.size = nbt.getDouble(SIZE_PARAMETER);
        if (nbt.get(SIZE_PARAMETER).getClass().equals(NbtCompound.class)) {
            this.minSize = nbt.getCompound(SIZE_PARAMETER).getDouble("minSize");
            this.maxSize = nbt.getCompound(SIZE_PARAMETER).getDouble("maxSize");
            this.size = ThreadLocalRandom.current().nextDouble(this.minSize, Math.max(this.minSize + 0.0001, this.maxSize));
        } else {
            this.minSize = 0.0;
            this.maxSize = 0.0;
            this.size = nbt.getDouble(SIZE_PARAMETER);
        }
        this.opacity = nbt.getInt(OPACITY_PARAMETER);
        this.masking = nbt.getBoolean(MASKING_PARAMETER);
        this.volumeCheck = nbt.getBoolean(CHECK_VOLUMES_PARAMETER);
        this.gradient = nbt.getBoolean(GRADIENT_PARAMETER);
        this.matchState = nbt.getBoolean(STATE_MATCH_PARAMETER);
        this.surfaceOnly = nbt.getBoolean(SURFACE_PARAMETER);
    }

    public ToolSettings() {}

    public void writeTo(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.put(MATERIAL_PARAMETER, NbtHelper.fromBlockState(material));
        nbt.putString(MODE_PARAMETER, mode);
        nbt.putString(SHAPE_PARAMETER, shape);
        if (minSize == 0.0 && maxSize == 0.0) {
            nbt.putDouble(SIZE_PARAMETER, size);
        } else {
            NbtCompound sizeCompound = new NbtCompound();
            sizeCompound.putDouble("minSize", minSize);
            sizeCompound.putDouble("maxSize", maxSize);
            nbt.put(SIZE_PARAMETER, sizeCompound);
        }
        nbt.putInt(OPACITY_PARAMETER, opacity);
        nbt.putBoolean(MASKING_PARAMETER, masking);
        nbt.putBoolean(CHECK_VOLUMES_PARAMETER, volumeCheck);
        nbt.putBoolean(GRADIENT_PARAMETER, gradient);
        nbt.putBoolean(STATE_MATCH_PARAMETER, matchState);
        nbt.putBoolean(SURFACE_PARAMETER, surfaceOnly);
    }

    public void applyCommand(String itemName) {
        if (!itemName.contains("/")) {return;}
        if (itemName.endsWith("/")) {return;}
        String[] parts = itemName.split("/");
        String[] commands = parts[parts.length - 1].split(";");
        for (String command : commands) {
            command = command.replaceAll(" ", "");
            String argument = command.split("=")[0];
            if (command.split("=").length < 2) {continue;}
            String value = command.split("=")[1];
            if (Objects.equals(argument, MATERIAL_PARAMETER)) {
                if (value.contains(":")) {
                    this.material = Registry.BLOCK.get(
                            new Identifier(value.split(":")[0], value.split(":")[1])).getDefaultState();
                } else {
                    this.material = Registry.BLOCK.get(new Identifier("minecraft", value)).getDefaultState();
                }
            }
            argument = PARAMETER_COMPARATOR.getNearest(argument);
            try {
                if (Objects.equals(argument, MODE_PARAMETER)) {
                    this.mode = MODE_COMPARATOR.getNearest(value);
                }
                if (Objects.equals(argument, SHAPE_PARAMETER)) {
                    this.shape = SHAPE_COMPARATOR.getNearest(value);
                }
                if (Objects.equals(argument, SIZE_PARAMETER)) {
                    if (value.startsWith("(") && value.endsWith(")")) {
                        this.minSize = Double.parseDouble(value.split(",")[0].replace("(", ""));
                        this.maxSize = Double.parseDouble(value.split(",")[1].replace(")", ""));
                        this.size = ThreadLocalRandom.current().nextDouble(this.minSize,
                                Math.max(this.minSize + 0.0001, this.maxSize));
                    } else {
                        this.size = Double.parseDouble(value);
                        this.minSize = 0.0;
                        this.maxSize = 0.0;
                    }
                }
                if (Objects.equals(argument, OPACITY_PARAMETER)) {
                    this.opacity = Integer.parseInt(value);
                }
                if (Objects.equals(argument, MASKING_PARAMETER)) {
                    this.masking = Boolean.parseBoolean(value);
                }
                if (Objects.equals(argument, CHECK_VOLUMES_PARAMETER)) {
                    this.volumeCheck = Boolean.parseBoolean(value);
                }
                if (Objects.equals(argument, GRADIENT_PARAMETER)) {
                    this.gradient = Boolean.parseBoolean(value);
                }
                if (Objects.equals(argument, STATE_MATCH_PARAMETER)) {
                    this.matchState = Boolean.parseBoolean(value);
                }
                if (Objects.equals(argument, SURFACE_PARAMETER)) {
                    this.surfaceOnly = Boolean.parseBoolean(value);
                }
            } catch (Exception exception) {}
        }
    }
}
