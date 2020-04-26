package org.starsautohost.racebuilder.nova;

import java.util.HashMap;
import java.util.Vector;

/*
 * Modified from Stars Nova project
 */
public class TechLevel
 {
     /// <summary>
     /// Enumeration of the different fields of technical research.
     /// </summary>
     public enum ResearchField 
     { 
         Biotechnology(0),
         Electronics(1),
         Energy(2),
         Propulsion(3), 
         Weapons(4),
         Construction(5)
         ;
         int value;
         private ResearchField(int value){
        	 this.value = value;
         }
         public int getValue(){
        	 return value;
         }
        
     }
     static public ResearchField FirstField = ResearchField.Biotechnology;
     static public ResearchField LastField = ResearchField.Construction;

     // These members are private to hide the 
     // implementaion of the hashtable and force access through the enums, 
     // in order to prevent errors due to using string literals (e.g. "Biotech" vs "Biotechnology")
     private HashMap<String, Integer> techValues = new HashMap<String, Integer>();
     // used for internal access to the Hashtable
     private static final String[] ResearchKeys = 
     {
      "Biotechnology", "Electronics", "Energy", 
      "Propulsion",    "Weapons",     "Construction" 
     };

     /// <summary>
     /// Default Constructor.
     /// </summary>
     public TechLevel()
     {
         for (String key : ResearchKeys)
         {
             this.techValues.put(key, 0);
         }
     }

     /// <summary>
     /// Constructor setting all levels to a specified value.
     /// </summary>
     /// <param name="level">Level to set all techs too.</param>
     public TechLevel(int level)
     {
         for (String key : ResearchKeys)
         {
             this.techValues.put(key, level);
         }
     }

     /// <summary>
     /// Constructor setting all levels to individual values.
     /// </summary>
     /// <param name="biotechnology">Level to set the biotechnology.</param>
     /// <param name="electronics">Level to set the electronics.</param>
     /// <param name="energy">Level to set the energy.</param>
     /// <param name="propulsion">Level to set the propulsion.</param>
     /// <param name="weapons">Level to set the weapons.</param>
     /// <param name="construction">Level to set the construction.</param>
     public TechLevel(int biotechnology, int electronics, int energy, int propulsion, int weapons, int construction)
     {
         this.techValues.put("Biotechnology",biotechnology);
         this.techValues.put("Electronics",electronics);
         this.techValues.put("Energy",energy);
         this.techValues.put("Propulsion",propulsion);
         this.techValues.put("Weapons",weapons);
         this.techValues.put("Construction",construction);
     }

     /// <summary>
     /// Copy Constructor.
     /// </summary>
     /// <param name="copy">Object to copy.</param>
     public TechLevel(TechLevel copy)
     {
         this.techValues = new HashMap<String, Integer>(copy.techValues);
     }

     /// <summary>
     /// Provide a new TechLevel instance which is a copy of the current instance.
     /// </summary>
     /// <returns></returns>
     public TechLevel Clone()
     {
         return new TechLevel(this);
     }

     /// <summary>
     /// Index operator to allow array type indexing to a TechLevel.
     /// </summary>
     /// <param name="index">A TechLevel.ResearchField.</param>
     /// <returns>The current tech level.</returns>
     public int get(ResearchField index) throws Exception
     {
         if (this.techValues == null)
         {
             throw new Exception("TechLevel.cs : index operator - attempt to index with no TechValues defined.");
         }
         int techLevel = 0; // default tech level
         switch (index)
         {
             case Biotechnology:
                 techLevel = techValues.get("Biotechnology");
                 break;
             case Construction:
                 techLevel = techValues.get("Construction");
                 break;
             case Electronics:
                 techLevel = techValues.get("Electronics");
                 break;
             case Energy:
                 techLevel = techValues.get("Energy");
                 break;
             case Propulsion:
                 techLevel = techValues.get("Propulsion");
                 break;
             case Weapons:
                 techLevel = techValues.get("Weapons");
                 break;
             default:
                 throw new Exception("TechLevel.cs: indexing operator - Unknown field of research " + index.toString());
         }
         return techLevel;
     }
     public void set(ResearchField index, int value) throws Exception{
         switch (index)
         {
             case Biotechnology:
                 this.techValues.put("Biotechnology",value);
                 break;
             case Construction:
                 this.techValues.put("Construction",value);
                 break;
             case Electronics:
                 this.techValues.put("Electronics",value);
                 break;
             case Energy:
                 this.techValues.put("Energy",value);
                 break;
             case Propulsion:
                 this.techValues.put("Propulsion",value);
                 break;
             case Weapons:
                 this.techValues.put("Weapons",value);
                 break;
             default:
                 throw new Exception("TechLevel.cs: indexing operator - Unknown field of research " + index.toString());
         }
     }

     /// <summary>
     /// Allow <c>foreach</c> to work with TechLevel.
     /// </summary>
     /*
     public Vector<Integer> getValues()
     {
    	 Vector<Integer> v = new Vector<Integer>();
         for (int level this.techValues.keySet())
         {
             yield return level;
         }
     }
     */

     // =============================================================================
     // Note: For two tech levels A and B if any field in A is less than any coresponding
     //       field in B, then A < B is true. It is possible for (A < B) && (B < A) to
     //       be true for TechLevels. This is so that all fields must be met in order
     //       to obtain a particular component. 
     //
     //       For example a race may have 10 propulsion tech and 5 in weapons.
     //       A particular weapon may need 9 propulsion and 12 in weapons.
     //       The race has less tech than required for the weapon (weapons tech too low)
     //       But the weapon also requires less tech than the race has (in construction).
     //
     //       A > B is only true if for all tech fields in A the coresponding field in B
     //       is less than or equal to A and at least one field in A is greater than the
     //       corresponding field in B.
     //
     //       TODO (priority 5) - Given the complexity here some unit tests would be nice.
     // =============================================================================

     /// <summary>
     /// Return true if lhs >= rhs (for all fields).
     /// </summary>
     /*
     public static bool operator >=(TechLevel lhs, TechLevel rhs)
     {
         Dictionary<string, int> lhsT = lhs.techValues;
         Dictionary<string, int> rhsT = rhs.techValues;

         foreach (string key in TechLevel.ResearchKeys)
         {
             if (lhsT[key] < rhsT[key])
             {
                 return false;
             }
         }

         return true;
     }

     /// <summary>
     /// Return true if lhs >= rhs for all fields and lhs > rhs for at least one field.
     /// </summary>
     /// <param name="lhs"></param>
     /// <param name="rhs"></param>
     /// <returns></returns>
     public static bool operator >(TechLevel lhs, TechLevel rhs)
     {
         return !(lhs <= rhs);
     }

     /// <summary>
     /// Return true if lhs &lt; rhs in any field.
     /// </summary>
     public static bool operator <(TechLevel lhs, TechLevel rhs)
     {
         Dictionary<string, int> lhsT = lhs.techValues;
         Dictionary<string, int> rhsT = rhs.techValues;

         foreach (string key in TechLevel.ResearchKeys)
         {
             if (lhsT[key] < rhsT[key])
             {
                 return true;
             }
         }

         return false;
     }

     /// <summary>
     /// Return true if lhs &lt; rhs in any field or lhs == rhs.
     /// </summary>
     public static bool operator <=(TechLevel lhs, TechLevel rhs)
     {
         Dictionary<string, int> lhsT = lhs.techValues;
         Dictionary<string, int> rhsT = rhs.techValues;

         foreach (string key in TechLevel.ResearchKeys)
         {
             if (lhsT[key] < rhsT[key])
             {
                 return true;
             }
         }

         if (lhsT == rhsT)
         {
             return true;
         }

         return false;
     }
     */
     
     /// <summary>
     /// Setting all levels to zero.
     /// </summary>
     public void Zero()
     {
         this.techValues.put("Biotechnology",0);
         this.techValues.put("Electronics",0);
         this.techValues.put("Energy",0);
         this.techValues.put("Propulsion",0);
         this.techValues.put("Weapons",0);
         this.techValues.put("Construction",0);
     }

     /// <summary>
     /// Load from XML: initializing constructor from an XML node.
     /// </summary>
     /// <param name="node">An <see cref="XmlNode"/> within
     /// a Nova component definition file (xml document).
     /// </param>
     /*
     public TechLevel(XmlNode node)
     {
         XmlNode subnode = node.FirstChild;
         while (subnode != null)
         {
             try
             {
                 foreach (string key in ResearchKeys)
                 {
                     if (subnode.Name.ToLower() == key.ToLower())
                     {
                         this.techValues[key] = int.Parse(((XmlText)subnode.FirstChild).Value, System.Globalization.CultureInfo.InvariantCulture);
                     }
                 }
             }
             catch (Exception e)
             {
                 Report.Error(e.Message);
             }
             subnode = subnode.NextSibling;
         }
     }

     /// <summary>
     /// Save: Serialize this property to an <see cref="XmlElement"/>.
     /// </summary>
     /// <param name="xmldoc">The parent <see cref="XmlDocument"/>.</param>
     /// <returns>An <see cref="XmlElement"/> representation of the Tech Level.</returns>
     public XmlElement ToXml(XmlDocument xmldoc)
     {
        return this.ToXml(xmldoc, "Tech");
     }
     
     /// <summary>
     /// Save: Serialize this property to an <see cref="XmlElement"/> with a specified
     /// node name.
     /// </summary>
     /// <param name="xmldoc">The parent <see cref="XmlDocument"/>.</param>
     /// <param name="nodeName">The node name of this XML element.</param>
     /// <returns>An <see cref="XmlElement"/> representation of the Tech Level.</returns>
     public XmlElement ToXml(XmlDocument xmldoc, string nodeName)
     {
         XmlElement xmlelResource = xmldoc.CreateElement(nodeName);

         foreach (string key in TechLevel.ResearchKeys)
         {
             XmlElement xmlelTech = xmldoc.CreateElement(key);
             XmlText xmltxtTech = xmldoc.CreateTextNode(techValues[key].ToString(System.Globalization.CultureInfo.InvariantCulture));
             xmlelTech.AppendChild(xmltxtTech);
             xmlelResource.AppendChild(xmlelTech);
         }

         return xmlelResource;
     }
     */

     /// <summary>
     /// Provides a string representation of this tech level. Useful for debugging.
     /// </summary>
     /// <returns></returns>
     /*
     @Override
     public String toString()
     {
         return ", " + techValues.Where(tech => tech.Value > 0).Select(tech => tech.Key + ": " + tech.Value).ToArray());
     }
     */
}

