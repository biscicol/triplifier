The file dwcterms.rdf is identical in content to that which is available directly from TDWG at http://rs.tdwg.org/dwc/rdf/dwcterms.rdf.

The formatting is improved, however, to make it more easily readable for humans.  The formatting changes are easily accomplished with the following gVIM commands.

:%s/\t</\r\t</g
%s/Description>/Description>\r/g


The file triplifier-vocab.rdf is the master default vocabulary file used by the Triplifier.  It includes some Dublin Core terms and all Darwin Core terms.  The only changes needed from what is available from TDWG is to change the "organizedInClass" attribute of the record-level terms we map to Occurrence to point to "http://rs.tdwg.org/dwc/terms/Occurrence" rather than "all".  The "organizedInClass" attribute also needs to be added to dcterms:type.


