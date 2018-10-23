// https://searchcode.com/api/result/76509009/

package org.gwtopenmaps.openlayers.client.util;


import org.w3c.dom.Element;

/**
 * <p>
 * A JSObject is an opaque handle to a JavaScript object. Like it's parent it
 * cannot be created directly. A JSObject has to be created via JSNI or via the
 * static factory method createJSObject.
 *
 * It expands upon it's parent by offering several convenience functions, such as
 * setter and getter methods to set properties on the JSObject. A property can be
 * a number, boolean or string, but also another JSObject or a DOM Element.
 * To set a function on a JSObject, it can be created via JSNI, returned as a JSObject and set as property.
 * </p>
 * <p>
 * The purpose of the setter and getter methods is to reduce the amount of JSNI code
 * necessary for working with JavaScript objects.
 * </p>
 *
 * @author Erdem Gunay
 * @author Edwin Commandeur - Atlis EJS
 *
 */
public class JSObject
{

    public static JSObject createJSObject()
    {
        return JSObjectHelper.IMPL.createObject();
    }

    public static JSObject createJSFunction()
    {
        return JSObjectHelper.IMPL.createFunction();
    }

    public static JSObject createJSArray()
    {
        return JSObjectHelper.IMPL.createArray();
    }

    protected JSObject()
    {
        super();
    }

    // TODO: implement setProperty and getProperty methods here

    public final void setProperty(String name, int value)
    {
        JSObjectHelper.IMPL.setProperty(this, name, value);
    }

    public final int getPropertyAsInt(String name)
    {
        return JSObjectHelper.IMPL.getPropertyAsInt(this, name);
    }

    public final void setProperty(String name, String value)
    {
        JSObjectHelper.IMPL.setProperty(this, name, value);
    }

    public final String getPropertyAsString(String name)
    {
        return JSObjectHelper.IMPL.getPropertyAsString(this, name);
    }

    public final void setProperty(String name, boolean value)
    {
        JSObjectHelper.IMPL.setProperty(this, name, value);
    }

    public final boolean getPropertyAsBoolean(String name)
    {
        return JSObjectHelper.IMPL.getPropertyAsBoolean(this, name);
    }

    public final void setProperty(String name, float value)
    {
        JSObjectHelper.IMPL.setProperty(this, name, value);
    }

    public final float getPropertyAsFloat(String name)
    {
        return JSObjectHelper.IMPL.getPropertyAsFloat(this, name);
    }

    public final void setProperty(String name, double value)
    {
        JSObjectHelper.IMPL.setProperty(this, name, value);
    }

    public final double getPropertyAsDouble(String name)
    {
        return JSObjectHelper.IMPL.getPropertyAsDouble(this, name);
    }

    public final void setProperty(String name, Element value)
    {
        JSObjectHelper.IMPL.setProperty(this, name, value);
    }

    public final Element getPropertyAsDomElement(String name)
    {
        return JSObjectHelper.IMPL.getPropertyAsDomElement(this, name);
    }

    public final void setProperty(String name, JSObject value)
    {
        JSObjectHelper.IMPL.setProperty(this, name, value);
    }

    public final JSObject getProperty(String name)
    {
        return JSObjectHelper.IMPL.getProperty(this, name);
    }

    /**
     * Let's you inspect the names of properties of a javascript object.
     *
     * @return property names - A String with the names of all the properties
     *   of the object, separated by a comma. For example: prop1,prop2,prop3.
     */
    public final String getPropertyNames()
    {
        return JSObjectHelper.IMPL.getPropertyNames(this);
    }

    public final String getPropertyValues()
    {
        return JSObjectHelper.IMPL.getPropertyValues(this);
    }

    public final boolean hasProperty(String name)
    {
        return JSObjectHelper.IMPL.hasProperty(this, name);
    }

    /**
     * Unset/Clear the property with the given name. Uses the javascript operator delete
     * @param object The object in which the property exists
     * @param name The name of the property
     */
    public final void unsetProperty(String name)
    {
        JSObjectHelper.IMPL.unsetProperty(this, name);
    }

    /**
     * Checks whether the JSObject is a JavaScript Array by duck typing. If the
     * JSObject is a JavaScript Array it is returned as is, otherwise a JavaScript
     * Array is returned with the JSObject as element.
     *
     * @return JSObject - A JavaScript Array
     */
    public final JSObject ensureOpaqueArray()
    {
        return JSObjectHelper.IMPL.ensureOpaqueArray(this);
    }

    /**
     * Takes a property name of a JSObject that is a javascript array.  It ensures
     * the object is an array by calling ensureOpaqueArray so you don't have to
     *
     * @param name
     * @return JSObject[]
     */
    public final JSObject[] getPropertyAsArray(String name)
    {
        JSObject JSArray = JSObjectHelper.IMPL.ensureOpaqueArray(this.getProperty(name));
        String[] indices = JSArray.getPropertyNames().split(",");
        JSObject[] arrayOfProperties = new JSObject[indices.length];
        int i = 0;
        for (String index : indices)
        {
            arrayOfProperties[i++] = JSArray.getProperty(index);
        }

        return arrayOfProperties;
    }
    // TODO: do we need a setProperty(array[])?
    
	@SuppressWarnings("unchecked")
	public <T> T cast() {
		return (T) this;
	}
}

