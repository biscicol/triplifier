The file dwcterms.rdf is identical in content to that which is available directly from TDWG at http://rs.tdwg.org/dwc/rdf/dwcterms.rdf.

The formatting is improved, however, to make it more easily readable for humans.  The formatting changes are easily accomplished with the following gVIM commands.

:%s/\t</\r\t</g
%s/Description>/Description>\r/g

