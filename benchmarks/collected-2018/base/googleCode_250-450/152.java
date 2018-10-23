// https://searchcode.com/api/result/13357134/

//#ifndef BLACKBERRY
//
// BlackBerry devices before JDE 4.2.1 did not support JSR-82 and
// will fail to load because of this class.  BlackBerry phones that
// do support JSR-82 are the 8300 (Curve) and 8800 (other 8x00 can
// be upgraded).

//#ifndef ANDROID

package org.j4me.bluetoothgps;

import java.io.IOException;

import java.util.Enumeration;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import org.j4me.logging.*;

/**
 * Looks for Bluetooth devices (within 10 meters) and returns their
 * names and addresses.
 */
class BluetoothDeviceDiscovery
	implements DiscoveryListener {

    /**
     * Indicates device discovery is in progress
     */
    public static final int INQUIRY_IN_PROGRESS = -1;

    /**
     * Indicates service search is in progress
     */
    public static final int SERVICE_SEARCH_IN_PROGRESS = -1;

    /**
     * Keeps the discovery agent reference.
     */
    private DiscoveryAgent discoveryAgent;

    /**
     * A list of discovered devices.  This is a list of {@link RemoteDevice} objects.
     */
    private Vector discoveredDevices = new Vector();

    /**
     * The list of discovered services for the selected device
     */
    private Vector services = new Vector();

    /**
     * The result of the inquiry.  When the inquiry is done this result will
     * be positive number and have one of the following values.
     * <p>
     * {@link DiscoveryListener#INQUIRY_COMPLETED}
     * {@link DiscoveryListener#INQUIRY_ERROR}
     * {@link DiscoveryListener#INQUIRY_TERMINATED}
     * <p>
     * While it is searching it will have the following value
     * <p>
     * {@link #INQUIRY_IN_PROGRESS}
     */
    private int deviceDiscoveryResult = -1;

    /**
     * The result of the service search.  When the inquiry is done this result
     * will be a positive number and have one of the following values.
     * <p>
     * {@link DiscoveryListener#SERVICE_SEARCH_COMPLETED}
     * {@link DiscoveryListener#SERVICE_SEARCH_TERMINATED}
     * {@link DiscoveryListener#SERVICE_SEARCH_ERROR}
     * {@link DiscoveryListener#SERVICE_SEARCH_NO_RECORDS}
     * {@link DiscoveryListener#SERVICE_SEARCH_DEVICE_NOT_REACHABLE}
     * <p>
     * While is is searching it will have the following value
     * {@link #SERVICE_SEARCH_IN_PROGRESS}
     */
    private int serviceSearchResult = -1;

	/**
	 * Uses Bluetooth device discovery to get a list of the nearby devices
	 * that are turned on and accepting connections (within 10 meters).
	 * <p>
	 * Discovering Bluetooth devices is a lengthy operation.  It usually takes
	 * more than ten seconds.  Therefore this method call normally should
	 * be made from a separate thread to keep the application responsive.
	 * 
	 * @return An array of all the nearby Bluetooth devices that will accept
	 *  a connection, not just GPS devices.  Each array element returns another
	 *  <code>String[2]</code> where the first element is the device's human readable
	 *  name and the second is the address (devices that do not support human
	 *  readable names will be set to the address).  If the operation completed
	 *  successfully, and no devices are nearby, the returned array will have
	 *  length 0.  If the operation terminated, for example because the device
	 *  does not support the Bluetooth API, the returned array will be <code>null</code>.
	 * @throws IOException if any Bluetooth I/O errors occur.  For example if
	 *  another Bluetooth discovery operation is already in progess. 
	 * @throws SecurityException if the user did not grant access to Bluetooth
	 *  on the device.
	 */
    public String[][] discoverNearbyDeviceNamesAndAddresses ()
    	throws IOException, SecurityException
    {
		// Start discovering devices.
		doDiscoverDevices();
		
		while ( getDeviceDiscoveryResult() == BluetoothDeviceDiscovery.INQUIRY_IN_PROGRESS )
		{
			try
			{
				Thread.sleep( 20 );
			}
			catch (InterruptedException e)
			{
				// This thread is being killed, just exit.
				return null;
			}
		}
		
		// Did it fail?
		if ( getDeviceDiscoveryResult() != BluetoothDeviceDiscovery.INQUIRY_COMPLETED )
		{
			throw new IOException("Search for Bluetooth devices failed (code " + getDeviceDiscoveryResult() + ").");
		}
		
		// Create a list of the discovered devices.
		Vector discoveredDevices = getDiscoveredDevices();
		Enumeration devices = discoveredDevices.elements();
		String[][] ret = new String[discoveredDevices.size()][2];
		
		for ( int i = 0; devices.hasMoreElements(); i++ )
		{
			RemoteDevice device = (RemoteDevice)devices.nextElement();

			// Discover the Bluetooth URL.
			String address = device.getBluetoothAddress();

			// Discover the name as will be shown to the user.
			String name = null;
			
			try
			{
				name = device.getFriendlyName( false );
			}
			catch (IOException e)
			{
				Log.debug("Could not get friendly name for device " + address, e);
			}

			if ( name == null )
			{
				name = address;
			}
			
			// Record this device.
			String[] val = new String[] { name, address };
			ret[i] = val;
		}
		
		return ret;
    }
    
    /**
     * Start the device discovery
     * @throws BluetoothStateException
     */
    public void doDiscoverDevices() throws BluetoothStateException {
        Log.debug("Starting Bluetooth device discovery");

        // create/get a local device and discovery agent
        LocalDevice localDevice = LocalDevice.getLocalDevice();
        discoveryAgent = localDevice.getDiscoveryAgent();

        // place the device in inquiry mode
        discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
    }

    /**
     * Perform service discovery on a remote device.
     *
     * @param remote the remote device to inquire services for
     * @throws BluetoothStateException
     */
    public void doDiscoverService(RemoteDevice remote)
        throws BluetoothStateException {
        //
        // this large array of integer values will tell JABWT to query
        // for all known attributes
        // see https://www.bluetooth.org/foundry/assignnumb/document/service_discovery
        // section 4.5 for meaning of these IDs
        //
        // note: you don't have to retrieve all attribute everytime. in fact, you
        // can put null for attr and retrieve just the default values. that probably
        // good enough for most cases. we list all attributes for demo purpose
        //
        // note: For Nokia 6230, there is a limit (13) of number of attribute to retrieve
        // and this large array will cause error. If you are using Nokia 6230, you should
        // reduce this array to 13 elements. Just pick any 13 of them and make sure you pick
        // 0x0001 (ServiceClassID) so we can display the profiel name

        /* int[] attr = new int[]{0x0000, 0x0001, 0x0002, 0x0003, 0x0004,
             0x0005, 0x0006, 0x0007, 0x0008, 0x0009, 0x000A, 0x000B, 0x000C, 0x000D,
             0x0100, 0x0101, 0x0102, 0x0200, 0x0201,
             0x0301, 0x0302, 0x0303, 0x0304, 0x0305, 0x0306, 0x0307, 0x0308, 0x0309,
             0x030A, 0x030B, 0x030C, 0x030D, 0x030E, 0x0310, 0x0311, 0x0312, 0x0313 };
        */
        int[] attr = new int[] {
                0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0007,
                0x0008, 0x0009, 0x000A, 0x000B, 0x000C
            };

        //
        // search for L2CAP services, most services based on L2CAP
        //
        // note: Rococo simulator required a new instance of Listener for
        // every search. not sure if this is also the case in real devices
        discoveryAgent.searchServices(attr, // attributes to retrieve from remote device
            new UUID[] { new UUID(0x0100) }, // search criteria, 0x0100 = L2CAP
            remote, this); // direct discovery response to Listener object
    }

    /**
     * @see javax.bluetooth.DiscoveryListener#deviceDiscovered(javax.bluetooth.RemoteDevice, javax.bluetooth.DeviceClass)
     */
    public void deviceDiscovered(RemoteDevice remoteDevice,
        DeviceClass deviceClass) {
        Log.debug("Discovered device " + remoteDevice.getBluetoothAddress());
        discoveredDevices.addElement(remoteDevice);
    }

    /**
     * @see javax.bluetooth.DiscoveryListener#servicesDiscovered(int, javax.bluetooth.ServiceRecord[])
     */
    public void servicesDiscovered(int arg0, ServiceRecord[] records) {
        if (Log.isInfoEnabled()) {
            Log.info("Found services " + records.length);
        }

        // note: we do not use transId because we only have one search at a time
        for (int i = 0; i < records.length; i++) {
            ServiceRecord record = records[i];

            services.addElement(record);
        }
    }

    /**
     * @see javax.bluetooth.DiscoveryListener#serviceSearchCompleted(int, int)
     */
    public void serviceSearchCompleted(int transID, int respCode) {
        serviceSearchResult = respCode;
    }

    /**
     * @see javax.bluetooth.DiscoveryListener#inquiryCompleted(int)
     */
    public void inquiryCompleted(int inquiryResult) {
        deviceDiscoveryResult = inquiryResult;
    }

    /**
     * Get the list of discovered devices.  This is a list of {@link RemoteDevice}
     * objects.
     *
     * @return a list of bluetooth devices in range
     */
    public Vector getDiscoveredDevices() {
        return discoveredDevices;
    }

    /**
     * Given the friendly name of the device, find the Remote Device
     *
     * @param friendlyName - The friendly name of the bluetooth device
     * @return the remove device.  If the device is not found this will return
     *  <code>null</code>
     */
    public RemoteDevice getDeviceByFriendlyName(String friendlyName)
        throws IOException {
        Enumeration devices = discoveredDevices.elements();

        RemoteDevice device = null;

        while (devices.hasMoreElements()) {
            device = (RemoteDevice) devices.nextElement();

            if (device.getFriendlyName(false).equals(friendlyName)) {
                break;
            }
        }

        return device;
    }

    /**
     * Get the first discovered service from selected remote device.
     * Your application call this method after your app receives COMPLETED
     * callback event. This will return the first service that match your
     * UUIDs in startInquiry().
     *
     * @return ServiceRecord null if no service discovered
     */
    public ServiceRecord getFirstDiscoveredService() {
        if (services.size() > 0) {
            return (ServiceRecord) services.elementAt(0);
        } else {
            return null;
        }
    }

    /**
     * Get the result of the device discovery.
     *
     * @see javax.bluetooth.DiscoveryListener#INQUIRY_COMPLETED
     * @see javax.bluetooth.DiscoveryListener#INQUIRY_ERROR
     * @see javax.bluetooth.DiscoveryListener#INQUIRY_TERMINATED
     *
     * @return a value greater than or equal to 0 when device discovery is
     *  completed.
     */
    public int getDeviceDiscoveryResult() {
        return deviceDiscoveryResult;
    }

    public String getDeviceDiscoveryResultAsString() {
        switch (deviceDiscoveryResult) {
        case DiscoveryListener.INQUIRY_COMPLETED:
            return "Completed";

        case DiscoveryListener.INQUIRY_ERROR:
            return "Error";

        case DiscoveryListener.INQUIRY_TERMINATED:
            return "Terminated";

        default:
            return "Unknown Code";
        }
    }

    /**
     * Get the result of the service search.
     *
     * @return {@link #SERVICE_SEARCH_IN_PROGRESS} if search is still in progress.
     */
    public int getServiceSearchResult() {
        return serviceSearchResult;
    }

    public String getServiceSearchResultAsString() {
        switch (serviceSearchResult) {
        case DiscoveryListener.SERVICE_SEARCH_COMPLETED:
            return "Completed";

        case DiscoveryListener.SERVICE_SEARCH_TERMINATED:
            return "Terminated";

        case DiscoveryListener.SERVICE_SEARCH_ERROR:
            return "Error";

        case DiscoveryListener.SERVICE_SEARCH_NO_RECORDS:
            return "No Records";

        case DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE:
            return "Device Not Reachable";

        default:
            return "Unknown Code";
        }
    }

    /**
     * Find channel id of this service record.
     * <p>
     * Channel id is store under ProtocolDescriptorList (0x0004) attribute,
     * then under RFCOMM protocol (0x0003) data element.
     *
     * @param serviceRecord - A service record from the bluetooth device
     * @return the channel id.  If the id could not be determined, <code>null</code>
     *  is returned.
     */
    public static String findChannelId(ServiceRecord serviceRecord) {
        // e1 is ProtocolDescriptorList
        DataElement e1 = serviceRecord.getAttributeValue(0x0004);

        for (Enumeration enum1 = (Enumeration) e1.getValue();
                enum1.hasMoreElements();) {
            // e2 is one ProtocolDescriptor
            DataElement e2 = (DataElement) enum1.nextElement();
            Enumeration enum2 = (Enumeration) e2.getValue();

            // e3 is RFCOMM (0x0003)
            DataElement e3 = (DataElement) enum2.nextElement();

            if (e3.getValue().equals(new UUID(0x0003))) {
                // e4 is channel id
                DataElement e4 = (DataElement) enum2.nextElement();
                int id = (int) e4.getLong();

                return Integer.toString(id);
            }
        }

        return null;
    }
}

//#endif // Android
//#endif // BlackBerry

