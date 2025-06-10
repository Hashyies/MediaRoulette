package me.hash.mediaroulette.utils.media.image_generation;

import java.awt.Color;

public class Theme {
    private String name;
    private String backgroundImage;
    private ColorPalette colorPalette;
    private TextStyle textStyle;
    private BoxStyle boxStyle;

    // Constructors
    public Theme() {}

    public Theme(String name, String backgroundImage, ColorPalette colorPalette,
                 TextStyle textStyle, BoxStyle boxStyle) {
        this.name = name;
        this.backgroundImage = backgroundImage;
        this.colorPalette = colorPalette;
        this.textStyle = textStyle;
        this.boxStyle = boxStyle;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBackgroundImage() { return backgroundImage; }
    public void setBackgroundImage(String backgroundImage) { this.backgroundImage = backgroundImage; }

    public ColorPalette getColorPalette() { return colorPalette; }
    public void setColorPalette(ColorPalette colorPalette) { this.colorPalette = colorPalette; }

    public TextStyle getTextStyle() { return textStyle; }
    public void setTextStyle(TextStyle textStyle) { this.textStyle = textStyle; }

    public BoxStyle getBoxStyle() { return boxStyle; }
    public void setBoxStyle(BoxStyle boxStyle) { this.boxStyle = boxStyle; }

    // Inner classes for theme components
    public static class ColorPalette {
        private String primary;
        private String secondary;
        private String accent;
        private String text;
        private String overlay;
        private String shadow;

        public ColorPalette() {}

        // Getters and Setters
        public String getPrimary() { return primary; }
        public void setPrimary(String primary) { this.primary = primary; }

        public String getSecondary() { return secondary; }
        public void setSecondary(String secondary) { this.secondary = secondary; }

        public String getAccent() { return accent; }
        public void setAccent(String accent) { this.accent = accent; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getOverlay() { return overlay; }
        public void setOverlay(String overlay) { this.overlay = overlay; }

        public String getShadow() { return shadow; }
        public void setShadow(String shadow) { this.shadow = shadow; }

        public Color getPrimaryColor() { return parseColor(primary); }
        public Color getSecondaryColor() { return parseColor(secondary); }
        public Color getAccentColor() { return parseColor(accent); }
        public Color getTextColor() { return parseColor(text); }
        public Color getOverlayColor() { return parseColor(overlay); }
        public Color getShadowColor() { return parseColor(shadow); }

        private Color parseColor(String colorStr) {
            if (colorStr == null || colorStr.isEmpty()) return Color.BLACK;
            try {
                if (colorStr.startsWith("#")) {
                    return Color.decode(colorStr);
                } else if (colorStr.startsWith("rgba(")) {
                    // Parse rgba(r,g,b,a) format
                    String values = colorStr.substring(5, colorStr.length() - 1);
                    String[] parts = values.split(",");
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    float a = Float.parseFloat(parts[3].trim());
                    return new Color(r, g, b, (int)(a * 255));
                }
                return Color.decode(colorStr);
            } catch (Exception e) {
                return Color.BLACK;
            }
        }
    }

    public static class TextStyle {
        private String fontFamily;
        private int fontSize;
        private String fontWeight;
        private boolean antiAliasing;

        public TextStyle() {}

        // Getters and Setters
        public String getFontFamily() { return fontFamily; }
        public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }

        public String getFontWeight() { return fontWeight; }
        public void setFontWeight(String fontWeight) { this.fontWeight = fontWeight; }

        public boolean isAntiAliasing() { return antiAliasing; }
        public void setAntiAliasing(boolean antiAliasing) { this.antiAliasing = antiAliasing; }
    }

    public static class BoxStyle {
        private int cornerRadius;
        private int paddingX;
        private int paddingY;
        private int borderWidth;
        private int shadowOffsetX;
        private int shadowOffsetY;
        private String boxType;

        public BoxStyle() {}

        // Getters and Setters
        public int getCornerRadius() { return cornerRadius; }
        public void setCornerRadius(int cornerRadius) { this.cornerRadius = cornerRadius; }

        public int getPaddingX() { return paddingX; }
        public void setPaddingX(int paddingX) { this.paddingX = paddingX; }

        public int getPaddingY() { return paddingY; }
        public void setPaddingY(int paddingY) { this.paddingY = paddingY; }

        public int getBorderWidth() { return borderWidth; }
        public void setBorderWidth(int borderWidth) { this.borderWidth = borderWidth; }

        public int getShadowOffsetX() { return shadowOffsetX; }
        public void setShadowOffsetX(int shadowOffsetX) { this.shadowOffsetX = shadowOffsetX; }

        public int getShadowOffsetY() { return shadowOffsetY; }
        public void setShadowOffsetY(int shadowOffsetY) { this.shadowOffsetY = shadowOffsetY; }

        public String getBoxType() { return boxType; }
        public void setBoxType(String boxType) { this.boxType = boxType; }
    }
}
