package org.matsim.core.mobsim.qsim.qnetsimengine;

public interface TimeVariantLink {

	void recalcTimeVariantAttributes(double time);
	// yyyy my intuition says that this should be moved to the InternalInterface.  kai, dec'11

}