package dev.itzgamesgd.moreminecrafts.progression;

/** Centered uniform scaling and pointer mapping for fixed-layout screens. */
public record ViewportScale(double scale, double left, double top) {
    public static ViewportScale fit(double viewportWidth, double viewportHeight,
                                    double contentWidth, double contentHeight, double padding) {
        double availableWidth = Math.max(1.0D, viewportWidth - 2.0D * padding);
        double availableHeight = Math.max(1.0D, viewportHeight - 2.0D * padding);
        double scale = Math.min(1.0D, Math.min(availableWidth / contentWidth, availableHeight / contentHeight));
        return new ViewportScale(scale, (viewportWidth - contentWidth * scale) / 2.0D,
                (viewportHeight - contentHeight * scale) / 2.0D);
    }

    public double screenX(double contentX, double frameLeft) {
        return left + (contentX - frameLeft) * scale;
    }

    public double screenY(double contentY, double frameTop) {
        return top + (contentY - frameTop) * scale;
    }

    public double contentX(double screenX, double frameLeft) {
        return frameLeft + (screenX - left) / scale;
    }

    public double contentY(double screenY, double frameTop) {
        return frameTop + (screenY - top) / scale;
    }
}
