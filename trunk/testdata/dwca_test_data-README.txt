The file dwca-all_classes.zip is a DwC archive that includes all 6 core classes with
1 or more terms for each class.  The file dwca-4_classes.zip is the same as the
previous file, except the classes Identification and Event have been removed, along
with their associated terms.  This is useful for testing that the simplifier deals
with missing Identification and Event classes properly.  The classes and terms
included in dwca-all_classes.zip are as follows.


Taxon
    <field index="31" term="http://rs.tdwg.org/dwc/terms/order"/>
    <field index="32" term="http://rs.tdwg.org/dwc/terms/taxonID"/>
    <field index="1" term="http://rs.tdwg.org/dwc/terms/infraspecificEpithet"/>
    <field index="12" term="http://rs.tdwg.org/dwc/terms/genus"/>
    <field index="13" term="http://rs.tdwg.org/dwc/terms/scientificName"/>
    <field index="14" term="http://rs.tdwg.org/dwc/terms/kingdom"/>
    <field index="6" term="http://rs.tdwg.org/dwc/terms/class"/>
    <field index="19" term="http://rs.tdwg.org/dwc/terms/vernacularName"/>
    <field index="20" term="http://rs.tdwg.org/dwc/terms/specificEpithet"/>
    <field index="24" term="http://rs.tdwg.org/dwc/terms/family"/>
    <field index="25" term="http://rs.tdwg.org/dwc/terms/phylum"/>

Occurrence
    <field index="7" term="http://rs.tdwg.org/dwc/terms/catalogNumber"/>
    <field index="8" term="http://rs.tdwg.org/dwc/terms/sex"/>
    <field index="15" term="http://rs.tdwg.org/dwc/terms/individualCount"/>
    <field index="4" term="http://rs.tdwg.org/dwc/terms/recordedBy"/>
    <field index="29" term="http://rs.tdwg.org/dwc/terms/otherCatalogNumbers"/>
    <field index="30" term="http://rs.tdwg.org/dwc/terms/preparations"/>

record-level terms that should be included with Occurrence
    <field index="2" term="http://rs.tdwg.org/dwc/terms/institutionCode"/>
    <field index="3" term="http://rs.tdwg.org/dwc/terms/collectionCode"/>
    <field index="26" term="http://rs.tdwg.org/dwc/terms/basisOfRecord"/>

dcterms:Location
    <field index="10" term="http://rs.tdwg.org/dwc/terms/country"/>
    <field index="5" term="http://rs.tdwg.org/dwc/terms/stateProvince"/>
    <field index="17" term="http://rs.tdwg.org/dwc/terms/waterBody"/>
    <field index="18" term="http://rs.tdwg.org/dwc/terms/continent"/>
    <field index="23" term="http://rs.tdwg.org/dwc/terms/county"/>

Event
    <field index="11" term="http://rs.tdwg.org/dwc/terms/year"/>
    <field index="16" term="http://rs.tdwg.org/dwc/terms/day"/>
    <field index="22" term="http://rs.tdwg.org/dwc/terms/month"/>
    <field index="27" term="http://rs.tdwg.org/dwc/terms/verbatimEventDate"/>

Identification
    <field index="9" term="http://rs.tdwg.org/dwc/terms/identifiedBy"/>


record-level terms that should be dropped
    <field index="21" term="http://rs.tdwg.org/dwc/terms/informationWithheld"/>
    <field index="28" term="http://rs.tdwg.org/dwc/terms/dynamicProperties"/>

