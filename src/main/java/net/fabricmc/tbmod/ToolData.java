package net.fabricmc.tbmod;


import java.util.Objects;

public class ToolData {
    public static final String SEP = "/";
    public static final String PREFIX = "TL/";

    public static final String BRUSH_MODE = "PAINT";
    public static final String SCULPT_MODE = "BUILD";
    public static final String ERASE_MODE = "ERASE";

    public static final String MASK_PREFIX = "MSK_";
    public static final String SIZE_PREFIX = "SZE_";
    public static final String PERCENTAGE_PREFIX = "OCT_";
    public static final String GRAD_PREFIX = "GRD_";

    public String mode;
    public boolean doMasking;
    public double size;
    public boolean doGradient;
    public int percentage;

    public ToolData(String name) {
        this.mode = BRUSH_MODE;
        this.doMasking = true;
        this.size = 3;
        this.doGradient = false;
        this.percentage = 100;
        String[] parts = name.split(SEP);
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (Objects.equals(part, BRUSH_MODE)) {this.mode = BRUSH_MODE;}
            if (Objects.equals(part, SCULPT_MODE)) {this.mode = SCULPT_MODE;}
            if (Objects.equals(part, ERASE_MODE)) {this.mode = ERASE_MODE;}
            if (part.startsWith(MASK_PREFIX)) {this.doMasking = part.endsWith("true");}
            if (part.startsWith(GRAD_PREFIX)) {this.doGradient = part.endsWith("true");}
            if (part.startsWith(SIZE_PREFIX)) {this.size = Double.parseDouble(part.split("_")[1]);}
            if (part.startsWith(PERCENTAGE_PREFIX)) {this.percentage = Integer.parseInt(part.split("_")[1]);}
        }
        //this.mode = SCULPT_MODE;
    }

    public String makeName() {
        return PREFIX + this.mode + SEP + SIZE_PREFIX + Double.toString(this.size) + SEP + PERCENTAGE_PREFIX +
                Integer.toString(this.percentage) + SEP + MASK_PREFIX + Boolean.toString(this.doMasking) + SEP +
                GRAD_PREFIX + Boolean.toString(this.doGradient);
    }
}
