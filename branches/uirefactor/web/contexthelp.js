
/**
 * Provides contextual pop-up help messages for the Triplifier.  This class requires a pre-defined
 * <div> element in the HTML document to use as a message container.  The <div> must be defined as
 * follows.
 *
 * <div id="someID"><div></div><a href="#">close</a></div>
 *
 * The inner <div> will contain the message contents.  Once a ContextHelpManager is instantiated,
 * each help message is defined separately by calling setHelpItem().
 **/

/**
 * Constructs a new ContextHelpManager.
 *
 * @param msgelementid  The ID of the <div> to use as the popup message container.
 **/
function ContextHelpManager(msgelementid) {
	this.is_open = false;

	this.msgdiv = $('#' + msgelementid);

	// Make sure the message element is a child of the <body> element to ensure that
	// absolute positioning works properly.
	this.msgdiv.detach();
	this.msgdiv.appendTo('body');

	this.contentdiv = this.msgdiv.children('div');

	// Add an event handler to the close element.
	var closelink = this.msgdiv.children('a');
	var self = this;
	closelink.click(function(evnt) { self.hideMessage(); return false; });

	// Respond to "esc" key presses to close an active help message.
	// To do this, we intercept all keypress events for the document to ensure that an "esc"
	// press isn't missed due to the focus being on the wrong document element.
	$(document).keydown(function(evnt) { self.keyDown(evnt); });
}

/**
 * Defines a new help message in this ContextHelpManager.  The message can
 * include arbitrary HTML elements.
 *
 * @param linkid  The ID of the <a> element that opens the help message.
 * @param message  The help message to display.
 **/
ContextHelpManager.prototype.setHelpItem = function(linkid, message) {
	openlink = $('#' + linkid);

	var self = this;
	openlink.click(function(evnt) { self.showMessage(evnt, message); return false; } )
}

ContextHelpManager.prototype.keyDown = function(evnt) {
	if ((evnt.which == 27) && this.is_open)
		this.hideMessage();
}

ContextHelpManager.prototype.showMessage = function(evnt, message) {
	// Get the location of the click event.
	var msgX = evnt.pageX;
	var msgY = evnt.pageY;

	// Set the message contents.
	this.contentdiv.html(message);

	// Get the size of the popup message.
	var msgheight = this.msgdiv.outerHeight();
	var msgwidth = this.msgdiv.outerWidth();

	// Get the viewport size and scroll positions.
	var winheight = $(window).height();
	var winwidth = $(window).width();
	var scrollX = $('html').scrollLeft();
	var scrollY = $('html').scrollTop();

	// Make sure the message box won't run off the bottom of the screen.
	if ((msgY + msgheight) > (winheight + scrollY))
		msgY -= (msgY + msgheight) - (winheight + scrollY);

	// Try to ensure it doesn't run off the sides, either.
	if ((msgX + msgwidth) > (winwidth + scrollX))
		msgX -= msgwidth;
	if (msgX < 0)
		msgX = 0;

	this.msgdiv.css('left', msgX);
	this.msgdiv.css('top', msgY);

	this.msgdiv.fadeIn('fast');

	this.is_open = true;
}

ContextHelpManager.prototype.hideMessage = function() {
	this.msgdiv.fadeOut('fast');
	this.is_open = false;
}

