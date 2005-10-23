package nomad.com.message;

import nomad.com.ComPortListener;

/**
 * @author Christian Schneider
 * @hidden
 */
public abstract class IAmMessage extends MidiMessage {
	public abstract void setVersion(int high, int low);
	public abstract int getVersionHigh();
	public abstract boolean isSenderModular();
	public void notifyListener(ComPortListener listener) {
		listener.messageReceived(this);
	}
}
