package org.starsautohost.racebuilder.nova;

/*
 * Modified from Stars Nova project
 */
public class TemperatureTolerance extends EnvironmentTolerance {
    /// <summary>
    /// Default constructor, required for serialization?.
    /// </summary>
    public TemperatureTolerance() 
    { 
    }

    // Calculate the minimum and maximum values of the tolerance ranges
    // expressed as a percentage of the total range. 
    // Temperature is in the range -200 to 200.
    @Override
    protected int makeInternalValue(double value)
    {
        return (int)((200 + value) / 4);
    }

    @Override
    protected String format(int value)
    {
        return Temperature.formatWithUnit(value);
    }
}
