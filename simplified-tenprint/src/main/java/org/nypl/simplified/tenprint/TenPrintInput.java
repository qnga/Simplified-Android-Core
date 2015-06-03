package org.nypl.simplified.tenprint;

import org.nypl.simplified.assertions.Assertions;

import com.io7m.jnull.NullCheck;

/**
 * An immutable set of parameters to the 10 PRINT algorithm.
 */

@SuppressWarnings({ "boxing", "synthetic-access" }) public final class TenPrintInput
{
  private static final class Builder implements TenPrintInputBuilderType
  {
    private String  author;
    private float   base_brightness;
    private float   base_saturation;
    private float   color_distance;
    private int     cover_height;
    private float   grid_scale;
    private boolean invert;
    private int     margin;
    private int     shape_thickness;
    private String  title;

    Builder()
    {
      this.shape_thickness = 10;
      this.margin = 2;
      this.base_saturation = 1.0f;
      this.base_brightness = 0.9f;
      this.color_distance = 100.0f;
      this.cover_height = 300;
      this.invert = false;
      this.title = "";
      this.author = "";
      this.grid_scale = 1.0f;
    }

    @Override public TenPrintInput build()
    {
      return new TenPrintInput(
        this.shape_thickness,
        this.margin,
        this.base_saturation,
        this.base_brightness,
        this.color_distance,
        this.cover_height,
        this.invert,
        this.title,
        this.author,
        this.grid_scale);
    }

    @Override public void setAuthor(
      final String in_author)
    {
      this.author = NullCheck.notNull(in_author);
    }

    @Override public void setBaseBrightness(
      final float in_base_brightness)
    {
      Assertions.checkPrecondition(
        in_base_brightness >= 0.0,
        "Brightness %f must be >= 0.0",
        in_base_brightness);
      Assertions.checkPrecondition(
        in_base_brightness <= 1.0,
        "Brightness %f must be <= 1.0",
        in_base_brightness);
      this.base_brightness = in_base_brightness;
    }

    @Override public void setBaseSaturation(
      final float in_base_saturation)
    {
      Assertions.checkPrecondition(
        in_base_saturation >= 0.0,
        "Saturation %f must be >= 0.0",
        in_base_saturation);
      Assertions.checkPrecondition(
        in_base_saturation <= 1.0,
        "Saturation %f must be <= 1.0",
        in_base_saturation);
      this.base_saturation = in_base_saturation;
    }

    @Override public void setColorDistance(
      final float in_color_distance)
    {
      Assertions.checkPrecondition(
        in_color_distance >= 0.0,
        "Color distance %f must be >= 0.0",
        in_color_distance);
      Assertions.checkPrecondition(
        in_color_distance <= 360.0,
        "Color distance %f must be <= %f",
        in_color_distance,
        360.0);
      this.color_distance = in_color_distance;
    }

    @Override public void setCoverHeight(
      final int in_cover_height)
    {
      Assertions.checkPrecondition(
        in_cover_height >= 10,
        "Cover height %d must be >= 10",
        in_cover_height);
      this.cover_height = in_cover_height;
    }

    @Override public void setGridScale(
      final float in_grid_scale)
    {
      Assertions.checkPrecondition(
        in_grid_scale > 0.1,
        "Grid scale %f must be > %f",
        in_grid_scale,
        0.1);
      this.grid_scale = in_grid_scale;
    }

    @Override public void setInvert(
      final boolean in_invert)
    {
      this.invert = in_invert;
    }

    @Override public void setMargin(
      final int in_margin)
    {
      Assertions.checkPrecondition(
        in_margin >= 1,
        "Margin %d must be >= 1",
        in_margin);
      Assertions.checkPrecondition(
        in_margin <= 10,
        "Margin %d must be <= 10",
        in_margin);
      this.margin = in_margin;
    }

    @Override public void setShapeThickness(
      final int in_shape_thickness)
    {
      Assertions.checkPrecondition(
        in_shape_thickness >= 1,
        "Shape thickness %d must be >= 1",
        in_shape_thickness);
      Assertions.checkPrecondition(
        in_shape_thickness <= 30,
        "Shape thickness %d must be <= 30",
        in_shape_thickness);
      this.shape_thickness = in_shape_thickness;
    }

    @Override public void setTitle(
      final String in_title)
    {
      this.title = NullCheck.notNull(in_title);
    }
  }

  public static TenPrintInputBuilderType newBuilder()
  {
    return new Builder();
  }

  private final String  author;
  private final float   base_brightness;
  private final float   base_saturation;
  private final float   color_distance;
  private final int     cover_height;
  private final int     cover_width;
  private final float   grid_scale;
  private final boolean invert;
  private final int     margin;
  private final int     shape_thickness;
  private final String  title;

  private TenPrintInput(
    final int in_shape_thickness,
    final int in_margin,
    final float in_base_saturation,
    final float in_base_brightness,
    final float in_color_distance,
    final int in_cover_height,
    final boolean in_invert,
    final String in_title,
    final String in_author,
    final float in_grid_scale)
  {
    Assertions.checkPrecondition(
      in_shape_thickness >= 1,
      "Shape thickness %d must be >= 1",
      in_shape_thickness);
    Assertions.checkPrecondition(
      in_shape_thickness <= 30,
      "Shape thickness %d must be <= 30",
      in_shape_thickness);
    this.shape_thickness = in_shape_thickness;

    Assertions.checkPrecondition(
      in_margin >= 1,
      "Margin %d must be >= 1",
      in_margin);
    Assertions.checkPrecondition(
      in_margin <= 10,
      "Margin %d must be <= 10",
      in_margin);
    this.margin = in_margin;

    Assertions.checkPrecondition(
      in_base_saturation >= 0.0,
      "Saturation %f must be >= 0.0",
      in_base_saturation);
    Assertions.checkPrecondition(
      in_base_saturation <= 1.0,
      "Saturation %f must be <= 1.0",
      in_base_saturation);
    this.base_saturation = in_base_saturation;

    Assertions.checkPrecondition(
      in_base_brightness >= 0.0,
      "Brightness %f must be >= 0.0",
      in_base_brightness);
    Assertions.checkPrecondition(
      in_base_brightness <= 1.0,
      "Brightness %f must be <= 1.0",
      in_base_brightness);
    this.base_brightness = in_base_brightness;

    Assertions.checkPrecondition(
      in_color_distance >= 0.0,
      "Color distance %f must be >= 0.0",
      in_color_distance);
    Assertions.checkPrecondition(
      in_color_distance <= 360.0,
      "Color distance %f must be <= %f",
      in_color_distance,
      360.0);
    this.color_distance = in_color_distance;

    Assertions.checkPrecondition(
      in_cover_height >= 10,
      "Cover height %d must be >= 10",
      in_cover_height);
    this.cover_height = in_cover_height;
    this.cover_width = (int) (in_cover_height / 1.5);

    this.invert = in_invert;
    this.title = NullCheck.notNull(in_title);
    this.author = NullCheck.notNull(in_author);

    Assertions.checkPrecondition(
      in_grid_scale > 0.1,
      "Grid scale %f must be > %f",
      in_grid_scale,
      0.1);
    this.grid_scale = in_grid_scale;
  }

  public String getAuthor()
  {
    return this.author;
  }

  public float getBaseBrightness()
  {
    return this.base_brightness;
  }

  public float getBaseSaturation()
  {
    return this.base_saturation;
  }

  public float getColorDistance()
  {
    return this.color_distance;
  }

  public int getCoverHeight()
  {
    return this.cover_height;
  }

  public int getCoverWidth()
  {
    return this.cover_width;
  }

  public float getGridScale()
  {
    return this.grid_scale;
  }

  public int getMargin()
  {
    return this.margin;
  }

  public int getShapeThickness()
  {
    return this.shape_thickness;
  }

  public String getTitle()
  {
    return this.title;
  }

  public boolean invert()
  {
    return this.invert;
  }
}
