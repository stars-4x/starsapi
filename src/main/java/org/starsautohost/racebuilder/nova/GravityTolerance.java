package org.starsautohost.racebuilder.nova;

/*
 * Modified from Stars Nova project
 */
public class GravityTolerance extends  EnvironmentTolerance{
    public GravityTolerance() 
    { 
    }

    // Calculate the minimum and maximum values of the tolerance ranges
    // expressed as a percentage of the total range. 
    // Gravity was in the range 0 to 10, UI values will differ, see Nova.Common.Gravity!
    @Override
    protected int makeInternalValue(double value)
    {
        return (int)(value * 10);
    }

    @Override
    protected String format(int value)
    {
        return Gravity.formatWithUnit(value);
    }
}
