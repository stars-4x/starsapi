package org.starsautohost.racebuilder.nova;
/*
 * Modified from Stars Nova project
 */
public class Temperature{
     public static String formatWithUnit(int value){
         return format(value) + getUnit();
     }

     public static String format(int value){
         // always trunkate temperature for GUI values to 0 decimal points
         return ""+barPositionToEnvironmentValue(value);
     }

     public static String getUnit()
     {
         return "°C";
     }
     
     public static double barPositionToEnvironmentValue(int pos){
         return (pos * 4) - 200;
     }       
}
