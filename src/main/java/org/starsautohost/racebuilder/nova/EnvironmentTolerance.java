package org.starsautohost.racebuilder.nova;

/*
 * Modified from Stars Nova project
 */
public class EnvironmentTolerance {
     private final String MinInternalIdentifier = "MinInternal";
     private final String MaxInternalIdentifier = "MaxInternal";
     private final String MinToleranceIdentifier = "MinTolerance";
     private final String MaxToleranceIdentifier = "MaxTolerance";
     private final String MinIdentifier = "Min";
     private final String MaxIdentifier = "Max";
     private final String ImmuneIdentifier = "Immune";

     private int minimumInternalValue = 15;
     private int maximumInternalValue = 85;
     private boolean immune = false;

     /// <summary>
     /// Default constructor, required for serialization.
     /// </summary>
     public EnvironmentTolerance() 
     { 
     }

     /// <summary>
     /// Return the Median value of an integer range.
     /// </summary>
     /// <remarks>
     /// FIXME (priority 3) - Mathematically this finds the mean, which in some
     /// circumstances is different from the Median. 
     /// </remarks>
     public int getMedian()
     {
         return (int)(((getMaximumValue() - getMinimumValue()) / 2) + getMinimumValue());
     }

     public int getMaximumValue(){
    	 return maximumInternalValue;
     }
     public void setMaximumValue(int value){
    	 maximumInternalValue = value;
     }
     public int getMinimumValue(){
    	 return minimumInternalValue;
     }
     public void setMinimumValue(int value){
    	 minimumInternalValue = value;
     }

     public boolean isImmune(){
    	 return immune;
     }
     public void setImmune(boolean immune){
    	 this.immune = immune;
     }

     /// <summary>
     /// Return the optimum level as a percentage * 100 for this race Environment.
     /// </summary>
     public int getOptimumLevel()
     {
    	 return getMedian();
     }

     /// <summary>
     /// Calculate the minimum and maximum values of the tolerance ranges
     /// expressed as a percentage of the total range * 100.  
     /// </summary>
     /// <param name="value"></param>
     /// <returns></returns>
     protected int makeInternalValue(double value)
     {
         return 0;
     }

     protected String format(int value)
     {
         return "N/A";
     }

     /// <summary>
     /// Load from XML.
     /// </summary>
     /// <param name="node">The node is a "EnvironmentTolerance" <see cref="XmlNode"/> in 
     /// a Nova component definition file (xml document).
     /// </param>
     /*
     public void FromXml(XmlNode node)
     {
         XmlNode subnode = node.FirstChild;
         while (subnode != null)
         {
             try
             {
                 switch (subnode.Name)
                 {
                     case MinIdentifier:
                         MinimumValue = MakeInternalValue(double.Parse(((XmlText)subnode.FirstChild).Value, System.Globalization.CultureInfo.InvariantCulture));
                         break;
                     case MaxIdentifier:
                         MaximumValue = MakeInternalValue(double.Parse(((XmlText)subnode.FirstChild).Value, System.Globalization.CultureInfo.InvariantCulture));
                         break;
                     case MinInternalIdentifier:
                         MinimumValue = int.Parse(((XmlText)subnode.FirstChild).Value, System.Globalization.CultureInfo.InvariantCulture);
                         break;
                     case MaxInternalIdentifier:
                         MaximumValue = int.Parse(((XmlText)subnode.FirstChild).Value, System.Globalization.CultureInfo.InvariantCulture);
                         break;
                     case ImmuneIdentifier:
                         Immune = bool.Parse(((XmlText)subnode.FirstChild).Value);
                         break;
                 }
             }
             catch
             {
                 // ignore incomplete or unset values
             }

             subnode = subnode.NextSibling;
         }
     }

     /// <summary>
     /// Save: Serialize this EnvironmentTolerance to an <see cref="XmlElement"/>.
     /// </summary>
     /// <param name="xmldoc">The parent <see cref="XmlDocument"/>.</param>
     /// <returns>An <see cref="XmlElement"/> representation of the EnvironmentTolerance.</returns>
     public XmlElement ToXml(XmlDocument xmldoc, string nodeName = "EnvironmentTolerance")
     {
         XmlElement xmlelEnvironmentTolerance = xmldoc.CreateElement(nodeName);
         Global.SaveData(xmldoc, xmlelEnvironmentTolerance, MinInternalIdentifier, MinimumValue);
         Global.SaveData(xmldoc, xmlelEnvironmentTolerance, MaxInternalIdentifier, MaximumValue);
         // "correct" values for human readability only
         Global.SaveData(xmldoc, xmlelEnvironmentTolerance, MinToleranceIdentifier, Format(MinimumValue));
         Global.SaveData(xmldoc, xmlelEnvironmentTolerance, MaxToleranceIdentifier, Format(MaximumValue));
         Global.SaveData(xmldoc, xmlelEnvironmentTolerance, ImmuneIdentifier, Immune.ToString());
         return xmlelEnvironmentTolerance;
     }
     */
}
