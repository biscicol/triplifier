/**
 * Implements shared, generic functionality for all triplifier project sections.  A "section" includes
 * all of the content inside one of the HTML section <div>s.  This top-level base class provides
 * basic activate/deactive functionality.
 **/
function ProjectSection(element) {
	// If element is null, the constructor is being called merely for inheritance purposes,
	// so exit without creating any "own" properties.
	if (element == null) {
		return;
	}

	this.element = element;
	this.contentelem = element.children("div.sectioncontent");

	// track whether this section is active
	this.isactive = false;
	
	this.contentelem.addClass("flexTable");
	//this.contentelem.children("table").hide();
}

/**
 * Set the activation state of this ProjectSection.
 **/
ProjectSection.prototype.setActive = function(isactive) {
	this.element.toggleClass("active", isactive);

	var inputs = this.contentelem.find("input");
	inputs.fadeToggle(isactive);

	this.isactive = isactive;
}

